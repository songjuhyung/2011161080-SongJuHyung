import java.net.*;
import java.util.*;

public class UnreliNET {
	static int max_packet_size = 1024; //�ִ���Ŷ������ 1024ũ��� ����
	int total_forwarded = 0; 
	int returnPort;  

	// define thread which is used to handle one-direction of communication 
	public class Forwarder extends Thread {  
	/*
		1. ��� �� �� ���� (UnreliNET <-> Sender or UnreliNET <-> Receiver)�� ����ϴ� �������Դϴ�.
		Sender, Receiver �̷��� �ΰ��� �����尡 ���ư��ϴ�.
	*/
		private DatagramSocket source;   //�����ͱ׷� ���� (�۽���) 
		private DatagramSocket dst_socket; //�����ͱ׷� ���� (������)
		private InetAddress dst_addr; //ip �������ּ�
		private int dst_port;  //������ ��Ʈ
		private int min_propagation_delay = 0; //�ּ� ���ĵ����� 
		private int max_propagation_delay; // �ִ� ���� ������
		/*
			2. propagation delay�� ó���� ���� PC �Ǵ� OS���� internet latency(���ͳ� ����) ������ ó���� ���������� ������ �־� �߰��߽��ϴ�.
			   �����ص� �Ǵ� �κ��Դϴ�.
		*/
		private float corrupt_rate; // �Ѽ��� 
		private int corruptionCounter = 0;  // �Ѽյ� ��Ŷ ���� ����
		private float drop_rate; // �ս��� 
		private int dropCounter = 0; //�սǵ� ��Ŷ�� ����� ������
		private Random random; //������
		private Random rnd_byte; //������
		private boolean ack_stream; //ack�� ������ �������� ��������

		public Forwarder(DatagramSocket source, DatagramSocket dst_socket, String dst_host, int dst_port,
				float corrupt_rate, float drop_rate, int min_propagation_delay, int max_propagation_delay, long seed,
				boolean ack_stream) throws UnknownHostException {
			this.source = source;  //�۽��� ���� 
			this.dst_socket = dst_socket; // ������ ����
			this.dst_addr = InetAddress.getByName(dst_host); //������ ȣ��Ʈ ip�� �̸��� ������
			this.dst_port = dst_port; // ������ ��Ʈ �״�ξ�
			this.corrupt_rate = corrupt_rate; // �Ѽ��� �״�� ��
			this.drop_rate = drop_rate; // �ս��� �״�� �� 
			this.min_propagation_delay = min_propagation_delay; 
			this.max_propagation_delay = max_propagation_delay;
			this.random = new Random(seed); 
			/*
				3. Random �Լ��� seed ������� random ���� �����ϱ� ������ �̷��� ó���Ǿ����ϴ�.
			*/
			this.rnd_byte = new Random(seed);//
			this.ack_stream = ack_stream; 
		}

		public void run() { //���� ���ۺη� ���� 
			try {
				byte[] data = new byte[max_packet_size]; //�ִ� ��Ŷ ������ 1024�� ũ��� ������ �̸��� ���� �迭���� 
				DatagramPacket packet = new DatagramPacket(data, data.length); //�����ͱ׷���Ŷ���� ������, ������ ���̰� �� 

				while (true) { //�Ƹ� ����ó�� �����ϱ����� ������ true �� ���� 
					// read data from the incoming socket ������ �������κ��� ������ ���� 
					source.receive(packet); // ��Ŷ ����
					total_forwarded += packet.getLength();
					/*
						4. �Ʒ��� ���������� 1024 ũ��� ��Ŷ�� �ޱ� ������ �� ���� ��Ŷ�� �����صα� ���� ó���߽��ϴ�.
					*/
					
					// check the length of the packet ��Ŷ ���̸� �˻���
					if (packet.getLength() > max_packet_size) { //��Ŷ���� 1024���� ū�� �ϴ� ���� Ȯ�� 
 						System.err.println("Error: packet length is more than " + max_packet_size + " bytes"); //��Ŷ���̰� �ִ� ũ�⺸�� ũ�ٰ� ��� 
					 	System.exit(-1); //���� 
					}

					// decide if to drop the packet or not
					if (random.nextFloat() <= drop_rate) {
						/*
							5. 0~1 ������ ���� �������� �����ϰ�, �� ���� ����ڰ� ������ �ս������� ���� ��� Ȯ���� �¾Ҵٰ� �� �� �ֽ��ϴ�.
							ex) drop_rate �� 0 �ϰ�� random.nextFloat()�� 0~1���� �Ǽ��̱� ������ �սǵ��� �ʽ��ϴ�.
							ex) drop_rate �� 0.9 �ϰ�� random.nextFloat()���� 0~0.9 ������ ���� ���͵� �ش�Ǳ� ������ 90% Ȯ���� �սǽ�Ų�ٰ� ����˴ϴ�.
						*/
						dropCounter++; //�սǵ� ������ ���� ī���� 1�� ������Ŵ 
						System.out.println(dropCounter + " packet(s) dropped"); // ���ݱ��� ��� ��Ŷ�� �սǵǾ���
						continue; //����̾ 
					}

					// decide if to corrupt the packet or not
					if (random.nextFloat() <= corrupt_rate) { 
						/*
							�� �κе� ���� �����ϴ�.
						*/
 						for (int i = 0; i < packet.getLength(); ++i) // ��Ŷ�� ������ ����ŭ �ݺ��� �ϰ�, 
							// we have an extra random number generator for the
							// corruption since the packet
							// length might be different between submissions
							if (i == 0 || rnd_byte.nextFloat() <= 0.3) 
																		// if to
																		// corrupt
																		// a
																		// byte
								data[i] = (byte) ((data[i] + 1) % 10); 
							/*
								6. �� �κ��� �ջ�� ��Ŷ�� ����� �����Դϴ�. data[i]�� (byte) ((data[i] + 1) % 10)�� ���� �ٲ����� ��Ŷ�� �������뿡�� �ջ��� �Ǵ°��Դϴ�.
								0.3, data+1 % 10�� ���� ���Ƿ� ���� �ջ� ��Ģ�Դϴ�. ���ʿ��Ͻôٰ� �����ǽø� ���Ƿ� �ٸ� ���·� ó���� �� �ֽ��ϴ�.
							*/
						corruptionCounter++; //���������� �Ѽ��� �ϳ� ������Ŵ 
						System.out.println(corruptionCounter + " packet(s) corrupted"); //���ݱ��� ��� ��Ŷ�� �ѼյǾ��ٰ� ���
					}

					// In the second thread (the one that forwards the ACKS)
					// we need to know the source port from the packets
					if (!ack_stream) //ack��Ʈ���� ���� �ƴ϶�� 
						returnPort = packet.getPort(); //���� ��Ʈ�� ���� ������Ʈ�� ����ְ� 
					else
						dst_port = returnPort; //�׷��� �ʴٸ� ���ϵ� ��Ʈ ���� ������ ��Ʈ�� �Ѵ�
					// add some propagation delay
					int delay = min_propagation_delay 
							+ random.nextInt(max_propagation_delay - min_propagation_delay + 1);
					/*
						nop ����
					*/
					Thread.sleep(delay); //������ ������ ��ġ��ŭ ����ߴ� 
					// send the data
					dst_socket.send(new DatagramPacket(data, packet.getLength(), dst_addr, dst_port));// ������ �ּ�, ��Ʈ, ����� ����, �����ͷ� �����ͱ׷����� ���� ������
				}

			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1); //���ܰ� �ִٸ� �����Ų��.
			}
		}
	}

	public UnreliNET(float data_corrupt_rate, float data_loss_rate, float ack_corrupt_rate, float ack_loss_rate,
			int min_propagation_delay, int max_propagation_delay, int unreliNetPort, String rcvHost, int rcvPort) { 

		System.out.println("unreliNetPort = " + unreliNetPort + "\nrcvHost = " + rcvHost + "\nrcvPort = " + rcvPort 
				+ "\ndata corruption rate = " + data_corrupt_rate + "\nack/nak corruption rate = " + ack_corrupt_rate
				+ "\ndata loss rate = " + data_loss_rate + "\nack/nak loss rate = " + ack_loss_rate
				+ "\nmin propagation delay = " + min_propagation_delay + "\nmax propagation delay = "
				+ max_propagation_delay);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("Forwarded " + total_forwarded + " bytes"); //
			}
		});

		try {
			DatagramSocket sender = new DatagramSocket(unreliNetPort); 
			DatagramSocket receiver = new DatagramSocket(); 
			/*
				8. sender������ ������ ��쿡�� remote port�� �־�� UDP ����� �����մϴ�.
				������ receiver�� �� UnreliNET���� ������ ���̱� ������ port�� �ʿ�����ϴ�.
				receiver�� UnreliNET�� �����ִ� port�� �����ϴ°� �½��ϴ�.
				
				�� �ٽ� �����帮�� DatagramSocket�� ���ڰ��� remotePort�� �����Ͻø� �ǰڽ��ϴ�.
			
			*/

			// create threads to process sender's incoming data
			Forwarder th1 = new Forwarder(sender, receiver, rcvHost, rcvPort, data_corrupt_rate, data_loss_rate,
					min_propagation_delay, max_propagation_delay, 0, false); 
			th1.start();

			// create threads to process receiver's incoming data
			Forwarder th2 = new Forwarder(receiver, sender, "localhost", returnPort, ack_corrupt_rate, ack_loss_rate,
					min_propagation_delay, max_propagation_delay, 0, true);
					
			/*
				9. ���⼭�� true�� false�� ack_stream���� ���� �����ִ�
				if (!ack_stream)
						returnPort = packet.getPort();
				�� �κ� ������ �ʿ��� ���Դϴ�.
				returnPort�� receiver -> sender ack ��û�� �ʿ��� ���Դϴ�.
				�ٷ� ������ �����ص�ȴ� �� ó�� receiver�� port�� ���� �� port�� �־����� �ʰ� Sender���� �Ѿ�� ��Ŷ�� Receiver�� �Ѱ��ٶ� �� ��� port�� �䱸�Ǳ� ������
				ack ��û�� ����� receiver�� ��� port ���� �޾ƿ;��ϱ� ������ �̷��� �����Ǿ����ϴ�.
				(��� ���ڰ����� ���� ��� port�� �״�� �־��൵ ��������� �Ϲ����� UDP - ACK ���� ���� ����� �̷��մϴ�.)
				
			*/
					
			th2.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private final static int DEFAULT_NET_PORT = 9000;
	private final static int DEFAULT_RCV_PORT = 9001;
	/*
		10. net port�� rcv port�� �ᱹ receiver�� sender�� �ش��ϴ� port�� �ǹ��մϴ�. 
	*/
	public static void main(String[] args) {
		// parse parameters
		if (args.length == 6) { 
		/*
			11. ���ڰ��� 6���ΰ��� �⺻���� �����Դϴ�. �䱸���� �� "��Ʈ��ȣ�� �Է����� ���� ��� ������ �⺻���� ����϶�" ��� ��õǾ��ֱ� ������
			��Ʈ��ȣ 2���� ���� 4���� ���ڰ��� ������ ��쿡�� DEFAULT_NET_PORT, DEFAULT_RCV_PORT �� �־��ִ� ������ �����߽��ϴ�.
		*/
			new UnreliNET(Float.parseFloat(args[0]), Float.parseFloat(args[1]), Float.parseFloat(args[2]),
					Float.parseFloat(args[3]), 0, 0, Integer.parseInt(args[4]), "localhost", Integer.parseInt(args[5]));
		} else if (args.length == 4) {
			new UnreliNET(Float.parseFloat(args[0]), Float.parseFloat(args[1]), Float.parseFloat(args[2]),
					Float.parseFloat(args[3]), 0, 0, DEFAULT_NET_PORT, "localhost", DEFAULT_RCV_PORT);
		} else {
			System.err.println("Usage: java UnreliNET <P_DATA_CORRUPT> <P_DATA_LOSS> <P_ACK_CORRUPT> <P_ACK_LOSS> " //�Ѽ��� �ս��� �Ѽ��� �ս��� �̷������� ���
					+ "<unreliNetPort> <rcvPort>");
			System.exit(-1);
		}
	}
}

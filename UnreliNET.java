import java.net.*;
import java.util.*;

public class UnreliNET {
	static int max_packet_size = 1024; //최대패킷사이즈 1024크기로 설정
	int total_forwarded = 0; 
	int returnPort;  

	// define thread which is used to handle one-direction of communication 
	public class Forwarder extends Thread {  
	/*
		1. 통신 중 한 방향 (UnreliNET <-> Sender or UnreliNET <-> Receiver)를 담당하는 쓰레드입니다.
		Sender, Receiver 이렇게 두개의 쓰레드가 돌아갑니다.
	*/
		private DatagramSocket source;   //데이터그램 소켓 (송신지) 
		private DatagramSocket dst_socket; //데이터그램 소켓 (수신지)
		private InetAddress dst_addr; //ip 목적지주소
		private int dst_port;  //목적지 포트
		private int min_propagation_delay = 0; //최소 전파딜레이 
		private int max_propagation_delay; // 최대 전파 딜레이
		/*
			2. propagation delay는 처리가 느린 PC 또는 OS에서 internet latency(인터넷 지연) 문제로 처리가 꼬여버리는 문제가 있어 추가했습니다.
			   삭제해도 되는 부분입니다.
		*/
		private float corrupt_rate; // 훼손율 
		private int corruptionCounter = 0;  // 훼손된 패킷 갯수 세기
		private float drop_rate; // 손실율 
		private int dropCounter = 0; //손실된 패킷이 몇개인지 세려고
		private Random random; //랜덤값
		private Random rnd_byte; //랜덤값
		private boolean ack_stream; //ack가 참인지 거짓인지 따지려고

		public Forwarder(DatagramSocket source, DatagramSocket dst_socket, String dst_host, int dst_port,
				float corrupt_rate, float drop_rate, int min_propagation_delay, int max_propagation_delay, long seed,
				boolean ack_stream) throws UnknownHostException {
			this.source = source;  //송신지 소켓 
			this.dst_socket = dst_socket; // 목적지 소켓
			this.dst_addr = InetAddress.getByName(dst_host); //목적지 호스트 ip의 이름을 가져옴
			this.dst_port = dst_port; // 목적지 포트 그대로씀
			this.corrupt_rate = corrupt_rate; // 훼손율 그대로 씀
			this.drop_rate = drop_rate; // 손실율 그대로 씀 
			this.min_propagation_delay = min_propagation_delay; 
			this.max_propagation_delay = max_propagation_delay;
			this.random = new Random(seed); 
			/*
				3. Random 함수는 seed 기반으로 random 값을 추출하기 때문에 이렇게 처리되었습니다.
			*/
			this.rnd_byte = new Random(seed);//
			this.ack_stream = ack_stream; 
		}

		public void run() { //실제 동작부로 이해 
			try {
				byte[] data = new byte[max_packet_size]; //최대 패킷 사이즈 1024의 크기로 데이터 이름을 갖는 배열선언 
				DatagramPacket packet = new DatagramPacket(data, data.length); //데이터그램패킷에는 데이터, 데이터 길이가 들어감 

				while (true) { //아마 서버처럼 동작하기위해 무한정 true 로 돌림 
					// read data from the incoming socket 들어오는 소켓으로부터 데이터 읽음 
					source.receive(packet); // 패킷 받음
					total_forwarded += packet.getLength();
					/*
						4. 아래에 나와있지만 1024 크기로 패킷을 받기 때문에 총 받은 패킷을 연산해두기 위해 처리했습니다.
					*/
					
					// check the length of the packet 패킷 길이를 검사함
					if (packet.getLength() > max_packet_size) { //패킷길이 1024보다 큰지 일단 여부 확인 
 						System.err.println("Error: packet length is more than " + max_packet_size + " bytes"); //패킷길이가 최대 크기보다 크다고 출력 
					 	System.exit(-1); //종료 
					}

					// decide if to drop the packet or not
					if (random.nextFloat() <= drop_rate) {
						/*
							5. 0~1 사이의 새로 랜덤값을 생성하고, 그 값이 사용자가 설정한 손실율보다 작을 경우 확률에 맞았다고 볼 수 있습니다.
							ex) drop_rate 가 0 일경우 random.nextFloat()는 0~1중의 실수이기 때문에 손실되지 않습니다.
							ex) drop_rate 가 0.9 일경우 random.nextFloat()에서 0~0.9 이하의 수만 나와도 해당되기 때문에 90% 확률로 손실시킨다고 보면됩니다.
						*/
						dropCounter++; //손실된 갯수를 세는 카운터 1개 증가시킴 
						System.out.println(dropCounter + " packet(s) dropped"); // 지금까지 몇개의 패킷이 손실되었음
						continue; //계속이어감 
					}

					// decide if to corrupt the packet or not
					if (random.nextFloat() <= corrupt_rate) { 
						/*
							이 부분도 위와 같습니다.
						*/
 						for (int i = 0; i < packet.getLength(); ++i) // 패킷의 길이의 수만큼 반복을 하고, 
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
								6. 이 부분은 손상된 패킷을 만드는 과정입니다. data[i]에 (byte) ((data[i] + 1) % 10)로 값을 바꿈으로 패킷이 원래내용에서 손상이 되는것입니다.
								0.3, data+1 % 10은 제가 임의로 만든 손상 규칙입니다. 불필요하시다고 생각되시면 임의로 다른 형태로 처리할 수 있습니다.
							*/
						corruptionCounter++; //마찬가지로 훼손율 하나 증가시킴 
						System.out.println(corruptionCounter + " packet(s) corrupted"); //지금까지 몇개의 패킷이 훼손되었다고 출력
					}

					// In the second thread (the one that forwards the ACKS)
					// we need to know the source port from the packets
					if (!ack_stream) //ack스트림이 참이 아니라면 
						returnPort = packet.getPort(); //얻어온 포트를 값을 리턴포트에 집어넣고 
					else
						dst_port = returnPort; //그렇지 않다면 리턴된 포트 값을 목적지 포트로 한다
					// add some propagation delay
					int delay = min_propagation_delay 
							+ random.nextInt(max_propagation_delay - min_propagation_delay + 1);
					/*
						nop 역할
					*/
					Thread.sleep(delay); //쓰레드 딜레이 수치만큼 잠시중단 
					// send the data
					dst_socket.send(new DatagramPacket(data, packet.getLength(), dst_addr, dst_port));// 목적지 주소, 포트, 얻어진 길이, 데이터로 데이터그램으로 만들어서 전송함
				}

			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1); //예외가 있다면 종료시킨다.
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
				8. sender쪽으로 보내는 경우에는 remote port가 있어야 UDP 통신이 가능합니다.
				하지만 receiver는 이 UnreliNET으로 보내는 것이기 때문에 port가 필요없습니다.
				receiver가 UnreliNET에 열려있는 port로 연결하는게 맞습니다.
				
				즉 다시 말씀드리면 DatagramSocket의 인자값은 remotePort라 생각하시면 되겠습니다.
			
			*/

			// create threads to process sender's incoming data
			Forwarder th1 = new Forwarder(sender, receiver, rcvHost, rcvPort, data_corrupt_rate, data_loss_rate,
					min_propagation_delay, max_propagation_delay, 0, false); 
			th1.start();

			// create threads to process receiver's incoming data
			Forwarder th2 = new Forwarder(receiver, sender, "localhost", returnPort, ack_corrupt_rate, ack_loss_rate,
					min_propagation_delay, max_propagation_delay, 0, true);
					
			/*
				9. 여기서의 true와 false는 ack_stream으로 저기 위에있는
				if (!ack_stream)
						returnPort = packet.getPort();
				이 부분 때문에 필요한 것입니다.
				returnPort는 receiver -> sender ack 요청시 필요한 것입니다.
				바로 위에서 설명해드렸던 것 처럼 receiver의 port는 생성 시 port를 주어지지 않고 Sender에서 넘어온 패킷을 Receiver로 넘겨줄때 그 대상 port가 요구되기 때문에
				ack 요청시 대상이 receiver일 경우 port 값을 받아와야하기 때문에 이렇게 구현되었습니다.
				(사실 인자값으로 받은 대상 port를 그대로 넣어줘도 상관없지만 일반적인 UDP - ACK 구조 구현 모습이 이러합니다.)
				
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
		10. net port와 rcv port는 결국 receiver와 sender에 해당하는 port를 의미합니다. 
	*/
	public static void main(String[] args) {
		// parse parameters
		if (args.length == 6) { 
		/*
			11. 인자값이 6개인것이 기본적인 형태입니다. 요구사항 중 "포트번호를 입력하지 않을 경우 지정된 기본값을 사용하라" 라고 명시되어있기 때문에
			포트번호 2개가 빠진 4개의 인자값이 들어왔을 경우에는 DEFAULT_NET_PORT, DEFAULT_RCV_PORT 를 넣어주는 것으로 구현했습니다.
		*/
			new UnreliNET(Float.parseFloat(args[0]), Float.parseFloat(args[1]), Float.parseFloat(args[2]),
					Float.parseFloat(args[3]), 0, 0, Integer.parseInt(args[4]), "localhost", Integer.parseInt(args[5]));
		} else if (args.length == 4) {
			new UnreliNET(Float.parseFloat(args[0]), Float.parseFloat(args[1]), Float.parseFloat(args[2]),
					Float.parseFloat(args[3]), 0, 0, DEFAULT_NET_PORT, "localhost", DEFAULT_RCV_PORT);
		} else {
			System.err.println("Usage: java UnreliNET <P_DATA_CORRUPT> <P_DATA_LOSS> <P_ACK_CORRUPT> <P_ACK_LOSS> " //훼손율 손실율 훼손율 손실율 이런순서로 사용
					+ "<unreliNetPort> <rcvPort>");
			System.exit(-1);
		}
	}
}

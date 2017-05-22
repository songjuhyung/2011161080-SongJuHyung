import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

class Receiver {
	private final static int DEFAULT_RCV_PORT = 9001; //������ ��Ʈ�� �⺻������ 9001 ���
	public static void main(String[] args) throws Exception {
		// check if the number of command line argument is 4
		if (args.length != 1) 
			new Receiver(DEFAULT_RCV_PORT); 
		else
			new Receiver(Integer.parseInt(args[0])); //�ƴ϶�� ó�� 0��°�� ���� ������� 
	}

	public Receiver(int port) throws Exception {
		// Do not change this 
		DatagramSocket socket = new DatagramSocket(port); 
		while (true) { // ������ �޼����� �׻� �޾ƿ��⸸ ��.
			String message = receiveMessage(socket);
			System.out.println(message);
		}
	}

	int seq = 0; //ó�� seq ���� 0 

	public String receiveMessage(DatagramSocket socket) throws Exception { 
		while (true) {
			int seq = -1; //�ʱ⼳���ε� -1������ ���� �켱 
			int receiveChecksum = -1;
			String message = null; //�޼����� ������ 
			int ack = -1;
			/* Receiving */
			byte[] inBuffer = new byte[1024]; // 1024�� ����ũ�⸸�� 
			DatagramPacket rcvedPkt = new DatagramPacket(inBuffer, inBuffer.length); //���ۿ� ���۱��̸�ŭ �����ͱ׷� ��Ŷ�� ���� 
			socket.receive(rcvedPkt); // ������ ��Ŷ�� �޾Ƶ���
			String rcvedData = new String(rcvedPkt.getData(), 0, rcvedPkt.getLength()); 

			// Check corrupted
			if (rcvedData.indexOf(";") == -1 || isCorrupted(rcvedData)) { 
				InetAddress receiverAddress = rcvedPkt.getAddress();  // ���� ��Ŷ�ּҸ� �����
				int receiverPort = rcvedPkt.getPort(); //���� ��Ŷ�� ��Ʈ�� ���� 

				byte[] outBuffer = new String("" + (this.seq - 1)).getBytes(); 
				DatagramPacket sendPkt = new DatagramPacket(outBuffer, outBuffer.length, receiverAddress, receiverPort);//������ �ּ�, ��Ʈ, �ƿ����ۿ� �ƿ����� ���̷� �۽���Ŷ ���� 

				socket.send(sendPkt); //��Ŷ ����
				//System.out.println("Packet corrupted!"); 
				continue; //�浹�Ǿ��ٰ� �˸� �׸��� �����Ѱܹ��� 
			} else
				rcvedData = rcvedData.substring(rcvedData.indexOf(";") + 1); 
			if (rcvedData.indexOf(";") == -1 || illegalSeq(rcvedData)) {
				InetAddress receiverAddress = rcvedPkt.getAddress(); //��Ŷ���� �ּ� ���� 
				int receiverPort = rcvedPkt.getPort();//��Ʈ ���������ξ��� 

				byte[] outBuffer = new String("" + (this.seq - 1)).getBytes(); //���� ����������-1 ��ŭ���� ����Ʈ ���� 
				DatagramPacket sendPkt = new DatagramPacket(outBuffer, outBuffer.length, receiverAddress, receiverPort); // ���������� �۽��� ��Ŷ���� 

				socket.send(sendPkt);
				//System.out.println("Illegal sequence!");
				continue;
			} else
				rcvedData = rcvedData.substring(rcvedData.indexOf(";") + 1); //�׷��������� �Ʊ�� ���������� +1 ���� 
			message = rcvedData; //�޼����� ������ ������ 

			/* Sending (ACK) */
			InetAddress receiverAddress = rcvedPkt.getAddress(); //ip�ּ� ���� ���� ��Ŷ���� 
			int receiverPort = rcvedPkt.getPort(); //��Ʈ ����

			byte[] outBuffer = new String("" + this.seq).getBytes(); //�ƿ����� ���� ���� ������ȣ ���� 
			DatagramPacket sendPkt = new DatagramPacket(outBuffer, outBuffer.length, receiverAddress, receiverPort); // ack�� ��ƺ��� ��Ŷ����

			socket.send(sendPkt); //ack���� 

			this.seq++;// ���� 1�� ������Ŵ
			return rcvedData; //������ ��ȯ 

		}
	}

	private boolean illegalSeq(String rcvedData) { 
		try {
			int seq = Integer.parseInt(rcvedData.substring(0, rcvedData.indexOf(";"))); // illegal�����Ϸ���, 0�������� rcved�����Ϳ��� ;���� �߶󳻼� 
			if (this.seq == seq) //���� ������ȣ�� ������ false  =legal�ϴٴ� �ǹ�
				return false;
				/*
					Sequence�� �սǵ� ��Ŷ�� Ȯ���ϱ� ���ؼ� �߰��Ǿ����ϴ�. sequence������� ������ �ʰ� �߰��� �ϳ��� ������ ������ �սǷ� �����ϰ� �������� ��û�ϰ� �˴ϴ�.
					this.seq == seq �̸� �������� �����̱� ������ legal �� ��Ȳ���� ó���˴ϴ�.
				*/
			else
				return true; //�׷��������� illegal
		} catch (Exception e) {
			return true;
		}
	}

	// check received data is corrupted �����Ͱ� �Ѽյž����� �˻��Ϸ��� 
	private boolean isCorrupted(String rcvedData) {
		try {
			long checksum = Long.parseLong(rcvedData.substring(0, rcvedData.indexOf(";"))); //�̹��� �߶󳽰��� Long ���� ��ȯ���� ý���̶�� long �� ������ ������� 
			rcvedData = rcvedData.substring(rcvedData.indexOf(";") + 1); // ;+1ũ�⸸ŭ �߶󳻼� �����͸� ������� 
			/*
				rcvedData.substring(0, rcvedData.indexOf(";"))�� rcvedData�� �� ó������ ";"�� �߰ߵǱ� ������, �� checksum �κ��� �ǹ��մϴ�.
				rcvedData.substring(rcvedData.indexOf(";") + 1)�� rcvedData�� ";"�� �߰ߵ� �ٷ� �����ڸ����� ������ �ڱ���, �� message �κ��� �ǹ��մϴ�.
			*/
			//System.out.println("Checksum : " + checksum);
			//System.out.println("Datachecksum : " + getchecksum(rcvedData.getBytes()));
			if (checksum == getchecksum(rcvedData.getBytes()))
				return false; //���� ���� ���������Ͱ��� ý������ ���ٸ� �Ѽյ����ʾ��� 
			else
				return true; 
		} catch (Exception e) {
			return true;
		}
	}

	// get checksum from byte data
	public long getchecksum(byte[] sendData) { //ý���� ������� �˰���
		long sum = 0;
		ByteArrayInputStream bais = new ByteArrayInputStream(sendData);
		CheckedInputStream cis = new CheckedInputStream(bais, new Adler32()); 
		byte readBuffer[] = new byte[5];
		try {
			while (cis.read(readBuffer) >= 0) {
				long value = cis.getChecksum().getValue(); 
				/*
					5 byte�� ���ҵ� �����͵��� checksum�� ��� ���� ������ message�� �� checksum ���� �� Sender�� ���� checksum�� ���� �� �ֵ��� ���ݴϴ�.
				*/
				// System.out.println("The value of checksum is " + value);
				sum += value;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return sum;
	}
}
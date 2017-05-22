import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.util.Scanner;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;

class Sender {
	private final static int DEFAULT_NET_PORT = 9000; //������ �⺻ ��Ʈ ���� 9000���� 
	public static void main(String[] args) throws Exception {
		// check if the number of command line argument is 4
		if (args.length != 1)
		/*
			1. ���ڰ����� port�� �־�������� ���� ���θ� Ȯ���մϴ�.
		*/
			new Sender("localhost", DEFAULT_NET_PORT);  //�������� ��Ʈ�� ���÷� 
		else
			new Sender("localhost", Integer.parseInt(args[0])); //java sender ù��°���� ���������� ����ȯ 
	}

	public Sender(String host, int port) throws Exception {
		Scanner sc = new Scanner(System.in); //�Է��� ���� sc�� ���� 
		while (sc.hasNextLine()) { //sc�� �����ٿ��� ���ڿ� line���� �����, ȣ��Ʈ�� ��Ʈ�� ������� �޼������� 
			String line = sc.nextLine();
			sendMessage(line, host, port);
			Thread.sleep(20); // Nop ���� (NO operation)
			/*
				2. �� �κе� ���ͳ� ���� ������ �����ϱ� �����Դϴ�.
			*/
		}
	}

	int seq = 0;

	public void sendMessage(String message, String host, int port) throws Exception {

		/* Sending */
		InetAddress senderAddress = InetAddress.getByName(host); //ȣ��Ʈ���� �̸��� ������ ip�ּ� 
		DatagramSocket sender = new DatagramSocket(); // �����ͱ׷� ���� ����
		sender.setSoTimeout(500); //500��ŭ Timeout �ð��� ���� (�ð� �ִ� ���� ����)	
		while (true) {
			long sendChecksum = getchecksum((seq + ";" + message).getBytes()); 
			/*
				3. �߿��� �κ��Դϴ�. packet�� checksum�� �����Ͽ� �������μ� receiver�� ���� message�� checksum�� ���Ͽ� �������� ��Ŷ���� Ȯ���� �� �ֵ��� ���ݴϴ�.
			*/
			String packet = makePacket(sendChecksum, seq, message); // ý����, ������, �޼����� ������� ��Ŷ�� ���� 

			byte[] sendData = packet.getBytes(); //��Ŷ�� ����Ʈ�� ���� 

			DatagramPacket sendPkt = new DatagramPacket(sendData, sendData.length, senderAddress, port); // �۽��� ������ ,����, �ּ�, ��Ʈ�� �����ͱ׷� ��Ŷ�� ���� (�۽���Ŷ)
			sender.send(sendPkt); //�۽���Ŷ ���� 

			/* Receiving */
			byte[] inBuffer = new byte[1024]; //1024ũ���� ���۸� ����
			DatagramPacket rcvedPkt = new DatagramPacket(inBuffer, inBuffer.length); //���ۿ� ���۱��̷� �����ͱ׷� ��Ŷ (������ ����)

			try {
				sender.receive(rcvedPkt); // ������ ��Ŷ ����
			} catch (SocketTimeoutException e) { // �ð��ʰ��ϸ�  ���������� �ѱ� �Ƹ� ������������ 500�� ���ε�.
				continue;
			}

			String rcvedData = new String(rcvedPkt.getData(), 0, rcvedPkt.getLength()); //������Ŷ �����Ϳ� ���� �� �����ͱ׷����� ���� 

			// Received packet's checksum
			if (rcvedData.equals("" + this.seq)) { //������ �������� ���ϰ�, ���ϴٸ� ������ 1�� �ø� �׸��� �۽���Ŷ ���� 
				//System.out.println("ACK" + rcvedData);
				this.seq++;
				sender.close();
				break;
			} else { //�׷��������� ���������� �������� �ѱ� 
				//System.out.println("ACK" + rcvedData);
				continue;
			}
		}
	}

	private String makePacket(long checksum, int seq, String message) { // ��Ŷ���� ý�� ,���� ,�޼��� 
		return checksum + ";" + seq + ";" + message; // ý��, seq , �޼��� ��ȯ 
	}

	// Calculate checksum for checking corrupted packet �Ѽյ� ��Ŷ�� �˻��ϱ����� ý�� ��� 
	public long getchecksum(byte[] sendData) { //�۽ŵ����� �����
		long sum = 0; 
		ByteArrayInputStream bais = new ByteArrayInputStream(sendData);
		CheckedInputStream cis = new CheckedInputStream(bais, new Adler32()); 
		byte readBuffer[] = new byte[5]; //�迭 5ũ��� ���� 
		try {
			while (cis.read(readBuffer) >= 0) { // �������۰� 0���� ũ�ٸ� 
				long value = cis.getChecksum().getValue(); //ý���� ���� value �� �������
				// System.out.println("The value of checksum is " + value);
				sum += value;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return sum;
	}
	public void checksumError() {
		System.out.println("Checksum Error");
	}
}
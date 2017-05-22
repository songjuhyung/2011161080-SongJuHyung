import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.util.Scanner;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;

class Sender {
	private final static int DEFAULT_NET_PORT = 9000; //센더측 기본 포트 설정 9000으로 
	public static void main(String[] args) throws Exception {
		// check if the number of command line argument is 4
		if (args.length != 1)
		/*
			1. 인자값으로 port를 넣어줬는지에 대한 여부를 확인합니다.
		*/
			new Sender("localhost", DEFAULT_NET_PORT);  //센더측에 포트를 로컬로 
		else
			new Sender("localhost", Integer.parseInt(args[0])); //java sender 첫번째값을 정수형으로 형변환 
	}

	public Sender(String host, int port) throws Exception {
		Scanner sc = new Scanner(System.in); //입력한 값을 sc로 만듬 
		while (sc.hasNextLine()) { //sc의 다음줄에는 문자열 line으로 만들고, 호스트와 포트를 기반으로 메세지보냄 
			String line = sc.nextLine();
			sendMessage(line, host, port);
			Thread.sleep(20); // Nop 역할 (NO operation)
			/*
				2. 이 부분도 인터넷 지연 현상을 방지하기 위함입니다.
			*/
		}
	}

	int seq = 0;

	public void sendMessage(String message, String host, int port) throws Exception {

		/* Sending */
		InetAddress senderAddress = InetAddress.getByName(host); //호스트에서 이름을 가져옴 ip주소 
		DatagramSocket sender = new DatagramSocket(); // 데이터그램 소켓 만듬
		sender.setSoTimeout(500); //500만큼 Timeout 시간을 설정 (시간 최대 길이 설정)	
		while (true) {
			long sendChecksum = getchecksum((seq + ";" + message).getBytes()); 
			/*
				3. 중요한 부분입니다. packet에 checksum을 포함하여 보냄으로서 receiver가 받은 message와 checksum을 비교하여 정상적인 패킷인지 확인할 수 있도록 해줍니다.
			*/
			String packet = makePacket(sendChecksum, seq, message); // 첵섬값, 시퀀스, 메세지를 기반으로 패킷을 만듬 

			byte[] sendData = packet.getBytes(); //패킷의 바이트를 얻어옴 

			DatagramPacket sendPkt = new DatagramPacket(sendData, sendData.length, senderAddress, port); // 송신측 데이터 ,길이, 주소, 포트로 데이터그램 패킷을 만듬 (송신패킷)
			sender.send(sendPkt); //송신패킷 보냄 

			/* Receiving */
			byte[] inBuffer = new byte[1024]; //1024크기의 버퍼를 만듬
			DatagramPacket rcvedPkt = new DatagramPacket(inBuffer, inBuffer.length); //버퍼와 버퍼길이로 데이터그램 패킷 (수신측 만듬)

			try {
				sender.receive(rcvedPkt); // 수신측 패킷 받음
			} catch (SocketTimeoutException e) { // 시간초과하면  다음순서로 넘김 아마 위에서설정한 500의 값인듯.
				continue;
			}

			String rcvedData = new String(rcvedPkt.getData(), 0, rcvedPkt.getLength()); //수신패킷 데이터와 길이 얻어서 데이터그램으로 만듬 

			// Received packet's checksum
			if (rcvedData.equals("" + this.seq)) { //순서가 동등한지 비교하고, 비교하다면 순서를 1개 늘림 그리고 송신패킷 끝냄 
				//System.out.println("ACK" + rcvedData);
				this.seq++;
				sender.close();
				break;
			} else { //그렇지않으면 수신측에서 다음으로 넘김 
				//System.out.println("ACK" + rcvedData);
				continue;
			}
		}
	}

	private String makePacket(long checksum, int seq, String message) { // 패킷만듬 첵섬 ,순서 ,메세지 
		return checksum + ";" + seq + ";" + message; // 첵섬, seq , 메세지 반환 
	}

	// Calculate checksum for checking corrupted packet 훼손된 패킷을 검사하기위한 첵섬 계산 
	public long getchecksum(byte[] sendData) { //송신데이터 끌어옴
		long sum = 0; 
		ByteArrayInputStream bais = new ByteArrayInputStream(sendData);
		CheckedInputStream cis = new CheckedInputStream(bais, new Adler32()); 
		byte readBuffer[] = new byte[5]; //배열 5크기로 설정 
		try {
			while (cis.read(readBuffer) >= 0) { // 읽은버퍼가 0보다 크다면 
				long value = cis.getChecksum().getValue(); //첵섬의 값을 value 로 집어넣음
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
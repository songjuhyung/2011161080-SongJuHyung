import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

class Receiver {
	private final static int DEFAULT_RCV_PORT = 9001; //수신측 포트는 기본값으로 9001 사용
	public static void main(String[] args) throws Exception {
		// check if the number of command line argument is 4
		if (args.length != 1) 
			new Receiver(DEFAULT_RCV_PORT); 
		else
			new Receiver(Integer.parseInt(args[0])); //아니라면 처음 0번째의 값을 집어넣음 
	}

	public Receiver(int port) throws Exception {
		// Do not change this 
		DatagramSocket socket = new DatagramSocket(port); 
		while (true) { // 소켓의 메세지를 항상 받아오기만 함.
			String message = receiveMessage(socket);
			System.out.println(message);
		}
	}

	int seq = 0; //처음 seq 값은 0 

	public String receiveMessage(DatagramSocket socket) throws Exception { 
		while (true) {
			int seq = -1; //초기설정인듯 -1값으로 만들어서 우선 
			int receiveChecksum = -1;
			String message = null; //메세지도 비운상태 
			int ack = -1;
			/* Receiving */
			byte[] inBuffer = new byte[1024]; // 1024의 버퍼크기만듬 
			DatagramPacket rcvedPkt = new DatagramPacket(inBuffer, inBuffer.length); //버퍼와 버퍼길이만큼 데이터그램 패킷을 만듬 
			socket.receive(rcvedPkt); // 수신측 패킷을 받아들임
			String rcvedData = new String(rcvedPkt.getData(), 0, rcvedPkt.getLength()); 

			// Check corrupted
			if (rcvedData.indexOf(";") == -1 || isCorrupted(rcvedData)) { 
				InetAddress receiverAddress = rcvedPkt.getAddress();  // 받은 패킷주소를 사용함
				int receiverPort = rcvedPkt.getPort(); //받은 패킷의 포트를 얻어옴 

				byte[] outBuffer = new String("" + (this.seq - 1)).getBytes(); 
				DatagramPacket sendPkt = new DatagramPacket(outBuffer, outBuffer.length, receiverAddress, receiverPort);//수신측 주소, 포트, 아웃버퍼와 아웃버퍼 길이로 송신패킷 형성 

				socket.send(sendPkt); //패킷 보냄
				//System.out.println("Packet corrupted!"); 
				continue; //충돌되었다고 알림 그리고 순서넘겨버림 
			} else
				rcvedData = rcvedData.substring(rcvedData.indexOf(";") + 1); 
			if (rcvedData.indexOf(";") == -1 || illegalSeq(rcvedData)) {
				InetAddress receiverAddress = rcvedPkt.getAddress(); //패킷에서 주소 얻어옴 
				int receiverPort = rcvedPkt.getPort();//포트 마찬가지로얻음 

				byte[] outBuffer = new String("" + (this.seq - 1)).getBytes(); //위와 마찬가지로-1 만큼빼서 바이트 얻어옴 
				DatagramPacket sendPkt = new DatagramPacket(outBuffer, outBuffer.length, receiverAddress, receiverPort); // 마찬가지로 송신측 패킷만듬 

				socket.send(sendPkt);
				//System.out.println("Illegal sequence!");
				continue;
			} else
				rcvedData = rcvedData.substring(rcvedData.indexOf(";") + 1); //그렇지않으면 아까와 마찬가지로 +1 더함 
			message = rcvedData; //메세지에 데이터 수신함 

			/* Sending (ACK) */
			InetAddress receiverAddress = rcvedPkt.getAddress(); //ip주소 얻어옴 받은 패킷에서 
			int receiverPort = rcvedPkt.getPort(); //포트 얻어옴

			byte[] outBuffer = new String("" + this.seq).getBytes(); //아웃버퍼 에서 현재 순서번호 얻어옴 
			DatagramPacket sendPkt = new DatagramPacket(outBuffer, outBuffer.length, receiverAddress, receiverPort); // ack를 담아보낼 패킷형성

			socket.send(sendPkt); //ack보냄 

			this.seq++;// 순서 1개 증가시킴
			return rcvedData; //데이터 반환 

		}
	}

	private boolean illegalSeq(String rcvedData) { 
		try {
			int seq = Integer.parseInt(rcvedData.substring(0, rcvedData.indexOf(";"))); // illegal정의하려고, 0에서부터 rcved데이터에서 ;까지 잘라내서 
			if (this.seq == seq) //만약 순서번호가 같으면 false  =legal하다는 의미
				return false;
				/*
					Sequence는 손실된 패킷을 확인하기 위해서 추가되었습니다. sequence순서대로 들어오지 않고 중간에 하나가 빠져서 들어오면 손실로 간주하고 재전송을 요청하게 됩니다.
					this.seq == seq 이면 정상적인 순서이기 때문에 legal 한 상황으로 처리됩니다.
				*/
			else
				return true; //그렇지않으면 illegal
		} catch (Exception e) {
			return true;
		}
	}

	// check received data is corrupted 데이터가 훼손돼었는지 검사하려고 
	private boolean isCorrupted(String rcvedData) {
		try {
			long checksum = Long.parseLong(rcvedData.substring(0, rcvedData.indexOf(";"))); //이번엔 잘라낸값을 Long 으로 변환시켜 첵섬이라는 long 형 변수에 집어넣음 
			rcvedData = rcvedData.substring(rcvedData.indexOf(";") + 1); // ;+1크기만큼 잘라내서 데이터를 집어넣음 
			/*
				rcvedData.substring(0, rcvedData.indexOf(";"))는 rcvedData의 맨 처음부터 ";"가 발견되기 전까지, 즉 checksum 부분을 의미합니다.
				rcvedData.substring(rcvedData.indexOf(";") + 1)는 rcvedData의 ";"가 발견된 바로 다음자리부터 나머지 뒤까지, 즉 message 부분을 의미합니다.
			*/
			//System.out.println("Checksum : " + checksum);
			//System.out.println("Datachecksum : " + getchecksum(rcvedData.getBytes()));
			if (checksum == getchecksum(rcvedData.getBytes()))
				return false; //얻은 값과 받은데이터값의 첵섬값이 같다면 훼손되지않았음 
			else
				return true; 
		} catch (Exception e) {
			return true;
		}
	}

	// get checksum from byte data
	public long getchecksum(byte[] sendData) { //첵섬을 얻기위한 알고리즘
		long sum = 0;
		ByteArrayInputStream bais = new ByteArrayInputStream(sendData);
		CheckedInputStream cis = new CheckedInputStream(bais, new Adler32()); 
		byte readBuffer[] = new byte[5];
		try {
			while (cis.read(readBuffer) >= 0) {
				long value = cis.getChecksum().getValue(); 
				/*
					5 byte로 분할된 데이터들의 checksum을 모두 더해 보내는 message의 총 checksum 값을 얻어서 Sender가 보낸 checksum과 비교할 수 있도록 해줍니다.
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
# 2011161080-SongJuHyung
UDP를 이용한 파일 전송 프로그램  
전체적인 프로그램의 구조는 
-------------
Server - Unrelitive Server - Client로 구성되며
> - Server는 파일을 받는 역할
> - Client는 파일을 보내는 역할 
> - Unrelitive Server는 고의적으로 패킷을 손상 및 손실을 일으키는 서버를 두어서 시뮬레이팅 하는 역할  
![setting](./setting.png)  
> - Sender에서 메세지를 보내면 UnreliNET 에서 보실 수 있는것과 같이 UnreliNE T을 실행할때 설정해둔 확률로 패킷이 손상(corrupted) 또는 손실(dropped)을 시킵니다.
> - 하지만 Receiver에서 보시는 것과 같이 패킷이 손상되어도 ACK, checksum, sequence를 통해 재전송 요청으로 정상도착할 수 있도록 구성하였습니다.

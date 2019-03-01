package fSupport;

public interface FSocket {
	//send message through socket
	public void Send(String msg, int threadID) throws Exception;
	
	//receive message from socket;
	public String Receive(int threadID) throws Exception;
}

package fSupport;

import java.net.Socket;

public interface FSocket {
	//create digest for connection verification
	public void CreateDigest() throws Exception;
	
	//digest verification
	public boolean Handshake(Socket s) throws Exception;
	
	//send message through socket
	public void Send(String msg, int threadID) throws Exception;
		
	//receive message from socket;
	public String Receive(int threadID) throws Exception;
}

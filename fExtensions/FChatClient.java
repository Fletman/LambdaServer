package fExtensions;

import fSupport.FClient;
import fSupport.FThread;
import fSupport.FThreadFunc;

public class FChatClient extends FClient {

	//used for outgoing connections, thread1 for incoming
	private FThread clientThread2;
	private FThreadFunc f2 = null;
	private Object arg2;
	
	public FChatClient(int port, String hostName, String pass) {
		super(port, hostName, pass);
	}

	//overloaded method, accepts lambda functions for incoming/outgoing communications
	public void SetLambda(FThreadFunc incoming, FThreadFunc outgoing, Object param1, Object param2)
	{
		f = incoming;
		arg = param1;
		
		f2 = outgoing;
		arg2 = param2;
	}
	
	@Override
	public void StartThread(int id) throws Exception
	{
		if(!Handshake(null))
		{
			System.out.println("Failed connection authentication");
			sock.close();
			return;
		}
		
		clientThread = new FThread(id, f, arg, this);
		clientThread.start();
		
		clientThread2 = new FThread(id, f2, arg2, this);
		clientThread2.start();
		
		clientThread.join();
		clientThread2.join();
	}
}

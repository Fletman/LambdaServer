package fExtensions;
import fSupport.FServer;

//example extension of FServer class
public class FChatServer extends FServer{
	
	public FChatServer(int p, String pass) {
		super(p, pass);
	}
	
	public FChatServer(int p, int threadCount, String pass) {
		super(p, threadCount, pass);
	}

	//send message to all connected clients (except original sender)
	public void Broadcast(String m, int sourceID) throws Exception
	{		
		for(int i = 0; i < oStream.length; i++)
		{
			if(i != sourceID && oStream[i] != null)
			{
				Send(m, i);
			}
		}
	}
	
	

}

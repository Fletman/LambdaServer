package fClient;

import java.net.*;

import fSupport.*;

import java.lang.Exception;
import java.io.InputStream;
import java.io.OutputStream;

public class FClient implements FThreadOwner, FSocket
{
	private Socket sock; //client's communications socket
	private InputStream iStream; //socket receiving stream
	private OutputStream oStream; //socket sending stream
	private final int BUFF_SIZE = 128; //maximum size of input/output buffer
	
	private FThread clientThread; //thread the lambda function will run
	private FThreadFunc f = null; //lambda function
	private Object arg; //argument for lambda function
	
	
	public FClient(int port, String hostName)
	{
		try{
			sock = new Socket(InetAddress.getByName(hostName), port, null, 0);
			iStream = sock.getInputStream();
			oStream = sock.getOutputStream();
		}
		catch(Exception e)
		{
			System.out.println("Failed to create socket");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	//set lambda function for a thread to run
	@Override
	public void SetLambda(FThreadFunc func, Object param)
	{
		f = func;
		arg = param;
	}
	
	public void Start() throws Exception
	{
		//if no function assigned, don't execute
		if(f == null)
		{
			throw new Exception("Invalid Lambda Function");
		}
		
		StartThread(0);
	}
	
	@Override
	public void StartThread(int t) throws Exception
	{
		clientThread = new FThread(t, f, arg, this);
		clientThread.start();
		clientThread.join();
	}

	@Override
	public void CloseThread(int t) throws Exception
	{
		System.out.println("Thread [" + t + "] exiting");
		
		iStream.close();
		oStream.close();
		
		iStream = null;
		oStream = null;
		
		sock.close();
		sock = null;
	}

	
	//send message to server
	@Override
	public void Send(String msg, int id) throws Exception
	{
		int packets = (int) Math.ceil((double)msg.length()/BUFF_SIZE);
				
		byte[] b = msg.getBytes();
				
		for(int i = 0; i < packets; i++)
		{
			oStream.write(b, i * BUFF_SIZE, Math.min(b.length, BUFF_SIZE));
		}
	}
	
	//receive message from server
	//NOTE: id (probably) not necessary for client-side implementation
	@Override
	public String Receive(int id) throws Exception
	{
		
		byte[] recv = new byte[BUFF_SIZE];
		String msg = "";
		int len;
		
		//read input in chunks to allow variable message sizes
		do{
			recv = new byte[BUFF_SIZE]; //clear out input buffer
			len = iStream.read(recv); //read byte array from socket
			msg += new String(recv, 0, len); //append bytes to output string
		}
		while(len == BUFF_SIZE);
						
		return msg;
	}	
}

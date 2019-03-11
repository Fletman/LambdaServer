package fSupport;

import java.net.*;
import java.lang.Exception;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

public class FClient implements FThreadOwner, FSocket
{
	protected Socket sock; //client's communications socket
	protected InputStream iStream; //socket receiving stream
	protected OutputStream oStream; //socket sending stream
	protected final int BUFF_SIZE = 128; //maximum size of input/output buffer
	
	protected FThread clientThread; //thread the lambda function will run
	protected FThreadFunc f = null; //lambda function
	protected Object arg; //argument for lambda function
	
	protected String password; //password used for handshake with Server
	protected byte[] clientDigest;
	
	
	public FClient(int port, String hostName, String pass)
	{
		try{
			sock = new Socket(InetAddress.getByName(hostName), port, null, 0);
			iStream = sock.getInputStream();
			oStream = sock.getOutputStream();
			
			password = pass;
			CreateDigest();
		}
		catch(Exception e)
		{
			System.out.println("Failed to create socket");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	@Override
	public void CreateDigest() throws Exception
	{
		clientDigest = MessageDigest.getInstance("SHA-256").digest(password.getBytes());
	}
	
	/**
	 * @param server not used here; only one (known) socket allowed per client
	 */
	@Override
	public boolean Handshake(Socket server)
	{
		try
		{
			Send(new String(clientDigest), 0);
			String result = Receive(0);
				
			return result.equals("Y");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
		
		return false;
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
		if(!Handshake(null))
		{
			System.out.println("Failed connection authentication");
			sock.close();
			return;
		}
		
		clientThread = new FThread(t, f, arg, this);
		clientThread.start();
		clientThread.join();
	}

	@Override
	public void CloseThread(int t) throws Exception
	{
		System.out.println("Thread [" + t + "] exiting");
		
		if(iStream != null)
		{
			iStream.close();
			iStream = null;
		}

		if(oStream != null)
		{
			oStream.close();
			oStream = null;
		}
		
		if(sock != null)
		{
			sock.close();
			sock = null;
		}
	}

	
	//send message to server
	@Override
	public void Send(String msg, int id) throws Exception
	{
		/*
		 * edge case where message length is a multiple of buffer size,
		 * causes Receive to get stuck indefinitely
		 */
		if(msg.length() % BUFF_SIZE == 0)
		{
			msg += '\0'; //increase message size by one with a null byte
		}
		
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

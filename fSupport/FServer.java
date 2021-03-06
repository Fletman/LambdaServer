package fSupport;

import java.net.*;
import java.lang.Exception;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

public class FServer implements FThreadOwner, FSocket
{
	protected final int DEFAULT_THREADS = 16; //default to allow maximum 16 threads
	
	//server info
	protected InetSocketAddress addr;
	protected ServerSocket listener;
	protected FThread listenThread;

	//list of sockets and corresponding threads
	protected Socket[] connections;
	protected FThread[] connThreads;
	protected InputStream[] iStream;
	protected OutputStream[] oStream;
	
	protected final int BUFF_SIZE = 128; //128 bytes per input/output buffer
	
	//thread lambda function & argument
	protected FThreadFunc f = null;
	protected Object arg;
	
	//password digest that incoming clients must present
	protected String password;
	protected byte[] serverDigest;
	
	public FServer(int port, String pass)
	{	
		try{
			Setup(port, DEFAULT_THREADS);	
			password = pass;
			CreateDigest();
		}
		catch(Exception e)
		{
			System.out.println("[" + port + "]Failed to create new FServer:");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public FServer(int port, int threadCount, String pass)
	{	
		try{
			Setup(port, threadCount);		
			password = pass;
			CreateDigest();
		}
		catch(Exception e)
		{
			System.out.println("[" + port + "]Failed to create new FServer:");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 * 
	 * @param port Port number for server socket
	 * @param threads Max number of threads allowed on server
	 * @throws Exception
	 */
	protected void Setup(int port, int threads) throws Exception
	{
		addr = new InetSocketAddress(InetAddress.getLocalHost(), port);
		listener = new ServerSocket();
		listener.bind(addr);
		
		connections = new Socket[threads];
		connThreads = new FThread[threads];
		
		iStream = new InputStream[threads];
		oStream = new OutputStream[threads];
	}
	
	@Override
	public void CreateDigest() throws Exception
	{
		serverDigest = MessageDigest.getInstance("SHA-256").digest(password.getBytes());
	}
	
	@Override
	public boolean Handshake(Socket client) throws Exception
	{
		InputStream clientIn = client.getInputStream();
		OutputStream clientOut = client.getOutputStream();
		
		String clientDigest = "";		
		byte[] recv;
		int len;
		
		//read input in chunks to allow variable message sizes
		do{
			recv = new byte[BUFF_SIZE]; //clear out input buffer
			len = clientIn.read(recv); //read byte array from socket
			clientDigest += new String(recv, 0, len); //append bytes to output string
		}
		while(len == BUFF_SIZE);
		
		boolean isAccepted = MessageDigest.isEqual(serverDigest, clientDigest.getBytes());
		
		if(!isAccepted)
		{
			clientOut.write(new String("N").getBytes());
		}
		else
		{
			clientOut.write(new String("Y").getBytes());
		}
		
		//TODO: does order of closing matter here? Let's find out
		//clientIn.close();
		//clientOut.close();
		
		return isAccepted;
	}
	
	@Override
	public void SetLambda(FThreadFunc lambda, Object param)
	{
		f = lambda;
		arg = param;
	}
	
	public void Start() throws Exception
	{
		if(f == null)
		{
			throw new Exception("Invalid Lambda Function");
		}
		
		//create listening socket in new thread
		FThreadFunc handle = CreateHandler();
		listenThread = new FThread(-1, handle, null, this);
		listenThread.start();
		
		listenThread.join();
	}
	
	protected FThreadFunc CreateHandler()
	{
		FThreadFunc handle = (obj, id) ->
		{
			Socket tempSock;
			while(true)
			{
				System.out.println("Searching for connections...");
				
				try{
					tempSock = listener.accept(); //blocking call, waits for a connection
				}
				catch(IOException e)
				{
					//IOException thrown when listener socket is closed (see: Kill()), used for exit condition
					break;
				}
				
				System.out.println("Connection acquired.");
				
				if(!Handshake(tempSock))
				{
					System.out.println("Invalid Digest presented");
					tempSock = null;
					continue;
				}
				
				for(int i = 0; i < connections.length; i++)
				{
					if(connections[i] == null)
					{
						connections[i] = tempSock;
						tempSock = null; //removing extra reference for garbage collector when thread finishes
						StartThread(i);
						break;
					}
				}
				
				if(tempSock != null)
				{
					System.out.println("Connection rejected; all threads in use.");
					tempSock = null;
				}
			}
		};
		
		return handle;
	}
	
	//send message to client
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
		
		if(oStream[id] == null)
		{
			System.out.println("WHAT " + id);
			System.exit(-1);
		}
		
		for(int i = 0; i < packets; i++)
		{
			oStream[id].write(b, i * BUFF_SIZE, Math.min(b.length, BUFF_SIZE));
		}
	}
	
	//receive message from client
	@Override
	public String Receive(int id) throws Exception
	{
		byte[] recv;
		String msg = "";
		int len;
		
		//read input in chunks to allow variable message sizes
		do{
			recv = new byte[BUFF_SIZE]; //clear out input buffer
			len = iStream[id].read(recv); //read byte array from socket
			msg += new String(recv, 0, len); //append bytes to output string
		}
		while(len == BUFF_SIZE); //TODO: is there an edge case when receiving exactly BUFF_SIZE?
				
		return msg;
	}	
	
	//start connection in new thread
	@Override
	public void StartThread(int t) throws Exception
	{
		connThreads[t] = new FThread(t, f, arg, this);
		iStream[t] = connections[t].getInputStream();
		oStream[t] = connections[t].getOutputStream();
		connThreads[t].start();
	}
	
	//end connection, close specified thread
	@Override
	public void CloseThread(int t) throws Exception
	{
		System.out.println("Closing Thread [" + t + "]");
		
		//handler thread closes differently
		if(t == -1)
		{
			return;
		}
		
		iStream[t].close();
		oStream[t].close();
		iStream[t] = null;
		oStream[t] = null;
		
		connections[t].close();
		connections[t] = null;
		connThreads[t] = null;
	}
		
	//kill server
	public void Kill() throws Exception
	{
		System.out.println("Received command to kill server. Exiting...");
		
		listener.close(); //NOTE: this will (intentionally) cause an IOException
	}

	
}

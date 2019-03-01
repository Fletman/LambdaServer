package fServer;

import java.net.*;
import java.lang.Exception;

import fSupport.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FServer implements FThreadOwner, FSocket
{
	private final int DEFAULT_THREADS = 16; //default to allow maximum 16 threads
	
	//server info
	private InetSocketAddress addr;
	private ServerSocket listener;
	private FThread listenThread;

	//list of sockets and corresponding threads
	private Socket[] connections;
	private FThread[] connThreads;
	private InputStream[] iStream;
	private OutputStream[] oStream;
	
	private final int BUFF_SIZE = 128; //128 bytes per input/output buffer
	
	//thread lambda function & argument
	private FThreadFunc f = null;
	private Object arg;
	
	
	public FServer(int p)
	{	
		try{
			Setup(p, DEFAULT_THREADS);			
		}
		catch(Exception e)
		{
			System.out.println("[" + p + "]Failed to create new socket:");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public FServer(int p, int threadCount)
	{	
		try{
			Setup(p, threadCount);			
		}
		catch(Exception e)
		{
			System.out.println("[" + p + "]Failed to create new socket:");
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
	private void Setup(int port, int threads) throws Exception
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
	
	private FThreadFunc CreateHandler()
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
		int packets = (int) Math.ceil((double)msg.length()/BUFF_SIZE);
		byte[] b = msg.getBytes();
		
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
		while(len == BUFF_SIZE);
				
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

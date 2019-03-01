package fSupport;
import java.lang.Thread;

public class FThread extends Thread
{
	private int threadID; //ID of FThread
	private FThreadFunc f; //lambda function to run
	private Object arg; //argument for lambda function
	private FThreadOwner owner; //Object owning this thread
	
	/**
	 * 
	 * @param id ID of new thread
	 * @param f lambda function for thread to run
	 * @param o parameter for lambda function
	 * @param owner FServer owning this thread
	 */
	public FThread(int id, FThreadFunc f, Object o, FThreadOwner tOwner)
	{
		this.threadID = id;
		this.f = f;
		this.arg = o;
		this.owner = tOwner;
		
		System.out.println("New thread [" + threadID + "] created");
	}
	
	public void run()
	{
		System.out.println("Thread [" + threadID + "] starting");
		try{
			f.function(arg, threadID);
			this.owner.CloseThread(this.threadID);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public int getID()
	{
		return this.threadID;
	}
}

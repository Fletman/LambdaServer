package fSupport;

public interface FThreadOwner {	
	//create and start a thread
	public void StartThread(int t) throws Exception;
	
	//end a running thread
	public void CloseThread(int t) throws Exception;
	
	//set lambda function for threads to run
	public void SetLambda(FThreadFunc lambda, Object param);
}

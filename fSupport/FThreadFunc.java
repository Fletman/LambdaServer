package fSupport;
import java.lang.Exception;

//lambda function for a FThread to run with an Object argument
public interface FThreadFunc{
	public void function(Object o, int threadID) throws Exception;
}
package fServer;

import fSupport.FThreadFunc;
import java.lang.Exception;


public class ExampleServer {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String message = "Greetings from the Server";
		
		//new server using port #50093
		FServer testServ = new FServer(50093);
		try {
			//NOTE: id field filled in automatically
			FThreadFunc lambda = (obj, id) ->
			{
				System.out.println("\n" + testServ.Receive(0) + "\n");
				testServ.Send((String)obj, id);
				
				testServ.Kill();
			};
			
			testServ.SetLambda(lambda, message);
			
			testServ.Start();			
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}

}

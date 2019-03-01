package fClient;

import java.net.InetAddress;
import java.lang.Exception;
import fSupport.FThreadFunc;

public class ExampleClient {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String message = "Hello from Client";

		try {
			//initialize client (on same machine as server)
			FClient testClient = new FClient(50093, InetAddress.getLocalHost().getHostName());
			
			//function for client to perform
			//NOTE: id param filled in automatically
			FThreadFunc lambda = (obj, id) ->
			{
				testClient.Send((String)obj, id);
				System.out.println("\n" + testClient.Receive(id) + "\n");
			};
			
			//assign lambda function to client thread
			testClient.SetLambda(lambda, message);
			
			//start running client
			testClient.Start();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}

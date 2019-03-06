package fExamples;
import fExtensions.FChatServer;
import fSupport.FThreadFunc;

public class ChatServerExample {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		FChatServer chatServ = new FChatServer(12345, "chatPassword");
		FThreadFunc lambda = (obj, id) -> 
		{
			while(true)
			{
				try{
					String s = chatServ.Receive(id);
					
					if(s.equals("/close")) //exit condition for client
					{break;}
					
					chatServ.Broadcast(s, id);
					System.out.println("[" + id + "] " + s);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				
			}
		};
		
		chatServ.SetLambda(lambda, null);
		
		try{
			chatServ.Start();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
	}
}

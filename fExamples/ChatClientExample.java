package fExamples;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

import fExtensions.FChatClient;
import fSupport.FThreadFunc;

public class ChatClientExample {

	public static void main(String[] args) throws UnknownHostException {
		// TODO Auto-generated method stub

		//running on local machine because I'm lazy
		FChatClient chatCli = new FChatClient(12345, InetAddress.getLocalHost().getHostName(), "chatPassword");

		Scanner keyboard = new Scanner(System.in); //console input from user
		
		//handler for input from server
		FThreadFunc inLambda = (obj, id) ->
		{
			String input; //user input from console
			
			while(true)
			{
				if(keyboard.hasNextLine())
				{
					input = keyboard.nextLine();
					
					if(input.equals(""))
					{
						continue;
					}
					
					chatCli.Send(input, id);
					
					if(input.equals("/close")) //exit condition
					{
						break;
					}
				}
			}
		};
		
		//handler for output to server
		FThreadFunc outLambda = (obj, id) ->
		{
			while(true)
			{
				try{
					System.out.println(chatCli.Receive(id));
				}
				catch(SocketException e)
				{
					break; //exit condition; socket closed
				}
			}
		};
		
		chatCli.SetLambda(inLambda, outLambda, null, null);
		try {
			chatCli.Start();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
		
		keyboard.close();
	}

}

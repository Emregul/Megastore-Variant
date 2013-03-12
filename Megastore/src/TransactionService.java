import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.io.IOException;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import java.util.*;

public class TransactionService 
{	
	private WriteLog log;
	private int listenPort;
	
	public TransactionService(int listenPort)
	{
		this.listenPort = listenPort;
		new ConnectionHandler().start();
		/*Creates a log for an entity key=1*/
		try {
			log = new WriteLog("One");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*Starts the position 0 of the log*/
		try {
			log.startPosition(0);
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
	}
	class ConnectionHandler extends Thread
	{//Listener for Server, just listens for connection
		private ServerSocket serverSocket;

		public ConnectionHandler()
		{	//create a serversocket given port
			try {
				this.serverSocket = new ServerSocket(listenPort);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void run()
		{
			Socket newSocket;
			try {
				while (true){
					newSocket = serverSocket.accept();//Accept socket
					System.out.println("Client Connection accepted: "+newSocket.getInetAddress().getHostAddress());
					//launch thread
					MessageListener newThread = new MessageListener(newSocket);
					newThread.start();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
	}
	class MessageListener extends Thread
	{//Thread that reads from client and sends a message
		private Socket socket;
		private PrintWriter toTransactionClient;
		private BufferedReader fromTransactionClient;
		private String message = null;
		public MessageListener(Socket socket)
		{
			this.socket = socket;
			try {
				toTransactionClient = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
				fromTransactionClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		public void run()
		{
			try {
				message = fromTransactionClient.readLine();
				MessageContent parsedMessage = Messages.parse(message);
				//parseMessage
				//call corresponding Method
				//send to client using InputStream
				String response = "test";
				toTransactionClient.println(response);
				toTransactionClient.flush();
				socket.close();
				//close connections
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
	public void receive_prepare(int cid, int propNum) {
		boolean keepTrying = true;
		while(keepTrying) {
			try {
				//position p
				long p = log.getPosition();
				WriteLogReadResult result = log.read(p);
				if(propNum > result.vNextBal) {
					if(log.checkAndWrite(p,result.vNextBal,propNum)) {
						String message = Messages.sendPrepareSuccessFromServiceToClient(cid, result.vBalloutNumber, result.value);
						//send(message);
						keepTrying = false;
					}
				}
				else {
					String message = Messages.sendPrepareFailureFromServiceToClient(cid,result.vBalloutNumber,result.value);
					//send(cid, message);
					keepTrying = false;
				}
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	/**
	 * receive for accept message
	 * @param cid
	 * @param propNum
	 * @param value
	 */
	public void receive_accept(int cid, int propNum, HashMap<String,Long> value) {
		try {
			String message = new String();
			if(log.checkAndWrite2(log.getPosition(), propNum, value)) {
				//success
				message = Messages.sendAcceptFromServiceToClient(cid,true);
				
			}
			else {
				//failure
				message = message = Messages.sendAcceptFromServiceToClient(cid,false);
				
			}
			//send message
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * receive for apply message
	 * @param cid
	 * @param propNum
	 * @param value
	 */
	public void receive_apply(int cid, int propNum, HashMap<String,Long> value) {
		try {
			log.write(log.getPosition(),propNum,value);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) 
	{
		TransactionService service1 = new TransactionService(11115);
	}

}

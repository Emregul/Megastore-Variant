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
	private long id;
	private int listenPort;
	
	public TransactionService(long id,int listenPort)
	{
		this.listenPort = listenPort;
		this.id = id;
		new ConnectionHandler().start();
		/*Creates a log for an entity key=1*/
		try {
			log = new WriteLog("log" +id);
			System.out.println("adas");
			log.erase();
			log = new WriteLog("log" +id);
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
				InternalMessageLog.WriteLog("server"+ id, " received " + parsedMessage.toString() );
				if(parsedMessage.messageType.equals("PREPARE")) {
					receive_prepare(parsedMessage.cid, parsedMessage.propositionNumber,toTransactionClient);
				}
				else if(parsedMessage.messageType.equals("ACCEPT")) {
					receive_accept(parsedMessage.cid, parsedMessage.propositionNumber,parsedMessage.propositionValues,toTransactionClient);
				}
				else if(parsedMessage.messageType.equals("APPLY")) {
					receive_apply(parsedMessage.cid, parsedMessage.propositionNumber,parsedMessage.propositionValues);
				}
				else
					System.out.println("Error: Unrecognized message format received from client");
				
				
				//parseMessage
				//call corresponding Method
				//send to client using InputStream
				socket.close();
				//close connections
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
	public synchronized void receive_prepare(int cid, long propNum, PrintWriter toTransactionClient) {
		boolean keepTrying = true;
		while(keepTrying) {
			try {
				//position p
				long p = log.getPosition();
				WriteLogReadResult result = log.read(p);
				System.out.println("log position"  + p);
				if(propNum > result.vNextBal) {
					if(log.checkAndWrite(p,result.vNextBal,propNum)) {
						InternalMessageLog.WriteLog("server"+ id, " sent " + Messages.sendPrepareSuccessFromServiceToClient(cid, result.vBalloutNumber, result.values));
						String message = Messages.sendPrepareSuccessFromServiceToClient(cid, result.vBalloutNumber, result.values);
						toTransactionClient.println(message);
						toTransactionClient.flush();
						keepTrying = false;
					}
				}
				else {
					InternalMessageLog.WriteLog("server"+ id, " sent " + Messages.sendPrepareFailureFromServiceToClient(cid,result.vBalloutNumber));
					String message = Messages.sendPrepareFailureFromServiceToClient(cid,result.vBalloutNumber);
					toTransactionClient.println(message);
					toTransactionClient.flush();
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
	public synchronized void receive_accept(int cid, long propNum, HashMap<String,String> value, PrintWriter toTransactionClient) {
		try {
			String message = new String();
			System.out.println(propNum);
			if(log.checkAndWrite(log.getPosition(), propNum, value)) {
				//success
				message = Messages.sendAcceptFromServiceToClient(cid,true);
				InternalMessageLog.WriteLog("server"+ id, " sent " + Messages.sendAcceptFromServiceToClient(cid,true));

			}
			else {
				//failure
				message = Messages.sendAcceptFromServiceToClient(cid,false);
				InternalMessageLog.WriteLog("server"+ id, " sent " + Messages.sendAcceptFromServiceToClient(cid,false));
			}
			//send message
			toTransactionClient.println(message);
			toTransactionClient.flush();
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
	public synchronized void receive_apply(int cid, long propNum, HashMap<String,String> value) {
		try {
			log.write(log.getPosition(),propNum,value);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) 
	{
		InternalMessageLog.init();
		TransactionService service1 = new TransactionService(1,11111);
		TransactionService service2 = new TransactionService(2,11112);
		TransactionService service3 = new TransactionService(3,11113);
	}

}

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
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

public class TransactionClient {
	//maintains the state
	private long propNum;
	private HashMap<Long,Transaction> ActiveTransactions;
	
	/**
	 * Constructor
	 */
	public TransactionClient() {
		propNum = 0;
		ActiveTransactions = new HashMap<Long,Transaction>();
	}
	
	
	public void SendMessagetoServerX(InetSocketAddress serverAddress, String message)
	{//Send a string message to a given server
		Socket socket;
		try {
			socket = new Socket(serverAddress.getHostName(),serverAddress.getPort());
			MessageSender newThread = new MessageSender(socket,message);
			newThread.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public class MessageSender extends Thread
	{//Thread that opens the socket connection and sends the message
		private Socket socket;
		private PrintWriter toTransactionServer;
		private BufferedReader fromTransactionServer;
		private String message;
		
		public MessageSender(Socket socket, String message)
		{
			this.socket = socket;
			this.message = message;
			
			try {
				toTransactionServer = new PrintWriter (new OutputStreamWriter(socket.getOutputStream()));
				fromTransactionServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		public void run()
		{
			try {
				//Parse given message send it to the server, read response from server and close the socket
				toTransactionServer.println(message);
				toTransactionServer.flush();
				message = fromTransactionServer.readLine();
				MessageContent parsedMessage = Messages.parse(message);
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    
		}
	}
	

	/**
	 * begin - a transaction with given transactionID
	 * @param transactionID
	 */
	public void begin(long transactionID) {
		Transaction t = new Transaction(transactionID);
		ActiveTransactions.put(transactionID,t);
	}
	
	
	/**
	 * read - key for transaction represented by transactionID
	 * @param transactionID
	 * @param key
	 * @return
	 */
	public String read(long transactionID, long key) {

		Transaction t = ActiveTransactions.get(transactionID);
		String value = t.readLocal(key);
		if(value != null)
			return value;
		else
			return value;
			//read from data-store
			
	}
	
	/**
	 * write - write to local WriteSet of transaction - on commit - this WriteSet would be written to DataStore
	 * @param transactionID
	 * @param key
	 * @param value
	 */
	public void write(long transactionID, long key, String value) {
		Transaction t = ActiveTransactions.get(transactionID);
		t.addToWriteSet(key,value);
	}
	
	/**
	 * commit - Transaction
	 * @param transactionID
	 * @return true - on success , false - on failure
	 */
	public boolean commit(long transactionID) {
		boolean result = false;
		Transaction t = ActiveTransactions.get(transactionID);
		//code for commit protocol
		
		return result;
	}
	/**
	 * findWinningVal
	 * @param responseSet
	 * @param propVal
	 * @return
	 */
	public String findWinningVal(List responseSet, String propVal) {
		String winningVal = null;
		return winningVal;
	}
	/**
	 * enhancedFindWinningVal
	 * @param responseSet
	 * @param propVal
	 * @return
	 */
	public String enhancedFindWinningVal(List responseSet, String propVal) {
		String winningVal = null;
		return winningVal;
	}
	
	public static void main(String[] args) 
	{
		Settings.init();
		TransactionClient client1 = new TransactionClient();
		client1.SendMessagetoServerX(Settings.serverIpMap.get(1),Messages.sendAcceptFromClientToService(1, 5, "a"));
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

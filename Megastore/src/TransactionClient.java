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
	private Database db;
	
	/**
	 * Constructor
	 */
	public TransactionClient() {
		propNum = 0;
		ActiveTransactions = new HashMap<Long,Transaction>();
		try {
			db = new Database();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Receive Messages from AppLayer
	 */
	public void ReceiveBeginMessageFromAppLayer(long transactionID) {
		this.begin(transactionID);
	}

	public String ReceiveReadMessageFromAppLayer(long transactionID, String key) {
		String result = read(transactionID, key);
		return result;
	}
	
	public void ReceiveWriteMessageFromAppLayer(long transactionID, String key, String value) {
		write(transactionID, key, value);
	}
	
	public void ReceiveCommitMessageFromAppLayer(long transactionID) {
		commit(transactionID);
	}
	
	public void ReceiveAbortMessageFromAppLayer(long transactionID) {
		abort(transactionID);
	}
	
	/**
	 * SendMessagetoServerX
	 * @param serverAddress
	 * @param message
	 * @return
	 */
	public String SendMessagetoServerX(InetSocketAddress serverAddress, String message)
	{//Send a string message to a given server
		Socket socket;
		try {
			socket = new Socket(serverAddress.getHostName(),serverAddress.getPort());
			MessageSender newThread = new MessageSender(socket,message);
			newThread.start();
			while (newThread.isAlive())
			{
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return newThread.parsedMessage;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public class MessageSender extends Thread
	{//Thread that opens the socket connection and sends the message
		private Socket socket;
		private PrintWriter toTransactionServer;
		private BufferedReader fromTransactionServer;
		private String message;
		private String parsedMessage;
		
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
				parsedMessage = fromTransactionServer.readLine();
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
	public String read(long transactionID, String key) {

		Transaction t = ActiveTransactions.get(transactionID);
		String value = t.readLocal(key);
		if(value != null)
			return value;
		else
			try {
				value = db.read(key, t.timestamp);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		return value;
	}
	
	/**
	 * write - write to local WriteSet of transaction - on commit - this WriteSet would be written to DataStore
	 * @param transactionID
	 * @param key
	 * @param value
	 */
	public void write(long transactionID, String key, String value) {
		Transaction t = ActiveTransactions.get(transactionID);
		t.addToWriteSet(key,value);
	}
	
	/**
	 * commit - Transaction
	 * @param transactionID
	 * @return true - on success , false - on failure
	 */
	public boolean commit(long transactionID) {
		Transaction t = ActiveTransactions.get(transactionID);
		//code for commit protocol
		HashMap<String,String> propVal = t.WriteSet;
		
		if(runPAXOSGivenPropNum(propVal)) {
			ActiveTransactions.remove(transactionID);
			return true;
		}
		//promotion here
		else 
			return false;
	}
	/**
	 * abprt - Transaction
	 * @param transactionID
	 * @return
	 */
	public boolean abort(long transactionID)
	{
		//to be written
		return true;
	}
	private boolean runPAXOSGivenPropNum(HashMap<String,String> propVal)
	{
		int D = Settings.serverIpList.length;
		//PREPARE PHASE
		List<MessageContent> responseSet = preparePhaseForPAXOS(propVal);
		//ACCEPT PHASE
		HashMap<String,String> propValue = findWinningVal(responseSet, propVal);
		int ackCount = 0;
		for (int i = 0; i < Settings.serverIpList.length; i++) 
		{
			String response = SendMessagetoServerX(Settings.serverIpList[i],Messages.sendAcceptFromClientToService(i+1, propNum, propValue));
			MessageContent parsedResponse = Messages.parse(response);
			if (parsedResponse.status)
				ackCount++;
		}
		
		if (ackCount < D/2)
		{
			try {
				Thread.sleep((long) (Math.random()*100));
				propNum  = nextPropNumber(responseSet,propNum);
				return runPAXOSGivenPropNum(propVal);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for (int i = 0; i < Settings.serverIpList.length; i++) 
		{
			SendMessagetoServerX(Settings.serverIpList[i], Messages.sendApplyFromClientToService(i+1, propNum, propValue));	
		}
		if (propValue.equals(propVal))
			return true;
		{
			//abort();//Maybe try to promote.
			return false;
		}
		
		
	}
	
	private List<MessageContent>  preparePhaseForPAXOS(HashMap<String,String> propVal)
	{
		boolean keepTrying = true;
		int D = Settings.serverIpList.length;
		List<MessageContent> responseSet = null;
		int ackCount;
		while (keepTrying)
		{
			ackCount = 0;
			responseSet = new ArrayList<MessageContent>();
			for (int i = 0; i < Settings.serverIpList.length; i++) 
			{
				String response = SendMessagetoServerX(Settings.serverIpList[i],Messages.sendPrepareFromClientToService(i+1, propNum));
				MessageContent parsedResponse = Messages.parse(response);
				responseSet.add(parsedResponse);
				if (parsedResponse.status)
					ackCount++;
				if (ackCount>(D/2))
					keepTrying = false;
				else
				{
					try {
						Thread.sleep((long) (Math.random()*100));
						propNum  = nextPropNumber(responseSet,propNum);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		return responseSet;
	}
	
	private long nextPropNumber(List<MessageContent> responseSet, long propNum)
	{
		long maxPropNumber = propNum;
		for (int i = 0; i < responseSet.size(); i++) 
		{
			if (responseSet.get(i).propositionNumber > maxPropNumber)
			{
				maxPropNumber = responseSet.get(i).propositionNumber;
			}
		}
		return maxPropNumber;
	}
	/**
	 * findWinningVal
	 * @param responseSet
	 * @param propVal
	 * @return
	 */
	public HashMap<String,String> findWinningVal(List<MessageContent> responseSet, HashMap<String,String> propVal) {
		HashMap<String,String> winningValue = null;
		long maxProp = Long.MIN_VALUE;
		for (int i = 0; i < responseSet.size(); i++)
		{
			MessageContent current = responseSet.get(i);
			if (current.propositionNumber>maxProp && current.propositionValues != null)
			{
				maxProp = current.propositionNumber;
				winningValue = current.propositionValues;
			}
		}
		if (winningValue == null)
			winningValue = propVal;
		return winningValue;
	}
	/**
	 * enhancedFindWinningVal
	 * @param responseSet
	 * @param propVal
	 * @return
	 */
	public String enhancedFindWinningVal(List responseSet, String propVal) {
		//have to write this
		String winningVal = null;
		return winningVal;
	}
	
	public static void main(String[] args) 
	{
		Settings.init();
		TransactionClient client1 = new TransactionClient();
		//client1.SendMessagetoServerX(Settings.serverIpMap.get(1),Messages.sendAcceptFromClientToService(1, 5, "a"));
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

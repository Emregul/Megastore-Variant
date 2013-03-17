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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TransactionClient {
	//maintains the state
	private long propNum;
	private HashMap<Long,Transaction> ActiveTransactions;
	private Database db;
	private long id;
	private ArrayList<MessageContent> responseSet;
	private int ackCount;

	/**
	 * Constructor
	 */
	public TransactionClient(long id) {
		propNum = 0;
		this.id = id;
		ActiveTransactions = new HashMap<Long,Transaction>();
		responseSet = new ArrayList<MessageContent>();
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
	public void SendMessagetoAllServers(String messageType,HashMap<String,String> propValue)
	{
		ExecutorService es = Executors.newCachedThreadPool();
		ackCount = 0;
		responseSet = new ArrayList<MessageContent>();
		for (int i = 0; i < Settings.serverIpList.length; i++) 
		{
			Socket socket;
			InetSocketAddress serverAddress = Settings.serverIpList[i];
			String message = "";
			try {

				socket = new Socket(serverAddress.getHostName(),serverAddress.getPort());
				if (messageType.equals("PREPARE"))
				{
					InternalMessageLog.WriteLog("client " + id, " sent " + Messages.sendPrepareFromClientToService(i+1, propNum));
					message = Messages.sendPrepareFromClientToService(i+1, propNum);
				}else if (messageType.equals("APPLY"))
				{
					InternalMessageLog.WriteLog("client"+id, " sent " + Messages.sendApplyFromClientToService(i+1, propNum, propValue));
					message = Messages.sendApplyFromClientToService(i+1, propNum, propValue);
				}else if (messageType.equals("ACCEPT"))
				{
					InternalMessageLog.WriteLog("client"+id, " sent " + Messages.sendAcceptFromClientToService(i+1, propNum, propValue));
					message = Messages.sendAcceptFromClientToService(i+1, propNum, propValue);
				}else
				{
					System.err.println("client" + id + "NOONE DARES COMETH" + messageType);
				}
				MessageSender newThread = new MessageSender(socket,message);
				es.execute(newThread);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		es.shutdown();
		try {
			es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			//Here be dragons!
		}
	}

	public class MessageSender extends Thread
	{//Thread that opens the socket connection and sends the message
		private Socket socket;
		private PrintWriter toTransactionServer;
		private BufferedReader fromTransactionServer;
		private String message;
		private String fromServer;

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
				fromServer = fromTransactionServer.readLine();
				if (fromServer != null)
				{
					MessageContent parsedResponse = Messages.parse(fromServer);
					synchronized(responseSet)
					{
						InternalMessageLog.WriteLog("client "+id, " received " + parsedResponse.toString());
						if (parsedResponse.status)
							ackCount++;
						responseSet.add(parsedResponse);
					}
				}
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
		SendMessagetoAllServers("ACCEPT", propValue);

		if (ackCount < D/2)
		{
			try {
				Thread.sleep((long) (Math.random()*100));
				propNum  = nextPropNumber(responseSet,propNum+1);
				InternalMessageLog.WriteLog("client"+id, "FAILED AT ACCEPT, RESTARTING WITH PROPNUM " +  propNum);
				return runPAXOSGivenPropNum(propVal);
				//return false;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		SendMessagetoAllServers("APPLY", propValue);
		if (propValue.equals(propVal))
		{
			InternalMessageLog.WriteLog("client"+id, "COMMITTED :) " +propValue);
			return true;
		}else
		{
			InternalMessageLog.WriteLog("client"+id, "ABORTED :(");
			return false;
		}


	}

	private List<MessageContent>  preparePhaseForPAXOS(HashMap<String,String> propVal)
	{
		boolean keepTrying = true;
		int D = Settings.serverIpList.length;
		while (keepTrying)
		{
			responseSet = new ArrayList<MessageContent>();
			SendMessagetoAllServers("PREPARE", propVal);
			if (ackCount>(D/2))
				keepTrying = false;
			else
			{
				try {
					Thread.sleep((long) (Math.random()*100));
					propNum  = nextPropNumber(responseSet,propNum+1);
					InternalMessageLog.WriteLog("client"+id, "FAILED AT PREPARE, RESTARTING WITH PROPNUM " +  propNum);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
			if (responseSet.get(i).vBalloutNumber > maxPropNumber)
			{
				maxPropNumber = responseSet.get(i).vBalloutNumber;
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
			if (current.propositionNumber>maxProp && current.vValues != null)
			{
				maxProp = current.propositionNumber;
				winningValue = current.vValues;
			}
		}
		if (winningValue == null || winningValue.isEmpty())
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
		//TransactionClient client1 = new TransactionClient();
		//client1.SendMessagetoServerX(Settings.serverIpMap.get(1),Messages.sendAcceptFromClientToService(1, 5, "a"));
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

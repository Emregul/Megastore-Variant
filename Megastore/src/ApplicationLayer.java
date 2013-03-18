import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApplicationLayer extends Thread{

	public TransactionClient Client;
	public String filename;
	
	/**
	 * Constructor
	 */
	public ApplicationLayer(String filename,long id) {
		Client = new TransactionClient(id);
		this.filename= filename;
	}
	
	/**
	 * run - to read a file for execution sequence
	 * @param filename
	 */
	public void run() {
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String line;
            // Continue to read lines while there are still some left to read
			while ((line = in.readLine()) != null) {
                // Print file line to screen
				this.message_to_api(line);
			}
			in.close();
		} 
        catch (IOException e) {
			System.err.println("File input error");
		}

	}
	
	/**
	 * message_to_api
	 * @param message
	 */
	public void message_to_api(String message) {
		if(message != null) {
			String[] parts = message.split("\\s+");
			String op; 	  //{BEGIN,COMMIT,READ,WRITE,ABORT}
			long transID; //Transaction ID
			String key;   //key for READ or WRITE operation
			String value; //value to be written by WRITE operation
			
			/*
			 * length = 2 for begin,commit and abort operations
			 * BEGIN 1
			 * COMMIT 1
			 */
			if(parts.length == 2) {
				op = parts[0];
				transID = Long.parseLong(parts[1]);
				//System.out.println("Operation : " + op);
				//System.out.println("TransactionID : " + transID);
				if(op.equals("BEGIN")) {
					Client.ReceiveBeginMessageFromAppLayer(transID);
				}
				else if(op.equals("COMMIT")) {
					Client.ReceiveCommitMessageFromAppLayer(transID);
				}
				else if(op.equals("ABORT")) {
					Client.ReceiveAbortMessageFromAppLayer(transID);
				}
				else {
					System.out.println("Error: Unrecognized message format.");
				}
			}
			/*
			 * length = 3 for read operation
			 * READ 1 X
			 */
			else if(parts.length == 3) {
				op = parts[0];
				transID = Long.parseLong(parts[1]);
				key = parts[2];
				//System.out.println("Operation : " + op);
				//System.out.println("TransactionID : " + transID);
				//System.out.println("Key : " + key);
				System.out.println("Transaction:"+ transID +" " + key + ":" + Client.ReceiveReadMessageFromAppLayer(transID,key));
			}
			/*
			 * length = 4 for write operation
			 * WRITE 1 X 25
			 */
			else if(parts.length == 4) {
				op = parts[0];
				transID = Long.parseLong(parts[1]);
				key = parts[2];
				value = parts[3];
				//System.out.println("Operation : " + op);
				//System.out.println("TransactionID : " + transID);
				//System.out.println("Key : " + key);
				//System.out.println("Value : " + value);
				Client.ReceiveWriteMessageFromAppLayer(transID,key,value);
				
			}
			else {
				System.out.println("Error: Unrecognized input format.");
			}
		}
	}
	public static void main(String[] args) {
		ApplicationLayer app1 = new ApplicationLayer("App1.txt",1);
		ApplicationLayer app2 = new ApplicationLayer("App2.txt",2);
		ExecutorService es = Executors.newCachedThreadPool();
		InternalMessageLog.init();
		InternalMessageLog.WriteLog("======================", "======================");
		es.execute(app1);
		es.execute(app2);
	}
}

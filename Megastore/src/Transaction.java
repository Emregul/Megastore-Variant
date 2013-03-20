<<<<<<< HEAD
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


public class Transaction {
	public HashMap<String,String> ReadSet;
	public HashMap<String,String> WriteSet;
	public long transactionID;
	public long timestamp;
	public long position;
	
	/**
	 * Constructor
	 * @param transID
	 */
	public Transaction(long transID) {
		transactionID = transID;
		ReadSet = new HashMap<String,String>();
		WriteSet = new HashMap<String,String>();
		timestamp = System.currentTimeMillis() / 1000L;
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
	public void setPosition(long position)
	{
		this.position = position;
	}
	
	/**
	 * addToReadSet - adds (key,value) pair to ReadSet of Transaction
	 * @param key
	 * @param value
	 */
	public void addToReadSet(String key, String value) {
		ReadSet.put(key, value);
	}
	
	/**
	 * addToWriteSet - adds (key,value) pair to WriteSet of Transaction
	 * @param key
	 * @param value
	 */
	public void addToWriteSet(String key, String value) {
		WriteSet.put(key, value);
	}
	
	/**
	 * readLocal - before reading from datastore- check for locally written key
	 * @param key
	 * @return null if key is not present else value
	 */
	public String readLocal(String key) {
			return WriteSet.get(key);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
=======
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


public class Transaction {
	public HashMap<String,String> ReadSet;
	public HashMap<String,String> WriteSet;
	public long transactionID;
	public long timestamp;
	public long position;
	
	/**
	 * Constructor
	 * @param transID
	 */
	public Transaction(long transID) {
		transactionID = transID;
		ReadSet = new HashMap<String,String>();
		WriteSet = new HashMap<String,String>();
		timestamp = System.currentTimeMillis() / 1000L;
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
	public void setPosition(long position)
	{
		this.position = position;
	}
	
	/**
	 * addToReadSet - adds (key,value) pair to ReadSet of Transaction
	 * @param key
	 * @param value
	 */
	public void addToReadSet(String key, String value) {
		ReadSet.put(key, value);
	}
	
	/**
	 * addToWriteSet - adds (key,value) pair to WriteSet of Transaction
	 * @param key
	 * @param value
	 */
	public void addToWriteSet(String key, String value) {
		WriteSet.put(key, value);
	}
	
	/**
	 * readLocal - before reading from datastore- check for locally written key
	 * @param key
	 * @return null if key is not present else value
	 */
	public String readLocal(String key) {
			return WriteSet.get(key);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
>>>>>>> Vivek- Without Promotion

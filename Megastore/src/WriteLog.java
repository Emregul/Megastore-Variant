import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * This is tuple that keeps the data in a position of the log
 * 
 * @author arlei
 *
 */
class WriteLogReadResult{
	public long vNextBal;
	public long vBalloutNumber;
	public HashMap<String, String> values;
	
	
	/**
	 * Prints the result
	 * 
	 * @param 
	 */
	void print(){
		System.out.println("vNextBal="+vNextBal+",vBalloutNumber="+vBalloutNumber+"\nvariables=");
		
		for (Map.Entry<String, String> entry : values.entrySet()) {
		    System.out.println("variable = " + entry.getKey() + ", value = " + entry.getValue());
		}
	}
}

/**
 * Implements the Write Log and its operations
 * 
 * @author arlei
 *
 */
public class WriteLog {
	
	/*Tests the operations in the log*/
	public static void main(String[] args) throws IOException {
		/*Creates a log for an entity key=1*/
		WriteLog log = new WriteLog("One");
		
		/*Starts the position 0 of the log*/
		log.startPosition(0);	
		
		/*Reads and prints the position 0 of the log with the initial values for the variables*/
		WriteLogReadResult result = log.read(0);
		result.print();
		
		/*Checks if nexBal for position 0 is -1 (true) and then updates nexBal to 10*/
		log.checkAndWrite(0, -1, 10);
		result = log.read(0);
		result.print();
				
		/*Writes the values "x = 100", "y = 200", and "z = 300" with balloutNumber 
		 * 10 to the position 0 of the log*/
		HashMap<String,String> values = new HashMap<String, String>();
		values.put("x", "100");
		values.put("y", "200");
		values.put("z", "300");
		log.write(0, 10, values);
		result = log.read(0);
		result.print();
		

		/*Checks if nexBal for position 1 is 1 (false) and then does not update Ballout to 10 
		 * and values to "x = 100", "y = 200", and "z = 300"*/
		log.checkAndWrite(1, 1, values);
		result = log.read(1);
		result.print();
		
		/*Checks if nexBal for position 1 is -1 (true) and then updates Ballout to 10 
		 * and values to "x = 100", "y = 200", and "z = 300"*/
		log.checkAndWrite(1, -1, values);
		result = log.read(1);
		result.print();
	}
	
	/**
	 * Constructor, an instance of Hbase must be running
	 * 
	 * The schema of the database is the following:
	 * Each position of the log is implemented as a row
	 * Each row has 3 column families: BalloutNumber, NextBal, and Values
	 * BalloutNumber and NextBal have a single column with a unique version
	 * The Values family has one column for each variable, each one having
	 * a single version as well.
	 * 
	 * @param key key of the log
	 * @throws IOException
	 */
	public WriteLog(String key) throws IOException{
		config = HBaseConfiguration.create();
		config.clear();
		config.set("hbase.zookeeper.quorum", "ec2-54-224-51-107.compute-1.amazonaws.com");
		config.set("hbase.zookeeper.property.clientPort","2181");
		config.set("hbase.master", "ec2-54-224-51-107.compute-1.amazonaws.com:60000");
		
		
		HBaseAdmin hbase = new HBaseAdmin(config);
		
		HTableDescriptor desc = new HTableDescriptor(key);
		
		HColumnDescriptor colFamBalloutNumber = new HColumnDescriptor(columnFamilyBalloutNumber.getBytes());
		colFamBalloutNumber.setMaxVersions(1);
		desc.addFamily(colFamBalloutNumber);
		
		HColumnDescriptor colFamNextBall = new HColumnDescriptor(columnFamilyNextBal.getBytes());
		colFamNextBall.setMaxVersions(1);
		desc.addFamily(colFamNextBall);
		
		HColumnDescriptor colFamValues = new HColumnDescriptor(columnFamilyValues.getBytes());
		colFamValues.setMaxVersions(1);
		desc.addFamily(colFamValues);
		
		try
		{
			hbase.createTable(desc);
		}catch(org.apache.hadoop.hbase.TableExistsException exc){
			/*Do nothing*/
		}
		table = new HTable(config, key);
		tableName = key;
	}
	
	/**
	 * @return Returns the current position in the log.
	 */
	public long getPosition() {
		return position;
	}
	
	/**
	 * Erases the log
	 * 
	 * @throws IOException
	 */
	public void erase() throws IOException{
		HBaseAdmin hbase = new HBaseAdmin(config);
		hbase.disableTable(tableName);
		hbase.deleteTable(tableName);
	}
	
	/**
	 * Reads the position in the log and returns it as a WriteLogReadResult
	 * @param position position to be returned
	 * @return WriteLogReadResult for the position given as parmeter
	 * @throws IOException
	 * 
	 */
	public WriteLogReadResult read(long position) throws IOException{
		
		Get g = new Get(Bytes.toBytes(String.valueOf(position)));
		Result r = table.get(g);
		WriteLogReadResult result = new WriteLogReadResult();
	
		byte[] value = r.getValue(Bytes.toBytes(columnFamilyNextBal), 
				Bytes.toBytes(columnNextBal));
					
		result.vNextBal = Long.valueOf(Bytes.toString(value));
		value = r.getValue(Bytes.toBytes(columnFamilyBalloutNumber), 
				Bytes.toBytes(columnBalloutNumber));
		result.vBalloutNumber = Long.valueOf(Bytes.toString(value));
		
		result.values = new HashMap<String, String>();
		
		NavigableMap<byte[], byte[]> familyMap = 
				r.getFamilyMap(Bytes.toBytes(columnFamilyValues));

		for (Map.Entry<byte[], byte[]> entry : familyMap.entrySet()) {
		     result.values.put(Bytes.toString(entry.getKey()), 
		    		 Bytes.toString(entry.getValue()));
		}
	    
		return result;
	}
	
	/**
	 * Writes balloutNumber and the value to the position in the log
	 * 
	 * @param position position in the log
	 * @param balloutNumber Ballout number
	 * @param values Values of variables
	 * @throws IOException
	 */
	public void write(long position, long balloutNumber, 
			HashMap<String,String> values) throws IOException{
		Put p = new Put(Bytes.toBytes(String.valueOf(position)));

		p.add(Bytes.toBytes(columnFamilyBalloutNumber), 
				Bytes.toBytes(columnBalloutNumber), 
				Bytes.toBytes(String.valueOf(balloutNumber)));
		
		for (Map.Entry<String, String> entry : values.entrySet()) {
			p.add(Bytes.toBytes(columnFamilyValues), 
					Bytes.toBytes(entry.getKey()), Bytes.toBytes(entry.getValue()));
		}
				
		table.put(p);
		this.position++;
		this.startPosition(this.position);
	}
	
	/**
	 * Checks whether vNextBal is equal to the nextBal value in the log, 
	 * if yes, updates it to propositionNumber
	 * 
	 * @param position position in the log
	 * @param vNextBal next ballout number
	 * @param propositionNumber proposition number
	 * @return TRUE if the updated was applied and FALSE otherwise.
	 * @throws IOException
	 */
	public boolean checkAndWrite(long position, long vNextBal, 
			long propositionNumber) throws IOException{
		Put p = new Put(Bytes.toBytes(String.valueOf(position)));
		p.add(Bytes.toBytes(columnFamilyNextBal), 
				Bytes.toBytes(columnNextBal), 
				Bytes.toBytes(String.valueOf(propositionNumber)));
		
		return table.checkAndPut(Bytes.toBytes(String.valueOf(position)), 
				Bytes.toBytes(columnFamilyNextBal), 
				Bytes.toBytes(columnNextBal), 
				Bytes.toBytes(String.valueOf(vNextBal)), 
				p);		
	}
	
	/**
	 * Checks whether propositionNumber corresponds to most recent update to nextBal, 
	 * if yes then writes balloutNumber and the value to the position in the log.
	 * @param position position in the log
	 * @param propositionNumber proposition number
	 * @param values values to be updated
	 * @return TRUE if the updated was applied and FALSE otherwise
	 * @throws IOException
	 */
	public boolean checkAndWrite(long position, long propositionNumber,
			HashMap<String,String> values) throws IOException{
		Put p = new Put(Bytes.toBytes(String.valueOf(position)));
		p.add(Bytes.toBytes(columnFamilyBalloutNumber), 
				Bytes.toBytes(columnBalloutNumber), 
				Bytes.toBytes(String.valueOf(propositionNumber)));
		
		for (Map.Entry<String, String> entry : values.entrySet()) {
			p.add(Bytes.toBytes(columnFamilyValues), 
					Bytes.toBytes(entry.getKey()), 
					Bytes.toBytes(entry.getValue()));
		}
		
		return table.checkAndPut(Bytes.toBytes(String.valueOf(position)), 
				Bytes.toBytes(columnFamilyNextBal), 
				Bytes.toBytes(columnNextBal), 
				Bytes.toBytes(String.valueOf(propositionNumber)), 
				p);
		
		/**
		 TODO: Discuss this change with Vivek
		Get g = new Get(Bytes.toBytes(String.valueOf(position)));
		Result r = table.get(g);
		WriteLogReadResult result = new WriteLogReadResult();
		byte[] values = r.getValue(Bytes.toBytes(columnFamily), Bytes.toBytes("nextBal"));
		result.vNextBal = Long.valueOf(Bytes.toString(values));
		values = r.getValue(Bytes.toBytes(columnFamily), Bytes.toBytes("balloutNumber"));
		result.vBalloutNumber = Long.valueOf(Bytes.toString(values));
		values = r.getValue(Bytes.toBytes(columnFamily), Bytes.toBytes("value"));
		result.value = Bytes.toString(values);
		
		if(result.vNextBal == propositionNumber) {
			p.add(Bytes.toBytes(columnFamily), Bytes.toBytes("balloutNumber"), Bytes.toBytes(String.valueOf(propositionNumber)));
			p.add(Bytes.toBytes(columnFamily), Bytes.toBytes("value"), Bytes.toBytes(value));
			table.put(p);
			return true;
		}
		else
			return false;
		**/
	}
	
	/*Log position*/
	private long position;
		
	/*Sets the initial values for the start position*/
	public void startPosition(long position) throws IOException{
		this.position = position;
		Put p = new Put(Bytes.toBytes(String.valueOf(position)));
		
		p.add(Bytes.toBytes(columnFamilyBalloutNumber), 
				Bytes.toBytes(columnBalloutNumber), 
				Bytes.toBytes(String.valueOf(initialBalloutNumber)));
		
		p.add(Bytes.toBytes(columnFamilyNextBal), 
				Bytes.toBytes(columnNextBal), 
				Bytes.toBytes(String.valueOf(initialNextBal)));
		
		table.put(p);
	}
	
	/*Variables used by Paxos*/
	final long initialNextBal = -1;
	final long initialBalloutNumber = -1;
	final String initialValue = "";
	
	/*HBase*/
	private HTable table;
	private Configuration config;
	final private String columnFamilyBalloutNumber = "balloutNumber";
	final private String columnFamilyNextBal = "nextBal";
	final private String columnFamilyValues = "values";
	final private String columnBalloutNumber = "balloutNumber";
	final private String columnNextBal = "nextBal";
	private String tableName;
}
	

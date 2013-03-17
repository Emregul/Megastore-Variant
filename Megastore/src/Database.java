import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * Implements the database using hbase as key-value store
 * 
 * @author arlei
 *
 */
public class Database {

	/**
	 * Testing the database
	 */
	public static void main(String[] args) throws IOException {
		
		//Creating database
		Database db = new Database();
		
		//Creating the first version of the variables
		HashMap <String,String> values0 = new HashMap<String,String>();
		values0.put("X", "X0");
		values0.put("Y", "Y0");
		values0.put("Z", "Z0");
		
		//Writing the variables with timestamp 1
		db.write(values0, 1);
		
		//Creating the second version of the variables
		HashMap <String,String> values1 = new HashMap<String,String>();
		values1.put("X", "X1");
		values1.put("Y", "Y1");
		values1.put("Z", "Z1");
		
		//Writing the variables with timestamp 2
		db.write(values1, 2);
		
		//Generating a list of keys to be read from the database
		List <String> keys = new ArrayList<String>();
		keys.add("X");
		keys.add("Y");
		keys.add("Z");
		
		//Reading the keys for timestamp 2, the first version should be returned
		HashMap<String,String> recovered0 = db.read(keys, 2);
		
		for (Map.Entry<String, String> entry : recovered0.entrySet()) {
		    System.out.println("variable = " + entry.getKey() + ", value = " + entry.getValue());
		}
		
		//Reading the keys for timestamp 3, the second version should be returned
		HashMap<String,String> recovered1 = db.read(keys, 3);
				
		for (Map.Entry<String, String> entry : recovered1.entrySet()) {
			System.out.println("variable = " + entry.getKey() + ", value = " + entry.getValue());
		}
		
		//Reading a single value
		String value = db.read("Z", 2);
		System.out.println("variable = Z, value = " + value);
		
		//Removing the first version of all the variables from the database
		db.removeOldVersions(keys, 1);
		
		//Reading the keys for timestamp 2, the first version (now null) should be returned
		recovered0 = db.read(keys, 2);
				
		for (Map.Entry<String, String> entry : recovered0.entrySet()) {
		    System.out.println("variable = " + entry.getKey() + ", value = " + entry.getValue());
		}
				
		//Reading the keys for timestamp 3, the second version should be returned
		recovered1 = db.read(keys, 3);
						
		for (Map.Entry<String, String> entry : recovered1.entrySet()) {
			System.out.println("variable = " + entry.getKey() + ", value = " + entry.getValue());
		}		
	}
	
	/**
	 * Constructor, an instance of Hbase must be running
	 * 
	 * The schema of the database is the following:
	 * Each variable has a key in the database
	 * Each row has a single column family called variables whose key is the name/key of the variable
	 * This column family has a single column called value
	 * The number of versions is set to 1000, not sure when to delete versions yet.
	 * 	 
	 * @throws IOException
	 */
	public Database() throws IOException{
		config = HBaseConfiguration.create();
		config.clear();
		config.set("hbase.zookeeper.quorum", "ec2-54-224-51-107.compute-1.amazonaws.com");
		config.set("hbase.zookeeper.property.clientPort","2181");
		config.set("hbase.master", "ec2-54-224-51-107.compute-1.amazonaws.com:60000");
		HBaseAdmin hbase = new HBaseAdmin(config);
		
		HTableDescriptor desc = new HTableDescriptor(tableName);
		
		HColumnDescriptor colFam = new HColumnDescriptor(columnFamily.getBytes());
		colFam.setMaxVersions(maxVersions);
		desc.addFamily(colFam);
		
		try
		{
			hbase.createTable(desc);
		}catch(org.apache.hadoop.hbase.TableExistsException exc){
			/*Do nothing*/
		}
		
		table = new HTable(config, tableName);
	}
	
	/**
	 * Reads the set of keys from the database. The most recent version that
	 * updated before the timestamp.
	 * 
	 * @param keys
	 * @param timestamp
	 * @return the values read as a hashmap
	 * @throws IOException
	 */
	public HashMap<String,String> read(List<String> keys, long timestamp) throws IOException{
		Iterator<String> iterator = keys.iterator();
		Get g;
		HashMap<String,String> values = new HashMap<String, String>();
		byte[] value;
		Result r;
		String key;
		
		while (iterator.hasNext()) {
			key = iterator.next();
			g = new Get(Bytes.toBytes(key));
			g.setTimeRange(0,timestamp);
			r = table.get(g);
			value = r.getValue(Bytes.toBytes(columnFamily), 
					Bytes.toBytes(column));
			values.put(key, Bytes.toString(value));
		}
		
		return values;
	}
	
	/**
	 * Reads a key from the database. The most recent version that
	 * updated before the timestamp.
	 * 
	 * @param keys
	 * @param timestamp
	 * @return the value read
	 * @throws IOException
	 */
	public String read(String key, long timestamp) throws IOException{
		Get g = new Get(Bytes.toBytes(key));
		Result r;
		g.setTimeRange(0,timestamp);
		r = table.get(g);
		String value = Bytes.toString(r.getValue(Bytes.toBytes(columnFamily), 
				Bytes.toBytes(column)));
		
		return value;
	}
	
	/**
	 * Writes the values to the database with the given timestamp.
	 * 
	 * @param values
	 * @param timestamp
	 * @throws IOException
	 */
	public void write(HashMap<String,String> values, long timestamp) throws IOException{
		Put p = null;
		
		for (Map.Entry<String, String> entry : values.entrySet()) {
			p = new Put(Bytes.toBytes(String.valueOf(entry.getKey())));
			p.add(Bytes.toBytes(columnFamily), 
					Bytes.toBytes(column),
					timestamp,
					Bytes.toBytes(entry.getValue()));
			table.put(p);
		}
	}
	
	/**
	 * Remove from the set of keys all the versions that are not unique
	 * and have timestamps that are older or equal to the one passed
	 * 
	 * @param keys
	 * @param timestamp
	 */
	public void removeOldVersions(List<String> keys, long timestamp) throws IOException{
		Iterator<String> iterKeys = keys.iterator();
		String key;
		Get g;
		Result r;
		List<KeyValue> keyValues;
		Delete del;
						
		while (iterKeys.hasNext()) {
			key = iterKeys.next();
			g = new Get(Bytes.toBytes(key));
			g.setMaxVersions(maxVersions);
			r = table.get(g);
			keyValues = r.getColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column));
			
			if(keyValues.size() > 1){
				del = new Delete(r.getRow(), timestamp);	//FIXME: Why is this deprecated?
				table.delete(del);
			}			
		}
			
	}
	
	/*HBase*/
	private HTable table;
	private Configuration config;
	final private String tableName = "Database";
	final private String columnFamily = "variables";
	final private String column = "values";
	final private int maxVersions = 1000;	//FIXME: How many? How remove versions?
}

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
 * Implements the database using hbase as key-value store
 * 
 * @author arlei
 *
 */
public class Database {

	/**
	 * Testing the database
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

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
	
	public void read(List<String> keys){
		
	}
	
	public void write(HashMap<String,String> values, long timestamp) throws IOException{
		Put p = null;
		
		for (Map.Entry<String, String> entry : values.entrySet()) {
			p = new Put(Bytes.toBytes(String.valueOf(entry.getKey())));
			p.add(Bytes.toBytes(columnFamily), 
					Bytes.toBytes(column),
					timestamp,
					Bytes.toBytes(entry.getValue()));
		}
		
		if(values.size() > 0){
			table.put(p);
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

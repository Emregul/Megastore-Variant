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

/*Just a tuple with the values for a position in the log*/
class WriteLogReadResult{
	public long vNextBal;
	public long vBalloutNumber;
	public String value;
	
	void print(){
		System.out.println("vNextBal="+vNextBal+",vBalloutNumber="+vBalloutNumber+",value="+value);
	}
}

/*Implements the Write Log and its operations*/
public class WriteLog {
	public long position;
	
	public long getPosition() {
		return this.position;
	}
	
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
		
		/*Checks if nexBal for position 0 is -1 (false) and then does not update nexBal to 10*/
		log.checkAndWrite(0, -1, 20);
		result = log.read(0);
		result.print();
		
		/*Writes the value "100" with balloutNumber 10 to the position 0 of the log*/
		log.write(0, 10, "100");
		result = log.read(0);
		result.print();
	}
	
	/*Constructor, an instance of Hbase must be running*/
	public WriteLog(String key) throws IOException{
		config = HBaseConfiguration.create();
		HBaseAdmin hbase = new HBaseAdmin(config);
		
		HTableDescriptor desc = new HTableDescriptor(key);
		HColumnDescriptor colFam = new HColumnDescriptor(columnFamily.getBytes());
		desc.addFamily(colFam);
		
		try
		{
			hbase.createTable(desc);
		}catch(org.apache.hadoop.hbase.TableExistsException exc){
			/*Do nothing*/
		}
		
		table = new HTable(config, key);
	}
	
	/*Reads the position in the log and returns it as a WriteLogReadResult*/
	public WriteLogReadResult read(long position) throws IOException{
		Get g = new Get(Bytes.toBytes(String.valueOf(position)));
		Result r = table.get(g);
		WriteLogReadResult result = new WriteLogReadResult();
	
		byte[] value = r.getValue(Bytes.toBytes(columnFamily), Bytes.toBytes("nextBal"));
					
		result.vNextBal = Long.valueOf(Bytes.toString(value));
		value = r.getValue(Bytes.toBytes(columnFamily), Bytes.toBytes("balloutNumber"));
		result.vBalloutNumber = Long.valueOf(Bytes.toString(value));
		value = r.getValue(Bytes.toBytes(columnFamily), Bytes.toBytes("value"));
		result.value = Bytes.toString(value);		
		
		return result;
	}
	
	/*Writes balloutNumber and the value to the position in the log*/
	public void write(long position, long balloutNumber, String value) throws IOException{
		Put p = new Put(Bytes.toBytes(String.valueOf(position)));

		p.add(Bytes.toBytes(columnFamily), Bytes.toBytes("balloutNumber"), Bytes.toBytes(String.valueOf(balloutNumber)));
		p.add(Bytes.toBytes(columnFamily), Bytes.toBytes("value"), Bytes.toBytes(value));
		table.put(p);
		this.position++;
		this.startPosition(this.position);
	}
	
	/*Checks whether vNextBal is equal to the nextBal value in the log, if yes, updates it to propositionNumber*/
	public boolean checkAndWrite(long position, long vNextBal, long propositionNumber) throws IOException{
		Put p = new Put(Bytes.toBytes(String.valueOf(position)));
		p.add(Bytes.toBytes(columnFamily), Bytes.toBytes("nextBal"), Bytes.toBytes(String.valueOf(propositionNumber)));
		
		return table.checkAndPut(Bytes.toBytes(String.valueOf(position)), Bytes.toBytes(columnFamily), Bytes.toBytes("nextBal"), Bytes.toBytes(String.valueOf(vNextBal)), p);
		
	}
	
	/*Checks whether propositionNumber corresponds to most recent update to nextBal, if yes then writes balloutNumber and the value to the position in the log*/
	public boolean checkAndWrite2(long position, long propositionNumber, String value) throws IOException{
		Put p = new Put(Bytes.toBytes(String.valueOf(position)));
		//Check with Arlei if it is ok to go this way
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
	}

	
	/*Sets the initial values for the start position*/
	public void startPosition(long position) throws IOException{
		this.position = position;
		Put p = new Put(Bytes.toBytes(String.valueOf(position)));
		p.add(Bytes.toBytes(columnFamily), Bytes.toBytes("balloutNumber"), Bytes.toBytes(String.valueOf(initialBalloutNumber)));
		p.add(Bytes.toBytes(columnFamily), Bytes.toBytes("nextBal"), Bytes.toBytes(String.valueOf(initialNextBal)));
		p.add(Bytes.toBytes(columnFamily), Bytes.toBytes("value"), Bytes.toBytes(initialValue));
		table.put(p);
	}
	
	/*Variables used by Paxos*/
	final long initialNextBal = -1;
	final long initialBalloutNumber = -1;
	final String initialValue = "";
	
	/*HBase*/
	private HTable table;
	private Configuration config;
	private String columnFamily = "Positions";
}

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;


/**
 * New class for logging
 * 
 * @author arlei
 *
 */
public class Logging {
	public Logging(String id, String logFileName){
		log = new File(logFileName);
		serverId = id;
		 cal = Calendar.getInstance();
	}
	
	public synchronized void write(String message)
	{
		try {
			try {
				writer = new BufferedWriter(new FileWriter(log,true));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	    	String time = sdf.format(cal.getTime());
			writer.write(time+"	"+serverId+"	"+message);
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	Calendar cal;
	private File log;
	private BufferedWriter writer;
	private String serverId;
}

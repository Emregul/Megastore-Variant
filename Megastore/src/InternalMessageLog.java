import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class InternalMessageLog {
	
	public static File log;
	public static BufferedWriter writer;
	public static void init()
	{
		log = new File("log.txt");
		
	}
	public static synchronized void WriteLog(String from, String message)
	{
		try {
			try {
				writer = new BufferedWriter(new FileWriter(log,true));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			writer.write(from + " " + message);
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

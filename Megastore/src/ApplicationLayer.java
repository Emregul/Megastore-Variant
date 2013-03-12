import java.io.*;

public class ApplicationLayer {

	public TransactionClient Client;
	
	/**
	 * Constructor
	 */
	public ApplicationLayer() {
		Client = new TransactionClient();
	}
	
	/**
	 * run - to read a file for execution sequence
	 * @param filename
	 */
	public void run(String filename) {
		try {
			FileInputStream fstream = new FileInputStream(filename);
			DataInputStream in = new DataInputStream(fstream);

            // Continue to read lines while there are still some left to read
			while (in.available() !=0) {
                // Print file line to screen
				System.out.println (in.readLine());
			}
			in.close();
		} 
        catch (Exception e) {
			System.err.println("File input error");
		}

	}
	
	public static void main(String[] args) {
		ApplicationLayer app1 = new ApplicationLayer();
		String inputfile1 = "App1.txt";
		app1.run(inputfile1);
	}
	

	
}

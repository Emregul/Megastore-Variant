import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;


public class Settings 
{//Settings for IPs
	public static HashMap<Integer,InetSocketAddress> serverIpMap = new HashMap<Integer,InetSocketAddress>();
	public static InetSocketAddress[] serverIpList = {new InetSocketAddress("localhost", 11111), new InetSocketAddress("localhost", 11112),new InetSocketAddress("localhost", 11113)};
	
	public static void init()
	{
		for (int i = 0; i < 3; i++) {
			serverIpMap.put(i+1,serverIpList[i]);
		}
	}
}

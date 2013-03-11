import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;


public class Settings 
{//Settings for IPs
	public static HashMap<Integer,InetSocketAddress> serverIpMap = new HashMap<Integer,InetSocketAddress>();
	public static InetSocketAddress[] serverIpList;
	
	public static void init()
	{
		serverIpMap.put(1, new InetSocketAddress("localhost", 11115));
		serverIpMap.put(2, new InetSocketAddress("127.0.0.1", 11112));
		serverIpMap.put(3, new InetSocketAddress("127.0.0.1", 11113));
	}
}

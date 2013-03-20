import java.util.HashMap;
import java.util.Map;


/**
 * Class for manipulating the content of a message in the Paxos implementation
 * 
 * @author arlei
 *
 */
class MessageContent{
	/**
	 * Constructor
	 */
	public MessageContent(){
		propositionNumber = -1;
		propositionValues = new HashMap<String,String>();
		cid = -1;
		vBalloutNumber = -1;
		vValues = new HashMap<String,String>();
		status = false;
		messageType = null;
		sender = "";
		logPosition = -1;
	}
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(" type: ");
		sb.append(messageType);
		sb.append(" propNum: ");
		sb.append(propositionNumber);
		sb.append(" vBalloutNumber: ");
		sb.append(vBalloutNumber);
		sb.append(" propVal: ");
		sb.append(propositionValues);
		sb.append(" vVal: ");
		sb.append(vValues);
		sb.append(" sender: ");
		sb.append(sender);
		sb.append(" logPosition: ");
		sb.append(logPosition);
		sb.append(" cid: ");
		sb.append(cid);
		sb.append("\n");
		return sb.toString();
	}
	/**
	 * Prints the content of a message, some parts may be set to default values
	 */
	public void print(){
		System.out.print(this.toString());
	}
	
	public String entityKey;
	public long propositionNumber;
	public HashMap<String,String> propositionValues;
	public int cid;
	public long vBalloutNumber;
	public HashMap<String,String> vValues;
	public boolean status;
	//{"PREPARE", "ACCEPT", "APPLY", "PREPARE_SUCCESS", "PREPARE_FAILURE", "POSITION"}
	public String messageType;
	public String sender;
	public long logPosition;
}

/**
 * Class for constructing and checking a message in the implementation of the Paxos protocol
 * 
 * @author arlei
 *
 */
public class Messages {
	
	public static String log(String message){
		String logMessage = "";
		
		MessageContent cont = getContent(message);
		
		if(isSendPrepareFromClientToService(message)){
			logMessage = "PREPARE FROM CLIENT "+cont.sender
					+" TO SERVICE "+cont.cid+
					" PROPNUM="+cont.propositionNumber;
		}
		
		if(isSendAcceptFromClientToService(message)){
			logMessage = "ACCEPT FROM CLIENT "+cont.sender
					+" TO	SERVICE "+cont.cid+
					" PROPNUM="+cont.propositionNumber+
					" PROPVAL="+cont.propositionValues;
		}
		
		if(isSendApplyFromClientToService(message)){
			logMessage = "APPLY FROM CLIENT "+cont.sender
					+" TO	SERVICE "+cont.cid+
					" PROPNUM="+cont.propositionNumber+
					" PROPVAL="+cont.propositionValues;
		}
		
		if(isSendGetPositionFromClientToService(message)){
			logMessage = "GET POSITION FROM CLIENT "+cont.sender
					+" TO SERVICE "+cont.cid;
		}

		if(isSendGetPositionFromServiceToClient(message)){
			logMessage = "GET POSITION="+cont.logPosition+
					" FROM SERVICE "+cont.sender;
		}
		
		if(isSendPrepareSuccessFromServiceToClient(message)){
			logMessage = "PREPARE SUCCESS FROM SERVICE "
					+cont.sender+" BALLOUTNUMBER="+cont.vBalloutNumber+
					" VVALUE="+cont.vValues;
		}
		
		if(isSendPrepareFailureFromServiceToClient(message)){
			logMessage = "PREPARE FAILURE FROM SERVICE "
					+cont.sender+" BALLOUTNUMBER="+cont.vBalloutNumber;
		}

		if(Messages.isSendAcceptFromServiceToClient(message)){
			logMessage = "ACCEPT FROM SERVICE "
					+cont.sender+" STATUS="+cont.status;
		}
				
		return logMessage;
	}
	
	/**
	 * Parses a message, which means identifying the type of message and printing it.
	 * 
	 * @param message
	 * @return
	 */
	public static MessageContent parse(String message){
		MessageContent content = null;
		if(Messages.isSendPrepareFromClientToService(message)){
			System.out.println("Send Prepare from Client to Service");
			content = Messages.getContent(message);
			content.print();
		}
		
		if(Messages.isSendAcceptFromClientToService(message)){
			System.out.println("Send Accept from Client to Service");
			content = Messages.getContent(message);
			content.print();
		}
		
		if(Messages.isSendApplyFromClientToService(message)){
			System.out.println("Send Apply from Client to Service");
			content = Messages.getContent(message);
			content.print();
		}
		
		if(Messages.isSendPrepareSuccessFromServiceToClient(message)){
			System.out.println("Send Prepare Success from Service to Client");
			content = Messages.getContent(message);
			content.print();
		}
		
		if(Messages.isSendPrepareFailureFromServiceToClient(message)){
			System.out.println("Send Prepare Failure from Service to Client");
			content = Messages.getContent(message);
			content.print();
		}
		
		if(Messages.isSendAcceptFromServiceToClient(message)){
			System.out.println("Send Accept from Service to Client");
			content = Messages.getContent(message);
			content.print();
		}
		
		if(Messages.isSendGetPositionFromClientToService(message)){
			System.out.println("Get Position from Client to Service");
			content = Messages.getContent(message);
			content.print();
		}
		
		if(Messages.isSendGetPositionFromServiceToClient(message)){
			System.out.println("Get Position from Service to Client");
			content = Messages.getContent(message);
			content.print();
		}
		
		return content;
	}
	
	/**
	 * Tests the message creation and parsing
	 * 
	 * @param args
	 */
	public static void main(String[] args){
		long propositionNumber = 10;
		HashMap<String,String> propositionValues = new HashMap<String,String>();
		int cid = 1000;
		long vBalloutNumber = 10000;
		HashMap<String,String> vValues = new HashMap<String,String>();
		boolean status = true;
		String sender = "me";
		int logPosition = 41;
		
		/*propositionValues.put("X", "0");
		propositionValues.put("Y", "1");
		propositionValues.put("Z", "2");
		*/
		/*vValues.put("X", "3");
		vValues.put("Y", "4");
		vValues.put("Z", "5");*/
		
		String message = Messages.sendPrepareFromClientToService(cid, propositionNumber,
				sender,logPosition);
		System.out.println("message = "+message);
		parse(message);
		
		message = Messages.sendAcceptFromClientToService(cid, propositionNumber, 
				sender, logPosition, propositionValues);
		System.out.println("message = "+message);
		parse(message);
				
		message = Messages.sendApplyFromClientToService(cid, propositionNumber, 
				sender, logPosition, propositionValues);
		System.out.println("message = "+message);
		parse(message);
				
		message = Messages.sendPrepareSuccessFromServiceToClient(cid, vBalloutNumber, 
				sender, logPosition, vValues);
		System.out.println("message = "+message);
		parse(message);
						
		message = Messages.sendPrepareFailureFromServiceToClient(cid, vBalloutNumber,
				sender, logPosition);
		System.out.println("message = "+message);
		parse(message);
				
		message = Messages.sendAcceptFromServiceToClient(cid, status, sender, 
				logPosition);
		System.out.println("message = "+message);
		parse(message);
		
		message = Messages.sendGetPositionFromClientToService(cid, sender);
		System.out.println("message = "+message);
		parse(message);

		message = Messages.sendGetPositionFromServiceToClient(cid, sender, logPosition);
		System.out.println("message = "+message);
		parse(message);
	}
	
	/*client => service*/
	
	
	/**
	 * Creates message send(d,PREPARE,propNum)
	 * 
	 * @param datacenter
	 * @param propositionNumber
	 * @param sender
	 * @param logPosition
	 * @return the message
	 */
	static String sendPrepareFromClientToService(int datacenter, long propositionNumber, 
			String sender, long logPosition){
		
		return "PREPARE,"+String.valueOf(datacenter)+","+String.valueOf(propositionNumber)+","
				+sender+","+String.valueOf(logPosition);
	}
	
	/**
	 * Creates message asking for the current log position
	 * 
	 * @param datacenter
	 * @param sender
	 * @return message
	 */
	static String sendGetPositionFromClientToService(int datacenter, String sender){
		return "POSITION,"+String.valueOf(datacenter)+","+sender;
	}
	
	/**
	 * Creates message asking for the current log position
	 * 
	 * @param datacenter
	 * @param sender
	 * @param logPosition
	 * @return message
	 */
	static String sendGetPositionFromServiceToClient(int datacenter, String sender, 
			long logPosition){
		return "POSITION,"+String.valueOf(datacenter)+","+sender+","+String.valueOf(logPosition);
	}
	
	/**
	 * Creates message send(d,ACCEPT,proposalNumber,proposalValue)
	 * 
	 * @param datacenter
	 * @param propositionNumber
	 * @param sender
	 * @param logPosition
	 * @param propositionValues
	 * @return the message
	 */
	static String sendAcceptFromClientToService(int datacenter, long propositionNumber,
			String sender, long logPosition, HashMap<String,String> propositionValues){
		
		String message = "ACCEPT,"+String.valueOf(datacenter)+","+String.valueOf(propositionNumber)
				+","+sender+","+String.valueOf(logPosition);
		
		for (Map.Entry<String, String> entry : propositionValues.entrySet()) {
		   message = message + "," + entry.getKey() + "=" + entry.getValue();
		}
		
		return message;
	}
	
	/**
	 * Creates message send(d,proposalNumber,propValue)
	 * 
	 * @param datacenter
	 * @param propositionNumber
	 * @param propositionValues
	 * @param sender
	 * @param logPosition
	 * @return the message
	 */
	static String sendApplyFromClientToService(int datacenter, long propositionNumber, 
			String sender, long logPosition, HashMap<String,String> propositionValues){
		String message = "APPLY,"+String.valueOf(datacenter)+","+String.valueOf(propositionNumber)
				+","+sender+","+String.valueOf(logPosition);
		
		for (Map.Entry<String, String> entry : propositionValues.entrySet()) {
			   message = message + "," + entry.getKey() + "=" + entry.getValue();
		}
		
		return message;
	}
	
	/*service => client*/
		
	/**
	 * Creates message send(cid,status,vBalloutNu8mber,vValue)
	 * 
	 * @param cid
	 * @param vBalloutNumber
	 * @param sender
	 * @param logPosition
	 * @param vValues
	 * @return the message
	 */
	static String sendPrepareSuccessFromServiceToClient(int cid, long vBalloutNumber, 
			String sender, long logPosition, HashMap<String,String> vValues){
		
		String message =  "PREPARE,SUCCESS,"+String.valueOf(cid)+","+String.valueOf(vBalloutNumber)
				+","+sender+","+String.valueOf(logPosition);
		
		for (Map.Entry<String, String> entry : vValues.entrySet()) {
			   message = message + "," + entry.getKey() + "=" + entry.getValue();
		}
		
		return message;
	}
	
	/**
	 * Creates message send(cid,FAILURE,vBalloutNumber)
	 * 
	 * @param cid
	 * @param vBalloutNumber
	 * @param sender
	 * @param logPosition
	 * @return the message
	 */
	static String sendPrepareFailureFromServiceToClient(int cid, long vBalloutNumber,
			String sender, long logPosition){
		
		return "PREPARE,FAILURE,"+String.valueOf(cid)+","+String.valueOf(vBalloutNumber)
				+","+sender+","+String.valueOf(logPosition);
	}
	
	
	/**
	 * Creates message send(cid,status)
	 * 
	 * @param cid
	 * @param status
	 * @return the message
	 */
	static String sendAcceptFromServiceToClient(int cid, boolean status, String sender,
			long logPosition){
		
		return "ACCEPT,"+String.valueOf(cid)+","+String.valueOf(status)+","+sender+
				","+String.valueOf(logPosition);
	}
	
	
	
	/**
	 * Checks if the message is send(d,PREPARE,propNum)
	 * 
	 * 		return "PREPARE,"+String.valueOf(datacenter)+","+String.valueOf(propositionNumber)+","
	 *			+sender+","+String.valueOf(logPosition);
	 * 
	 * @param message
	 * @return TRUE or FALSE
	 */
	static boolean isSendPrepareFromClientToService(String message){
		String[] fields = message.split(",");
		
		if(fields.length != 5){
			return false;
		}
		
		if(fields[0].equals("PREPARE")){
			return true;
		}
		else{
			return false;
		}
	}
	
	/**
	 * Checks if a message is an ask for the log position from client to service
	 * 
	 * 		return "POSITION,"+String.valueOf(datacenter)+","+sender;
	 * 
	 * @param message
	 * @return
	 */
	static boolean isSendGetPositionFromClientToService(String message){
		String[] fields = message.split(",");
		
		if(fields.length != 3){
			return false;
		}
		
		if(fields[0].equals("POSITION")){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Checks if a message is a log position communication from service to client
	 * 
	 * return "POSITION,"+String.valueOf(datacenter)+","+sender+","+String.valueOf(logPosition);
	 * 
	 * @param message
	 * @return
	 */
	static boolean isSendGetPositionFromServiceToClient(String message){
String[] fields = message.split(",");
		
		if(fields.length != 4){
			return false;
		}
		
		if(fields[0].equals("POSITION")){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Checks if the message is send(d,ACCEPT,proposalNumber,proposalValue)
	 *
	 * 			String message = "ACCEPT,"+String.valueOf(datacenter)+","+String.valueOf(propositionNumber)
	 *			+","+sender+","+String.valueOf(logPosition);
	 *
	 * 
	 * @param message
	 * @return TRUE or FALSE
	 */
	static boolean isSendAcceptFromClientToService(String message){
		String[] fields = message.split(",");
		
		if(fields.length < 5){
			return false;
		}
		
		if(fields[0].equals("ACCEPT") && ! fields[2].equals("true") && ! fields[2].equals("false")){
			return true;
		}
		else{
			return false;
		}
	}
	
	/**
	 * Checks if the message is send(d,proposalNumber,propValue)
	 *
	 * 		String message = "APPLY,"+String.valueOf(datacenter)+","+String.valueOf(propositionNumber)
	 *			+","+sender+","+String.valueOf(logPosition);
	 * 
	 * @param message
	 * @return TRUE or FALSE
	 */
	static boolean isSendApplyFromClientToService(String message){
		String[] fields = message.split(",");
		
		if(fields.length < 5){
			return false;
		}
		
		if(fields[0].equals("APPLY")){
			return true;
		}
		else{
			return false;
		}
	}
	
	/**
	 * Checks if the message is send(cid,status,vBalloutNu8mber,vValue)
	 * 
	 *	String message =  "PREPARE,SUCCESS,"+String.valueOf(cid)+","+String.valueOf(vBalloutNumber)
	 *			+","+sender+","+String.valueOf(logPosition);
	 * 
	 * @param message
	 * @return TRUE or FALSE
	 */
	static boolean isSendPrepareSuccessFromServiceToClient(String message){
		String[] fields = message.split(",");
		
		if(fields.length < 6){
			return false;
		}
		
		if(fields[0].equals("PREPARE") && fields[1].equals("SUCCESS")){
			return true;
		}
		else{
			return false;
		}
	}
	
	/**
	 * Checks if the message is send(cid,FAILURE,vBalloutNumber)
	 * 
	 * 		return "PREPARE,FAILURE,"+String.valueOf(cid)+","+String.valueOf(vBalloutNumber)
	 *			+","+sender+","+String.valueOf(logPosition);
	 * 
	 * @param message
	 * @return TRUE or FALSE
	 */
	static boolean isSendPrepareFailureFromServiceToClient(String message){
		String[] fields = message.split(",");
		
		if(fields.length != 6){
			return false;
		}
		
		if(fields[0].equals("PREPARE") && fields[1].equals("FAILURE")){
			return true;
		}
		else{
			return false;
		}
	}
	
	/**
	 * Checks if the message is send(cid,status)
	 * 
	 * 		return "ACCEPT,"+String.valueOf(cid)+","+String.valueOf(status)+","+sender+
	 *			","+String.valueOf(logPosition);
	 * 
	 * @param message
	 * @return TRUE or FALSE
	 */
	static boolean isSendAcceptFromServiceToClient(String message){
		String[] fields = message.split(",");
		
		if(fields.length != 5){
			return false;
		}
		
		if(fields[0].equals("ACCEPT") && (fields[2].equals("true") || fields[2].equals("false"))){
			return true;
		}
		else{
			return false;
		}
	}
	
	/**
	 * Gets the content from a message
	 * 
	 * @param message
	 * @return the content
	 */
	static public MessageContent getContent(String message){
		MessageContent content = new MessageContent();
		String[] fields = message.split(",");
		String[] varValue;

		//return "PREPARE,"+String.valueOf(datacenter)+","+String.valueOf(propositionNumber)+","
		//	+sender+","+String.valueOf(logPosition);
		if(Messages.isSendPrepareFromClientToService(message)){
			content.cid = Integer.parseInt(fields[1]);
			content.propositionNumber = Long.parseLong(fields[2]);
			content.messageType = "PREPARE";
			content.sender = fields[3];
			content.logPosition = Long.parseLong(fields[4]); 
		}
		
		//return "POSITION,"+String.valueOf(datacenter)+","+sender;
		if(Messages.isSendGetPositionFromClientToService(message)){
			content.cid = Integer.parseInt(fields[1]);
			content.sender = fields[2];
			content.messageType = "POSITION";
		}
		
		//return "POSITION,"+String.valueOf(datacenter)+","+sender+","+String.valueOf(logPosition);
		if(Messages.isSendGetPositionFromServiceToClient(message)){
			content.cid = Integer.parseInt(fields[1]);
			content.sender = fields[2];
			content.logPosition = Long.parseLong(fields[3]);
			content.messageType = "POSITION";

		}
		
		//String message = "ACCEPT,"+String.valueOf(datacenter)+","+String.valueOf(propositionNumber)
		//		+","+sender+","+String.valueOf(logPosition);
		if(Messages.isSendAcceptFromClientToService(message)){
			content.cid = Integer.parseInt(fields[1]);
			content.propositionNumber = Long.parseLong(fields[2]);
			content.messageType ="ACCEPT";
			content.sender = fields[3];
			content.logPosition = Long.parseLong(fields[4]);
//			System.out.println("dude" + content.propositionNumber);
			for(int f = 5; f < fields.length; f++){
				varValue = fields[f].split("=");
				content.propositionValues.put(varValue[0],varValue[1]);
			}
			
		}
		
		//String message = "APPLY,"+String.valueOf(datacenter)+","+String.valueOf(propositionNumber)
		//		+","+sender+","+String.valueOf(logPosition);
		if(Messages.isSendApplyFromClientToService(message)){
			content.cid = Integer.parseInt(fields[1]);
			content.propositionNumber = Long.parseLong(fields[2]);
			content.messageType = "APPLY";
			content.sender = fields[3];
			content.logPosition = Long.parseLong(fields[4]);
			
			for(int f = 5; f < fields.length; f++){
				varValue = fields[f].split("=");
				content.propositionValues.put(varValue[0],varValue[1]);
			}
		}
		
		//String message =  "PREPARE,SUCCESS,"+String.valueOf(cid)+","+String.valueOf(vBalloutNumber)
		//		+","+sender+","+String.valueOf(logPosition);
		if(Messages.isSendPrepareSuccessFromServiceToClient(message)){
			content.cid = Integer.parseInt(fields[2]);
			content.vBalloutNumber = Long.parseLong(fields[3]);
			content.messageType = "PREPARE_SUCCESS";
			content.status=true;
			content.sender = fields[4];
			content.logPosition = Long.parseLong(fields[5]);
			
			for(int f = 6; f < fields.length; f++){
				varValue = fields[f].split("=");
				content.vValues.put(varValue[0],varValue[1]);
			}
		}
		
		//return "PREPARE,FAILURE,"+String.valueOf(cid)+","+String.valueOf(vBalloutNumber)
		//		+","+sender+","+String.valueOf(logPosition);
		if(Messages.isSendPrepareFailureFromServiceToClient(message)){
			content.cid = Integer.parseInt(fields[2]);
			content.vBalloutNumber = Long.parseLong(fields[3]);
			content.messageType = "PREPARE_FAILURE";
			content.status = false;
			content.sender = fields[4];
			content.logPosition = Long.parseLong(fields[5]);
		}
		
		//return "ACCEPT,"+String.valueOf(cid)+","+String.valueOf(status)+","+sender+
		//		","+String.valueOf(logPosition);
		if(Messages.isSendAcceptFromServiceToClient(message)){
			content.cid = Integer.parseInt(fields[1]);
			content.status = Boolean.parseBoolean(fields[2]);
			content.messageType = "ACCEPT";
			content.sender = fields[3];
			content.logPosition = Long.parseLong(fields[4]);
		}
		
		return content;
	}
}

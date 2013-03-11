class MessageContent{
	public MessageContent(){
		datacenter = -1;
		propositionNumber = -1;
		propositionValue = "";
		cid = -1;
		vBalloutNumber = -1;
		vValue = "";
		status = false;
	}
	
	public void print(){
		System.out.println("datacenter = "+datacenter);
		System.out.println("propositionNumber = "+propositionNumber);
		System.out.println("propositionValue = "+propositionValue);
		System.out.println("cid = "+cid);
		System.out.println("vBalloutNumber = "+vBalloutNumber);
		System.out.println("vValue = "+vValue);
		System.out.println("status = "+status);
	}
	
	public String entityKey;
	public int datacenter;
	public long propositionNumber;
	public String propositionValue;
	int cid;
	long vBalloutNumber;
	String vValue;
	boolean status;
}

public class Messages {
	/*Message parsing*/
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
		return content;
	}
	
	public static void main(String[] args){
		int datacenter  = 0;
		long propositionNumber = 10;
		String propositionValue = "100";
		int cid = 1000;
		long vBalloutNumber = 10000;
		String vValue = "100";
		boolean status = true;
		
		String message = Messages.sendPrepareFromClientToService(datacenter, propositionNumber);
		System.out.println("message = "+message);
		parse(message);
		
		message = Messages.sendAcceptFromClientToService(datacenter, propositionNumber, propositionValue);
		System.out.println("message = "+message);
		parse(message);
				
		message = Messages.sendApplyFromClientToService(datacenter, propositionNumber, propositionValue);
		System.out.println("message = "+message);
		parse(message);
				
		message = Messages.sendPrepareSuccessFromServiceToClient(cid, vBalloutNumber, vValue);
		System.out.println("message = "+message);
		parse(message);
						
		message = Messages.sendPrepareFailureFromServiceToClient(cid, vBalloutNumber, vValue);
		System.out.println("message = "+message);
		parse(message);
				
		message = Messages.sendAcceptFromServiceToClient(cid, status);
		System.out.println("message = "+message);
		parse(message);
	}
	/*client => service*/
	static String sendPrepareFromClientToService(int datacenter, long propositionNumber){
		return "PREPARE,"+String.valueOf(datacenter)+","+String.valueOf(propositionNumber);
	}
	
	static String sendAcceptFromClientToService(int datacenter, long propositionNumber, String propositionValue){
		return "ACCEPT,"+String.valueOf(datacenter)+","+String.valueOf(propositionNumber)+","+propositionValue;
	}
	
	static String sendApplyFromClientToService(int datacenter, long propositionNumber, String propositionValue){
		return "APPLY,"+String.valueOf(datacenter)+","+String.valueOf(propositionNumber)+","+propositionValue;
	}
	
	/*service => client*/
	static String sendPrepareSuccessFromServiceToClient(int cid, long vBalloutNumber, String vValue){
		return "PREPARE,SUCCESS,"+String.valueOf(cid)+","+String.valueOf(vBalloutNumber)+","+vValue;
	}
	
	static String sendPrepareFailureFromServiceToClient(int cid, long vBalloutNumber, String vValue){
		return "PREPARE,FAILURE,"+String.valueOf(cid)+","+String.valueOf(vBalloutNumber);
	}
	
	static String sendAcceptFromServiceToClient(int cid, boolean status){
		return "ACCEPT,"+String.valueOf(cid)+","+String.valueOf(status);
	}
	
	static boolean isSendPrepareFromClientToService(String message){
		String[] fields = message.split(",");
		
		if(fields.length != 3){
			return false;
		}
		
		if(fields[0].equals("PREPARE")){
			return true;
		}
		else{
			return false;
		}
	}
	
	static boolean isSendAcceptFromClientToService(String message){
		String[] fields = message.split(",");
		
		if(fields.length != 4){
			return false;
		}
		
		if(fields[0].equals("ACCEPT")){
			return true;
		}
		else{
			return false;
		}
	}
	
	static boolean isSendApplyFromClientToService(String message){
		String[] fields = message.split(",");
		
		if(fields.length != 4){
			return false;
		}
		
		if(fields[0].equals("APPLY")){
			return true;
		}
		else{
			return false;
		}
	}
	
	static boolean isSendPrepareSuccessFromServiceToClient(String message){
		String[] fields = message.split(",");
		
		if(fields.length != 5){
			return false;
		}
		
		if(fields[0].equals("PREPARE") && fields[1].equals("SUCCESS")){
			return true;
		}
		else{
			return false;
		}
	}
	
	static boolean isSendPrepareFailureFromServiceToClient(String message){
		String[] fields = message.split(",");
		
		if(fields.length != 4){
			return false;
		}
		
		if(fields[0].equals("PREPARE") && fields[1].equals("FAILURE")){
			return true;
		}
		else{
			return false;
		}
	}
	
	static boolean isSendAcceptFromServiceToClient(String message){
		String[] fields = message.split(",");
		
		if(fields.length != 3){
			return false;
		}
		
		if(fields[0].equals("ACCEPT")){
			return true;
		}
		else{
			return false;
		}
	}
	
	static public MessageContent getContent(String message){
		MessageContent content = new MessageContent();
		String[] fields = message.split(",");
		
		if(Messages.isSendPrepareFromClientToService(message)){
			//"PREPARE,"+String.valueOf(datacenter)+","+String.valueOf(propositionNumber)+","+entityKey;
			content.datacenter = Integer.parseInt(fields[1]);
			content.propositionNumber = Long.parseLong(fields[2]);
		}
		
		if(Messages.isSendAcceptFromClientToService(message)){
			//"ACCEPT,"+String.valueOf(datacenter)+","+String.valueOf(propositionNumber)+","+propositionValue+","+entityKey;
			content.datacenter = Integer.parseInt(fields[1]);
			content.propositionNumber = Long.parseLong(fields[2]);
			content.propositionValue = fields[3];
		}
		
		if(Messages.isSendApplyFromClientToService(message)){
			//"APPLY,"+String.valueOf(datacenter)+","+String.valueOf(propositionNumber)+","+propositionValue+","+entityKey;
			content.datacenter = Integer.parseInt(fields[1]);
			content.propositionNumber = Long.parseLong(fields[2]);
			content.propositionValue = fields[3];
		}
		
		if(Messages.isSendPrepareSuccessFromServiceToClient(message)){
			//"PREPARE,SUCCESS,"+String.valueOf(cid)+","+String.valueOf(vBalloutNumber)+","+vValue+","+entityKey;
			content.cid = Integer.parseInt(fields[2]);
			content.propositionNumber = Long.parseLong(fields[3]);
			content.propositionValue = fields[4];
		}
		
		if(Messages.isSendPrepareFailureFromServiceToClient(message)){
			//"PREPARE,FAILURE,"+String.valueOf(cid)+","+String.valueOf(vBalloutNumber)+","+entityKey;
			content.cid = Integer.parseInt(fields[2]);
			content.vBalloutNumber = Long.parseLong(fields[3]);
		}
		
		if(Messages.isSendAcceptFromServiceToClient(message)){
			//"ACCEPT,"+String.valueOf(cid)+","+String.valueOf(status)+","+entityKey;
			content.cid = Integer.parseInt(fields[1]);
			content.status = Boolean.parseBoolean(fields[2]);
		}
		
		return content;
	}
}

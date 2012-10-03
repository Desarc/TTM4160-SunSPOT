package no.ntnu.item.ttm4160.sunspot.communication;


public class Message {

	//Communication layer
	public static String BROADCAST_SYN="broadcast_syn";
	public static String DATAGRAM_SYN="datagram_syn";
	public static String STREAM_ACK="stream_ack";
	public static String STREAM_SYN_ACK="stream_syn_ack";
	
	public static String DATAGRAM_PORT="100";
	public static String STREAM_PORT="101";
	

	//Content
	public static String button1Pressed="Button 1 has been pressed";
	public static String button2Pressed="Button 2 has been pressed";
	
	public static String CanYouDisplayMyReadings="can you display my readings?";
	public static String ICanDisplayReadings="i can display readings";
	public static String Approved="approved";
	public static String Denied="denied";
	public static String Reading="reading:";	//add value after ":". Example content: "reading:150" 
	public static String SenderDisconnect="sender disconnect";
	public static String ReceiverDisconnect="receiver disconnect";
	
	
	//Addressing
	public static String BROADCAST_ADDRESS="broadcast";
	
	
	/**
	 * Format: <Mac address>:<statemachine ID>
	 */
	private final String sender;
	private final String receiver;
	private final String content;
	
	public Message(String sender, String receiver, String content) {
		this.sender=sender;
		this.receiver=receiver;
		this.content=content;
	}

	public String getSender() {
		return sender;
	}
	
	public String getSenderMAC(){
		int index=sender.indexOf(":");
		if(index==-1){
			//":" not found
			return sender;
		}
		else{
			return sender.substring(0, index);
		}
	}

	public String getReceiver() {
		return receiver;
	}
	
	public String getReceiverMAC(){
		int index=receiver.indexOf(":");
		if(index==-1){
			//":" not found
			return receiver;
		}
		else{
			return receiver.substring(0, index);
		}
	}

	public String getContent() {
		return content;
	}
	
	public String toString() {
		return "Sender: "+sender+", Receiver: "+receiver+", Content: "+content;
	}
	
	
	
	/*
	 * Very simple serialization implementation. Do not touch.
	 */
	public String serializeMessage(){
		return getSender()+"&&"+getReceiver()+"&&"+getContent();
	}
	
	public static Message deSerializeMessage(String rawString){
		int firstDelimiter=rawString.indexOf("&&");
		int secondDelimiter=rawString.indexOf("&&", firstDelimiter+2);
		String sender=rawString.substring(0, firstDelimiter);
		String receiver=rawString.substring(firstDelimiter+2, secondDelimiter);
		String content=rawString.substring(secondDelimiter+2);
		return new Message(sender, receiver, content);
	}
}

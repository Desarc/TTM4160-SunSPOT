package no.ntnu.item.ttm4160.sunspot.utils;

/**
 * Event class, with a type, a timestamp, a reference to the state machine it belongs to, and optionally data.
 *
 */
public class Event {

	private String type;
	private String stateMachineId;
	private String data;
	private long timestamp;
	
	/*
	 * Event types:
	 */
	public static final String noEvent = "no event";
	public static final String sendReadings = "send readings";
	public static final String broadcast = "broadcast";
	public static final String broadcast_response = "broadcast response";
	public static final String receiveReadings = "readings received";
	public static final String connectionApproved = "connection approved";
	public static final String connectionDenied = "connection denied";
	public static final String disconnect = "disconnect";
	public static final String receiverDisconnect = "receiver disconnected";
	public static final String senderDisconnect = "sender disconnected";
	public static final String giveUp = "give up";
	
	
	public static final String testOn = "On";
	public static final String testOff = "Off";
	
	public Event(String type, String stateMachineId, long timestamp) {
		this.type = type;
		this.stateMachineId = stateMachineId;
		this.timestamp = timestamp;
	}
	
	public Event(String type, String stateMachineId, String data, long timestamp) {
		this(type, stateMachineId, timestamp);
		this.data = data;
	}
	
	/**
	 * If the type is zero, there is no event -> discard.
	 * 
	 * @return
	 */
	public String getType() {
		return type;	
	}
	
	public String getStateMachineId() {
		return stateMachineId;
	}
	
	public String getData() {
		return data;
	}
	
	public void setTimeStamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public long getTimeStamp() {
		return timestamp;
	}
}
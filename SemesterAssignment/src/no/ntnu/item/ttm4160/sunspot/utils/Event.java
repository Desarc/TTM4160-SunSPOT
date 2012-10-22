package no.ntnu.item.ttm4160.sunspot.utils;

/**
 * Event class, with a type, a timestamp, a reference to the state machine it belongs to, and optionally data.
 *
 */
public class Event {

	private int type;
	private String stateMachineId;
	private String data;
	private long timestamp;
	
	/*
	 * Event types:
	 */
	public static final int noEvent = 0;
	public static final int sendReadings = 1;
	public static final int broadcast = 2;
	public static final int broadcast_response = 3;
	public static final int receiveReadings = 4;
	public static final int connectionApproved = 5;
	public static final int connectionDenied = 6;
	public static final int disconnect = 7;
	public static final int receiverDisconnect = 7;
	public static final int senderDisconnect = 8;
	public static final int giveUp = 9;
	
	
	public static final int testOn = 12;
	public static final int testOff = 11;
	
	public Event(int type, String stateMachineId, long timestamp) {
		this.type = type;
		this.stateMachineId = stateMachineId;
		this.timestamp = timestamp;
	}
	
	public Event(int type, String stateMachineId, String data, long timestamp) {
		this(type, stateMachineId, timestamp);
		this.data = data;
	}
	
	/**
	 * If the type is zero, there is no event -> discard.
	 * 
	 * @return
	 */
	public int getType() {
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
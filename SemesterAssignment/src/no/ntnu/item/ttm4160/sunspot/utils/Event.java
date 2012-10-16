package no.ntnu.item.ttm4160.sunspot.utils;


public class Event {

	private int type;
	private String stateMachineId;
	private String data;
	private long timestamp;
	
	public static final int noEvent = 0;
	public static final int timeout = 1;
	public static final int broadcast = 2;
	public static final int sendReadings = 3;
	public static final int receiveReadings = 4;
	
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
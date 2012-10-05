package no.ntnu.item.ttm4160.sunspot.utils;


public class Event {

	private int type;
	private String stateMachineId;
	private String data;
	
	public static final int noEvent = 0;
	public static final int timeout = 1;
	public static final int broadcast = 2;
	public static final int transmitReadings = 3;
	public static final int receiveReadings = 4;
	
	public Event(int type, String stateMachineId) {
		this.type = type;
		this.stateMachineId = stateMachineId;
	}
	
	public Event(int type, String stateMachineId, String data) {
		this(type, stateMachineId);
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
}
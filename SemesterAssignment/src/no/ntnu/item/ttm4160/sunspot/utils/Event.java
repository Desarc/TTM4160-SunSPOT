package no.ntnu.item.ttm4160.sunspot.utils;


public class Event {

	private String type;
	
	public static final String noEvent = null;
	public static final String timeout = "Timeout";
	
	public Event(int code) {
		if (code == 0) {
			type = noEvent;
		}
	}
	
	/**
	 * If the type is null, there is no event.
	 * 
	 * @return
	 */
	public String getType() {
		return type;
		
	}
}
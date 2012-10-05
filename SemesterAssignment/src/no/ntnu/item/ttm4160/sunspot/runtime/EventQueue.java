package no.ntnu.item.ttm4160.sunspot.runtime;

import com.sun.spot.util.Queue;

import no.ntnu.item.ttm4160.sunspot.communication.Message;
import no.ntnu.item.ttm4160.sunspot.utils.*;

public class EventQueue {

	private Queue EventQueue;
	private Event event;
	
	public EventQueue() {
		EventQueue = new Queue();
	}
	
	public void saveMessage(Message msg) {
		EventQueue.put(msg);
	}
	
	public void getMessage() {
		
	}
	
}

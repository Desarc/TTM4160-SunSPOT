package no.ntnu.item.ttm4160.sunspot.runtime;

import com.sun.spot.util.Queue;

import no.ntnu.item.ttm4160.sunspot.utils.Event;

public class EventQueue {

	private Queue eventQueue;
	private Queue saveQueue;
	private String stateMachineId;
	
	public EventQueue(String stateMachineId) {
		eventQueue = new Queue();
		this.stateMachineId = stateMachineId;
	}
	
	public void addEvent(Event event) {
		eventQueue.put(event);
	}
	
	public void saveEvent(Event event) {
		saveQueue.put(event);
	}
	
	/**
	 * Saved events are always prioritized over other events.
	 * 
	 * @return
	 */
	public Event getNextEvent() {
		if (saveQueue.size() != 0) {
			return (Event)saveQueue.get();
		}
		else if (eventQueue.size() != 0) {
			return (Event)eventQueue.get();
		}
		else {
			return new Event(Event.noEvent, stateMachineId);
		}
	}
	
	public boolean isEmpty() {
		return (eventQueue.size() == 0 && saveQueue.size() == 0);
	}
	
	public String getStateMachineId() {
		return stateMachineId;
	}
	
}

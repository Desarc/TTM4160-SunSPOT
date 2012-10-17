package no.ntnu.item.ttm4160.sunspot.runtime;

import com.sun.spot.util.Queue;

import no.ntnu.item.ttm4160.sunspot.utils.Event;

public class EventQueue {

	private Queue eventQueue;
	private Queue saveQueue;
	private String stateMachineId;
	private Event nextEvent;
	private Event nextSaveEvent;
	
	public EventQueue(String stateMachineId) {
		eventQueue = new Queue();
		this.stateMachineId = stateMachineId;
	}
	
	public void addEvent(Event event) {
		if (nextEvent == null) {
			nextEvent = event;
		}
		else {
			eventQueue.put(event);
		}
	}
	
	public void saveEvent(Event event) {
		if (nextSaveEvent == null) {
			nextSaveEvent = event;
		}
		else {
			saveQueue.put(event);
		}
	}
	
	/**
	 * Saved events are always prioritized over other events.
	 * 
	 * @return
	 */
	public Event getNextEvent() {
		Event next = null;
		if (nextSaveEvent != null) {
			next = nextSaveEvent;
			if (saveQueue.size() != 0) {
				nextSaveEvent = (Event)saveQueue.get();
			}
			else {
				nextSaveEvent = null;
			}
		}
		else if (nextEvent != null) {
			next = nextEvent;
			if (eventQueue.size() != 0) {
				nextEvent = (Event)eventQueue.get();
			}
			else {
				nextEvent = null;
			}
		}
		return next;
	}
	
	public boolean isEmpty() {
		return (nextEvent == null && nextSaveEvent == null && eventQueue.size() == 0 && saveQueue.size() == 0);
	}
	
	public String getStateMachineId() {
		return stateMachineId;
	}
	
	public long checkTimeStamps() {
		if (nextSaveEvent != null) {
			return nextSaveEvent.getTimeStamp();
		}
		else if (nextEvent != null) {
			return nextEvent.getTimeStamp();
		}
		return Long.MAX_VALUE;
	}
	
}

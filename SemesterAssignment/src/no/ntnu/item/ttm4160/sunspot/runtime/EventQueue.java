package no.ntnu.item.ttm4160.sunspot.runtime;

import com.sun.spot.util.Queue;

import no.ntnu.item.ttm4160.sunspot.utils.Event;

/**
 * Class for keeping track of events for a specific {@link StateMachine}.
 *
 */
public class EventQueue {

	private Queue eventQueue;
	private Queue saveQueue;
	private String stateMachineId;
	private int priority;
	
	/*
	 * The next event is stored outside the Queue, for easier timestamp checking.
	 */
	private Event nextEvent;
	private Event nextSaveEvent;
	
	/**
	 * 
	 * @param stateMachineId {@link String} The ID of the {@link StateMachine} this queue belongs to.
	 * @param priority The scheduling priority for the events in this queue. {@link int}
	 */
	public EventQueue(String stateMachineId, int priority) {
		eventQueue = new Queue();
		this.stateMachineId = stateMachineId;
		this.priority = priority;
	}
	
	/**
	 * Adds a new event to the queue.
	 * @param event
	 */
	public void addEvent(Event event) {
		if (nextEvent == null) {
			nextEvent = event;
		}
		else {
			eventQueue.put(event);
		}
	}
	
	/**
	 * Saves an event in this queue. Saved events are prioritized over other events.
	 * @param event
	 */
	public void saveEvent(Event event) {
		if (nextSaveEvent == null) {
			nextSaveEvent = event;
		}
		else {
			saveQueue.put(event);
		}
	}
	
	/**
	 * Checks if any events are waiting, and returns the next event to be processed by the {@link StateMachine}.
	 * Saved events are always prioritized over other events.
	 * 
	 * @return The next {@link Event}, or {@link null} if there are no events.
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
	
	/**
	 * Checks if there are any events waiting for the corresponding {@link StateMachine}.
	 * @return False if no events.
	 */
	public boolean isEmpty() {
		return (nextEvent == null && nextSaveEvent == null && eventQueue.size() == 0 && saveQueue.size() == 0);
	}
	
	public String getStateMachineId() {
		return stateMachineId;
	}
	
	/**
	 * Returns the timestamp of the next event to be processed by the corresponding {@link StateMachine},
	 * or Long.MAX_VALUE if there are no events.
	 * @return {@link long}
	 */
	public long checkTimeStamps() {
		if (nextSaveEvent != null) {
			return nextSaveEvent.getTimeStamp();
		}
		else if (nextEvent != null) {
			return nextEvent.getTimeStamp();
		}
		return Long.MAX_VALUE;
	}
	
	public int getSchedulingPriority() {
		return priority;
	}

	public String getId() {
		return stateMachineId;
	}
	
}

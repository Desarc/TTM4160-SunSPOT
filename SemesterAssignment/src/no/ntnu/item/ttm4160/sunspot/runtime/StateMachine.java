package no.ntnu.item.ttm4160.sunspot.runtime;

import no.ntnu.item.ttm4160.sunspot.utils.Event;

public abstract class StateMachine {
	
	private String stateMachineId;
	private int state;
	
	/**
	 * Must always be implemented in any subclass!
	 * 
	 * @param event
	 */
	public void assignEvent(Event event) {
		
	}
	
	/**
	 * Tells scheduler to save an event.
	 *
	 */
	public void saveEvent(Event event) {
		
	}
	
	/**
	 * Returns control to scheduler.
	 * 
	 */
	public void returnControlToScheduler() {
		
	}
	
}

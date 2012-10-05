package no.ntnu.item.ttm4160.sunspot.runtime;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.utils.Event;

public abstract class StateMachine extends Thread {
	
	protected String stateMachineId;
	protected int state;
	protected Scheduler scheduler;
	protected SunSpotApplication app;
	
	
	class NotImplementedException extends Exception {
		
	}
	
	/**
	 * Must always be implemented in any subclass!
	 * 
	 * @param event
	 */
	public void assignEvent(Event event) throws NotImplementedException {
		throw new NotImplementedException();
	}
	
	/**
	 * Tells scheduler to save an event.
	 *
	 */
	public void saveEvent(Event event) {
		scheduler.saveEvent(event, stateMachineId);
	}
	
	/**
	 * Returns control to scheduler.
	 * 
	 */
	public void returnControlToScheduler() {
		notify();
	}
	
}

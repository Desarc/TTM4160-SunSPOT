package no.ntnu.item.ttm4160.sunspot.runtime;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.utils.Event;

public abstract class StateMachine extends Thread {
	
	protected String stateMachineId;
	protected int state;
	protected Scheduler scheduler;
	protected SunSpotApplication app;
	protected Event currentEvent;
	
	
	class NotImplementedException extends Exception {
		
	}
	
	
	public void assignEvent(Event event) {
		currentEvent = event;
		start(); //calls run()
	}
	
	/**
	 * Must always be implemented in any subclass!
	 * 
	 */
	public void run() {
		try {
			throw new NotImplementedException();
		} catch (NotImplementedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		scheduler.getNextEvent();
	}
	
}

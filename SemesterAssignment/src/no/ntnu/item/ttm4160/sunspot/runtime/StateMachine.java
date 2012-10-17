package no.ntnu.item.ttm4160.sunspot.runtime;

import com.sun.spot.sensorboard.peripheral.LEDColor;

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
	
	public StateMachine(String stateMachineId, Scheduler scheduler, SunSpotApplication app) {
		this.stateMachineId = stateMachineId;
		this.scheduler = scheduler;
		this.app = app;
	}
	
	public void assignEvent(Event event) {
		for (int i = 0; i < app.leds.length; i++) {
			app.leds[i].setColor(LEDColor.BLUE);
			app.leds[i].setOn();
		}
		currentEvent = event;
		run(); //calls run()
	}
	
	/**
	 * Must always be implemented in any subclass!
	 * 
	 */
	public abstract void run();
	
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
	
	public String getId() {
		return stateMachineId;
	}
	
}

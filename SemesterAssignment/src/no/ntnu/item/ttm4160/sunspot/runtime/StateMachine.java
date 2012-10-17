package no.ntnu.item.ttm4160.sunspot.runtime;


import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.utils.Event;

/**
 * Abstract class for state machines, defining some common methods and variables. Default priority is 0.
 *
 */
public abstract class StateMachine extends Thread {
	
	protected String stateMachineId;
	protected int state;
	protected Scheduler scheduler;
	protected SunSpotApplication app;
	protected Event currentEvent;
	/*
	 * Default priority is zero.
	 */
	protected int priority = 0;
	
	/**
	 * 
	 * @param stateMachineId The unique ID of this state machine. {@link String}
	 * @param scheduler A reference to the {@link Scheduler}.
	 * @param app A reference to the {@link SunSpotApplication} for LED-control.
	 */
	public StateMachine(String stateMachineId, Scheduler scheduler, SunSpotApplication app) {
		this.stateMachineId = stateMachineId;
		this.scheduler = scheduler;
		this.app = app;
	}
	
	/**
	 * 
	 * @param stateMachineId The unique ID of this state machine. {@link String}
	 * @param scheduler A reference to the {@link Scheduler}.
	 * @param app A reference to the {@link SunSpotApplication} for LED-control.
	 * @param priority The scheduling priority for this state machine. {@link int}
	 */
	public StateMachine(String stateMachineId, Scheduler scheduler, SunSpotApplication app, int priority) {
		this(stateMachineId, scheduler, app);
		this.priority = priority;
	}
	
	/**
	 * Assigns an events to this {@link StateMachine}, and lets it execute.
	 * @param event
	 */
	public void assignEvent(Event event) {
		currentEvent = event;
		run();
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
	 * Returns control to scheduler, allowing another event to be processed.
	 * 
	 */
	public void returnControlToScheduler() {
		scheduler.getNextEvent();
	}
	
	public String getId() {
		return stateMachineId;
	}
	
}

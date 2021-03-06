package no.ntnu.item.ttm4160.sunspot.runtime;


import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.utils.Event;

/**
 * Abstract class for state machines, defining some common methods and variables. Default priority is 0.
 *
 */
public abstract class StateMachine extends Thread {
	
	protected String stateMachineId;
	protected String state;
	protected Scheduler scheduler;
	protected SunSpotApplication app;
	protected Event currentEvent;
	protected TimerHandler timerHandler;
	protected boolean active;
	/*
	 * Default priority is zero.
	 */
	protected int priority = 0;
	
	public static final int Inf = Integer.MAX_VALUE;
	
	/**
	 * 
	 * @param stateMachineId The unique ID of this state machine. {@link String}
	 * @param scheduler A reference to the {@link Scheduler}.
	 * @param app A reference to the {@link SunSpotApplication} for LED-control.
	 */
	public StateMachine(String stateMachineId, Scheduler scheduler, TimerHandler timerHandler, SunSpotApplication app) {
		this.stateMachineId = stateMachineId;
		this.scheduler = scheduler;
		this.app = app;
		this.active = true;
		this.timerHandler = timerHandler;
	}
	
	/**
	 * 
	 * @param stateMachineId The unique ID of this state machine. {@link String}
	 * @param scheduler A reference to the {@link Scheduler}.
	 * @param app A reference to the {@link SunSpotApplication} for LED-control.
	 * @param priority The scheduling priority for this state machine. {@link int}
	 */
	public StateMachine(String stateMachineId, Scheduler scheduler, TimerHandler timerHandler, SunSpotApplication app, int priority) {
		this(stateMachineId, scheduler, timerHandler, app);
		this.priority = priority;
	}
	
	/**
	 * Creates a {@link Thread} instance for the state machine, and starts thread execution.
	 * @return
	 */
	public Thread startThread() {
		Thread stateMachineThread = new Thread(this);
		stateMachineThread.start();
		return stateMachineThread;
	}
	/**
	 * Assigns an events to this {@link StateMachine}, and lets it execute.
	 * @param event
	 */
	public void assignEvent(Event event) {
		currentEvent = event;
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
	 * @param terminate Tells the scheduler if this state machine should be terminated.
	 */
	public void returnControlToScheduler(boolean terminate) {
		scheduler.returnControl(terminate, stateMachineId);
	}
	
	public String getStateMachineId() {
		return stateMachineId;
	}
	
	public int getStateMachinePriority() {
		return this.priority;
	}
	
	public void deactivate() {
		active = false;
	}
	
	public boolean isActive() {
		return active;
	}
}

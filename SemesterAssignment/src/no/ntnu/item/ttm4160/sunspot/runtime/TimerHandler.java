package no.ntnu.item.ttm4160.sunspot.runtime;

import java.util.Vector;

import no.ntnu.item.ttm4160.sunspot.utils.Event;
import no.ntnu.item.ttm4160.sunspot.utils.SPOTTimer;

import com.sun.spot.util.Queue;

/**
 * Class for handling timers and timeout-events for a specific {@link StateMachine}.
 *
 */
public class TimerHandler extends Thread {
	
	private Queue timeoutEventQueue;
	private Vector activeTimers;
	private String stateMachineId;
	private Event nextEvent;
	private Scheduler scheduler;
	private int priority;
	
	/**
	 * 
	 * @param stateMachineId {@link String} The ID of the {@link StateMachine} this handler belongs to.
	 * @param scheduler A reference to the {@link Scheduler} for timeout notification.
	 * @param priority {@link int} Scheduling priority for this handler's timeout events.
	 */
	public TimerHandler(String stateMachineId, Scheduler scheduler, int priority) {
		this.stateMachineId = stateMachineId;
		this.scheduler = scheduler;
		this.priority = priority;
		activeTimers = new Vector();
		timeoutEventQueue = new Queue();
		
	}
	
	/**
	 * Creates a new timer with an event to be processed at timeout.
	 * @param time {@link long}
	 * @param event {@link Event}
	 */
	public synchronized void startNewTimer(long time, Event event) {
		SPOTTimer timer = new SPOTTimer(time, event, this);
		activeTimers.addElement(timer);
		timer.start();
	}
	
	/**
	 * Method called by a running {@link SPOTTimer}, to signal a timeout.
	 * Deletes the timer, updates the event timestamp, and adds the event to the processing queue.
	 * Finally, notifies the scheduler that a timeout has occurred.
	 * @param timer
	 */
	public synchronized void timeout(SPOTTimer timer) {
		activeTimers.removeElement(timer);
		Event timeout = timer.getEvent();
		timeout.setTimeStamp(System.currentTimeMillis());
		if (nextEvent == null) {
			nextEvent = timeout;
		}
		else {
			timeoutEventQueue.put(timeout);
		}
		scheduler.timerNotify();
	}
	
	/**
	 * Stops all running timers, and removes any existing timeout-events for the corresponding {@link StateMachine}.
	 */
	public synchronized void killAllTimers() {
		for (int i = 0; i < activeTimers.size(); i++) {
			Object element = activeTimers.elementAt(i);
			((SPOTTimer)element).cancel();
		}
		activeTimers = new Vector();
		timeoutEventQueue = new Queue();
	}
	
	/**
	 * Returns the timestamp for the next timeout-event to be processed, or Long.MAX_VALUE if there are no timeout-events.
	 * @return {@link long}
	 */
	public synchronized long checkTimeoutQueue() {
		if (nextEvent == null) {
			return Long.MAX_VALUE;
		}
		return nextEvent.getTimeStamp();
	}

	/**
	 * Returns the next timeout-event to be processed for the corresponding state machine, or null if there are no timeout-events.
	 * @return {@link Event}
	 */
	public synchronized Event getNextEvent() {
		Event next;
		if (timeoutEventQueue.size() > 0) {
			next = nextEvent;
			nextEvent = (Event)timeoutEventQueue.get();
		}
		else {
			next = nextEvent;
			nextEvent = null;
		}
		return next;
	}
	
	public String getStateMachineId() {
		return stateMachineId;
	}
	
	public int getSchedulingPriority() {
		return priority;
	}

	public String getId() {
		return stateMachineId;
	}
}

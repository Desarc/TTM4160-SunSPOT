package no.ntnu.item.ttm4160.sunspot.runtime;

import java.util.Enumeration;
import java.util.Hashtable;

import no.ntnu.item.ttm4160.sunspot.utils.Event;
import no.ntnu.item.ttm4160.sunspot.utils.SPOTTimer;

import com.sun.spot.util.Queue;

/**
 * Class for handling timers and timeout-events for a specific {@link StateMachine}.
 *
 */
public class TimerHandler extends Thread {
	
	private Queue timeoutEventQueue;
	private Hashtable activeTimers;
	private Hashtable activeTimerThreads;
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
		activeTimers = new Hashtable();
		timeoutEventQueue = new Queue();
		activeTimerThreads = new Hashtable();
	}
	
	/**
	 * Creates a new timer with an event to be processed at timeout.
	 * @param time {@link long}
	 * @param event {@link Event}
	 */
	public synchronized String startNewTimer(long time) {
		SPOTTimer timer = new SPOTTimer(time, this);
		activeTimers.put(timer.getTimerId(), timer);
		Thread timerThread = timer.startThread();
		activeTimerThreads.put(timer.getTimerId(), timerThread);
		timerThread.interrupt();
		return timer.getTimerId();
	}
	
	/**
	 * Method called by a running {@link SPOTTimer}, to signal a timeout.
	 * Deletes the timer, updates the event timestamp, and adds the event to the processing queue.
	 * Finally, notifies the scheduler that a timeout has occurred.
	 * @param timer
	 */
	public synchronized void timeout(SPOTTimer timer) {
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
	
	public void resetTimer(String timerId) {
		Thread timerThread = (Thread)activeTimerThreads.get(timerId);
		timerThread.interrupt();
	}
	
	public void startTimer(String timerId, Event event) {
		SPOTTimer timer = (SPOTTimer)activeTimers.get(timerId);
		if (!timer.isRunning()) {
			timer.setEvent(event);
			Thread timerThread = (Thread)activeTimerThreads.get(timerId);
			timerThread.interrupt();
		}
	}
	
	public synchronized void killTimer(String timerId) {
		((SPOTTimer)activeTimers.get(timerId)).deactivate();
		activeTimers.remove(timerId);
		((Thread)activeTimerThreads.get(timerId)).interrupt();
		activeTimerThreads.remove(timerId);
	}
	
	/**
	 * Stops all running timers, and removes any existing timeout-events for the corresponding {@link StateMachine}.
	 */
	public synchronized void killAllTimers() {
		Enumeration keys = activeTimers.keys();
		while (keys.hasMoreElements()) {
			killTimer((String)keys.nextElement());
		}
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

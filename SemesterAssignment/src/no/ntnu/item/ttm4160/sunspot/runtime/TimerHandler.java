package no.ntnu.item.ttm4160.sunspot.runtime;

import java.util.Enumeration;
import java.util.Hashtable;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.utils.Event;
import no.ntnu.item.ttm4160.sunspot.utils.SPOTTimer;

import com.sun.spot.util.Queue;

/**
 * Class for handling timers and timeout-events for a specific {@link StateMachine}.
 *
 */
public class TimerHandler {
	
	private Queue timeoutEventQueue;
	private Hashtable activeTimers;
	private Hashtable activeTimerThreads;
	private String stateMachineId;
	private Event nextEvent;
	private Scheduler scheduler;
	private int priority;
	private EventHandler eventHandler;
	
	/**
	 * 
	 * @param stateMachineId {@link String} The ID of the {@link StateMachine} this handler belongs to.
	 * @param scheduler A reference to the {@link Scheduler} for timeout notification.
	 * @param priority {@link int} Scheduling priority for this handler's timeout events.
	 */
	public TimerHandler(String stateMachineId, Scheduler scheduler, EventHandler eventHandler, int priority) {
		this.stateMachineId = stateMachineId;
		this.scheduler = scheduler;
		this.eventHandler = eventHandler;
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
	public synchronized String addNewTimer(long time) {
		SPOTTimer timer = new SPOTTimer(time, this);
		activeTimers.put(timer.getTimerId(), timer);
		Thread timerThread = timer.startThread();
		activeTimerThreads.put(timer.getTimerId(), timerThread);
		return timer.getTimerId();
	}
	
	/**
	 * Method called by a running {@link SPOTTimer}, to signal a timeout.
	 * Updates the event timestamp, and adds the event to the processing queue.
	 * Finally, notifies the scheduler that a timeout has occurred.
	 * @param timer
	 */
	public synchronized void timeout(SPOTTimer timer) {
		Event timeout = timer.getEvent();
		if (timeout.getType().equals(Event.broadcastGiveUp)) {
			eventHandler.decreaseActiveSendConnections();
		}
		else if (timeout.getType().equals(Event.receiverGiveUp)) {
			eventHandler.decreaseActiveReceiveConnections();
		}
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
	 * Resets a timer, making it start again with the same event type.
	 * @param timerId
	 */
	public synchronized void resetTimer(String timerId) {
		Thread timerThread = (Thread)activeTimerThreads.get(timerId);
		if (SunSpotApplication.output) {
			System.out.println("TIMERHANDLER INTERRUPTING TIMER "+timerThread);
		}
		if (timerThread != null) {
			timerThread.interrupt();
		}
	}
	
	/**
	 * Starts a given {@link SPOTTimer}, and provides it with an {@link Event} to forward at timeout.
	 */
	public synchronized void startTimer(String timerId, Event event) {
		SPOTTimer timer = (SPOTTimer)activeTimers.get(timerId);
		if (!timer.isRunning()) {
			timer.setEvent(event);
			Thread timerThread = (Thread)activeTimerThreads.get(timerId);
			if (SunSpotApplication.output) {	
				System.out.println("TIMERHANDLER INTERRUPTING TIMER "+timerThread);
			}
			timerThread.interrupt();
		}
	}
	
	/**
	 * Kills a given {@link SPOTTimer}.
	 * @param timerId
	 */
	public synchronized void killTimer(String timerId) {
		((SPOTTimer)activeTimers.get(timerId)).deactivate();
		activeTimers.remove(timerId);
		if (SunSpotApplication.output) {	
			System.out.println("TIMERHANDLER INTERRUPTING TIMER "+(Thread)activeTimerThreads.get(timerId));
		}
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
		timeoutEventQueue = new Queue();
	}
	
	/**
	 * Reset all running timers for the corresponding {@link StateMachine}.
	 */
	public synchronized void resetAllTimers() {
		Enumeration keys = activeTimers.keys();
		while (keys.hasMoreElements()) {
			resetTimer((String)keys.nextElement());
		}
	}
	
	/**
	 * Stops a given timer.
	 * @param timerId
	 */
	public synchronized void stopTimer(String timerId) {
		((SPOTTimer)activeTimers.get(timerId)).stop();
		((Thread)activeTimerThreads.get(timerId)).interrupt();
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
	
	public int getSchedulingPriority() {
		return priority;
	}

	public String getStatemachineId() {
		return stateMachineId;
	}
}

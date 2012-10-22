package no.ntnu.item.ttm4160.sunspot.runtime;

import java.util.Enumeration;
import java.util.Hashtable;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.communication.*;
import no.ntnu.item.ttm4160.sunspot.utils.*;

/**
 * Scheduler class for the SunSPOT. Handles messages from the {@link Communications} module,
 * action events from the {@link SunSpotApplication} module, timeout events from {@link SPOTTimer}s,
 * and assigns them to their respective {@link StateMachine}s in the proper order.
 *
 */
public class Scheduler {
	
	private Hashtable activeStateMachines;
	private Hashtable eventQueues;
	private Hashtable timerHandlers;
	private int state;
	
	public static final int idle = 0; //no events being processed by any state machine
	public static final int busy = 1; //a state machine is processing an event
	
	
	/**
	 * Initiates a new scheduler with no active {@link StateMachine}.
	 */
	public Scheduler() {
		state = idle;
		eventQueues = new Hashtable();
		timerHandlers = new Hashtable();
		activeStateMachines = new Hashtable();
	}
	
	
	/**
	 * Saves an event to the proper {@link EventQueue}.
	 * @param event
	 * @param stateMachineId
	 */
	public synchronized void saveEvent(Event event, String stateMachineId) {
		EventQueue queue = (EventQueue)eventQueues.get(stateMachineId);
		queue.saveEvent(event);
		
	}
	
	/**
	 * Method allowing {@link TimerHandler}s to notify the scheduler of timeouts. Starts event processing if
	 * no {@link StateMachine}s are running.
	 */
	public synchronized void timerNotify() {
		if (state == busy) {
			return;
		}
		getNextEvent();
	}
	
	/**
	 * Checks all {@link EventQueue}s and {@link TimerHandler}s, and decides which {@link Event} to process next according
	 * to priorities and timestamps. Timeout-events from {@link TimerHandler}s are always prioritized before events from
	 * {@link EventQueue}s (additionally, saved events are prioritized before other events). Further, events belonging to
	 * {@link StateMachine}s with higher priority are processed first. Finally, if all other priorities are equal, events
	 * are prioritized according to timestamps, from lowest to highest (FIFO). If an event is found, it is assigned to the
	 * corresponding state machine. If there are no events to process, the scheduler simply waits.
	 * Each event assigned to a state machine spawns a new {@link Thread}, which is terminated once control is returned to the
	 * scheduler.
	 */
	private synchronized void getNextEvent() {
		
		state = busy;
		EventQueue currentQueue = null;
		TimerHandler currentHandler = null;
		Event currentEvent;
		long nextTime = Long.MAX_VALUE;
		
		//timeout-events have priority
		for (Enumeration e = timerHandlers.keys(); e.hasMoreElements() ;) {
			TimerHandler handler = (TimerHandler)timerHandlers.get(e.nextElement());
			long time = handler.checkTimeoutQueue();
			if (time < nextTime) {
				nextTime = time;
				currentHandler = handler;
			}
		}
		if (nextTime < Long.MAX_VALUE) {
			currentEvent = currentHandler.getNextEvent();
			StateMachine currentMachine = (StateMachine)activeStateMachines.get(currentEvent.getStateMachineId());
			currentMachine.assignEvent(currentEvent);
			state = idle;
			return;
		}
		
		//checking for other events if there are no timeouts
		for (Enumeration e = eventQueues.keys(); e.hasMoreElements() ;) {
			EventQueue queue = (EventQueue)eventQueues.get(e.nextElement());
			long time = queue.checkTimeStamps();
			if (time < nextTime) {
				nextTime = time;
				currentQueue = queue;
			}
		}
		if (nextTime < Long.MAX_VALUE) {
			currentEvent = currentQueue.getNextEvent();
			StateMachine currentMachine = (StateMachine)activeStateMachines.get(currentEvent.getStateMachineId());			
			currentMachine.assignEvent(currentEvent);
			state = idle;
			return;
		}
		//no events in any queue
		state = idle;
		return;
	}
	
	/**
	 * Starts a new timer for a given {@link StateMachine}.
	 * @param stateMachineId
	 * @param event {@link Event} to be processed at timeout.
	 * @param time Time before timeout. {@link long}
	 */
	public synchronized void addTimer(String stateMachineId, Event event, long time) {
		TimerHandler handler = (TimerHandler)timerHandlers.get(stateMachineId);
		handler.startNewTimer(time, event);
	}

	/**
	 * Returns control to the scheduler, allowing another event to be processed. If terminate is set,
	 * the scheduler terminates the {@link StateMachine} releasing control, in addition to its {@link EventQueue}
	 * and {@link TimerHandler}.
	 * @param terminate {@link boolean}
	 * @param stateMachineId
	 */
	public synchronized void returnControl(boolean terminate, String stateMachineId) {
		if (terminate) {
			activeStateMachines.remove(stateMachineId);
			((TimerHandler)timerHandlers.get(stateMachineId)).killAllTimers();
			timerHandlers.remove(stateMachineId);
			eventQueues.remove(stateMachineId);
		}
		getNextEvent();
	}

	public synchronized void addStateMachine(StateMachine stateMachine) {
		activeStateMachines.put(stateMachine.getId(), stateMachine);
	}

	public synchronized void addEventQueue(EventQueue eventQueue) {
		eventQueues.put(eventQueue.getId(), eventQueue);
	}

	public synchronized void addTimerHandler(TimerHandler handler) {
		timerHandlers.put(handler.getId(), handler);		
	}
	
	/**
	 * Adds an event to it's proper queue, and starts event processing if no {@link StateMachine} is running.
	 * @param event
	 */
	public synchronized void addEvent(Event event) {
		EventQueue queue = (EventQueue)eventQueues.get(event.getStateMachineId());
		queue.addEvent(event);
		if (state == busy) {
			return;
		}
		getNextEvent();
	}
	
}

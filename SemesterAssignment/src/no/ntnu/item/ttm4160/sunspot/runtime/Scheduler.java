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
public class Scheduler extends Thread {
	
	private Hashtable activeStateMachines;
	private Hashtable activeThreads;
	private Hashtable eventQueues;
	private Hashtable timerHandlers;
	private String state;
	private int starvationCounter;
	private String previousStateMachine;
	
	private boolean discardOldEvents = false;
	private int oldEventLimit = 2000;
	private int starvationLimit = 3;
	
	public static final String idle = "idle"; //no events being processed by any state machine
	public static final String busy = "busy"; //a state machine is processing an event
	
	public static final int Inf = Integer.MAX_VALUE;
	
	/**
	 * Initiates a new scheduler with no active {@link StateMachine}.
	 */
	public Scheduler() {
		state = idle;
		eventQueues = new Hashtable();
		timerHandlers = new Hashtable();
		activeStateMachines = new Hashtable();
		activeThreads = new Hashtable();
		starvationCounter = 0;
		previousStateMachine = "";
	}
	
	
	public void run() {
		while (true) {
			try {
				if (SunSpotApplication.output) {
					System.out.println("Scheduler going to sleep...");
				}
				sleep(Inf);
			} catch (InterruptedException e) {
				if (SunSpotApplication.output) {
					System.out.println("Scheduler thread: "+Thread.currentThread());
				}
				getNextEvent();
			}
		}
	}
	
	/**
	 * Saves an event to the proper {@link EventQueue}.
	 * @param event
	 * @param stateMachineId
	 */
	public synchronized void saveEvent(Event event, String stateMachineId) {
		if (SunSpotApplication.output) {
			System.out.println("Event saved");
		}
		EventQueue queue = (EventQueue)eventQueues.get(stateMachineId);
		queue.saveEvent(event);
		
	}
	
	/**
	 * Method allowing {@link TimerHandler}s to notify the scheduler of timeouts. Starts event processing if
	 * no {@link StateMachine}s are running.
	 */
	public synchronized void timerNotify() {
		if (SunSpotApplication.output) {
			System.out.println("Scheduler notified of timeout");
		}
		if (state == busy) {
			return;
		}
		if (SunSpotApplication.output) {
			System.out.println("SCHEDULER INTERRUPTING ITSELF!");
		}
		interrupt();
	}
	
	/**
	 * Checks all {@link EventQueue}s and {@link TimerHandler}s, and decides which {@link Event} to process next according
	 * to priorities and timestamps. Internal events are always prioritized first, to avoid slow response because of accumulating
	 * timeouts. Timeout events from {@link TimerHandler}s are always prioritized before external events from
	 * {@link EventQueue}s (additionally, saved events are prioritized before other events). Further, events belonging to
	 * {@link StateMachine}s with higher priority are processed first. Finally, if all other priorities are equal, events
	 * are prioritized according to timestamps, from lowest to highest (FIFO). If an event is found, it is assigned to the
	 * corresponding state machine. If there are no events to process, the scheduler simply waits.
	 * To avoid starvation, we check if a state machine has consumed many events in a row, and if so, temporarily decrease
	 * its priority.
	 */
	private synchronized void getNextEvent() {
		if (SunSpotApplication.output) {
			System.out.println("Getting next event, state: "+state);
		}
		
		boolean skip = checkStarvation();
		state = busy;
		EventQueue currentQueue = null;
		TimerHandler currentHandler = null;
		int currentPriority = 0;
		Event currentEvent;
		long nextTime = Long.MAX_VALUE;
		
		//internal events must be prioritized first
		for (Enumeration e = eventQueues.keys(); e.hasMoreElements() ;) {
			String stateMachineId = (String)e.nextElement();
			if (stateMachineId == previousStateMachine && skip) {
				continue;
			}
			EventQueue queue = (EventQueue)eventQueues.get(stateMachineId);
			long time = queue.checkInternalTimeStamps();
			int priority = queue.getSchedulingPriority();
			if (time < nextTime && priority >= currentPriority) {
				nextTime = time;
				currentQueue = queue;
				currentPriority = priority;
			}
		}
		if (nextTime < Long.MAX_VALUE) {
			if (SunSpotApplication.output) {
				System.out.println("Next event is internal event.");
			}
			currentEvent = currentQueue.getNextEvent();
			if (discardOldEvents && (currentEvent.getTimeStamp()+oldEventLimit) < System.currentTimeMillis()) {
				getNextEvent();
				return;
			}
			StateMachine currentMachine = (StateMachine)activeStateMachines.get(currentEvent.getStateMachineId());			
			currentMachine.assignEvent(currentEvent);
			Thread currentThread = (Thread)activeThreads.get(currentEvent.getStateMachineId());
			if (currentMachine.getId().equals(previousStateMachine)) {
				starvationCounter++;
			}
			else {
				starvationCounter = 0;
			}
			previousStateMachine = currentMachine.getId();
			if (SunSpotApplication.output) {
				System.out.println("SCHEDULER INTERRUPTING STATE MACHINE "+currentThread);
			}
			currentThread.interrupt();
			return;
		}
		
		
		//timeout events have priority over external events
		for (Enumeration e = timerHandlers.keys(); e.hasMoreElements() ;) {
			String stateMachineId = (String)e.nextElement();
			if (stateMachineId == previousStateMachine && skip) {
				continue;
			}
			TimerHandler handler = (TimerHandler)timerHandlers.get(stateMachineId);
			long time = handler.checkTimeoutQueue();
			int priority = handler.getSchedulingPriority();
			if (time < nextTime && priority >= currentPriority) {
				nextTime = time;
				currentHandler = handler;
				currentPriority = priority;
			}
		}
		if (nextTime < Long.MAX_VALUE) {
			if (SunSpotApplication.output) {
				System.out.println("Next event is timeout");
			}
			currentEvent = currentHandler.getNextEvent();
			if (discardOldEvents && (currentEvent.getTimeStamp()+oldEventLimit) < System.currentTimeMillis()) {
				getNextEvent();
				return;
			}
			StateMachine currentMachine = (StateMachine)activeStateMachines.get(currentEvent.getStateMachineId());
			currentMachine.assignEvent(currentEvent);
			Thread currentThread = (Thread)activeThreads.get(currentEvent.getStateMachineId());
			if (currentMachine.getId().equals(previousStateMachine)) {
				starvationCounter++;
			}
			else {
				starvationCounter = 0;
			}
			previousStateMachine = currentMachine.getId();
			if (SunSpotApplication.output) {
				System.out.println("SCHEDULER INTERRUPTING STATE MACHINE "+currentThread);
			}
			currentThread.interrupt();
			return;
		}
		
		//checking for external events, if there are no timeouts or internal events
		for (Enumeration e = eventQueues.keys(); e.hasMoreElements() ;) {
			String stateMachineId = (String)e.nextElement();
			if (stateMachineId == previousStateMachine && skip) {
				continue;
			}
			EventQueue queue = (EventQueue)eventQueues.get(stateMachineId);
			long time = queue.checkExternalTimeStamps();
			int priority = queue.getSchedulingPriority();
			if (time < nextTime && priority >= currentPriority) {
				nextTime = time;
				currentQueue = queue;
				currentPriority = priority;
			}
		}
		if (nextTime < Long.MAX_VALUE) {
			if (SunSpotApplication.output) {
				System.out.println("Next event is external event");
			}
			currentEvent = currentQueue.getNextEvent();
			if (discardOldEvents && (currentEvent.getTimeStamp()+oldEventLimit) < System.currentTimeMillis()) {
				getNextEvent();
				return;
			}
			StateMachine currentMachine = (StateMachine)activeStateMachines.get(currentEvent.getStateMachineId());			
			currentMachine.assignEvent(currentEvent);
			Thread currentThread = (Thread)activeThreads.get(currentEvent.getStateMachineId());
			if (currentMachine.getId().equals(previousStateMachine)) {
				starvationCounter++;
			}
			else {
				starvationCounter = 0;
			}
			previousStateMachine = currentMachine.getId();
			if (SunSpotApplication.output) {
				System.out.println("SCHEDULER INTERRUPTING STATE MACHINE "+currentThread);
			}
			currentThread.interrupt();
			return;
		}
		//no events in any queue
		if (SunSpotApplication.output) {
			System.out.println("No event");
		}
		if (skip) {
			getNextEvent();
		}
		state = idle;
		return;
	}
	
	/**
	 * Checks if any {@link StateMachine} has executed many {@link Event}s in a row. (= starvation for other state machines).
	 * Resets starvationCounter in case no other state machines have events pending.
	 * @return
	 */
	private synchronized boolean checkStarvation() {
		if (starvationCounter >= starvationLimit && activeStateMachines.size() > 1) {
			if (SunSpotApplication.output) {
				System.out.println("Priorities changed to avoid potential starvation.");
			}
			starvationCounter = 0;
			return true;
		}
		return false;
	}


	/**
	 * Adds a new {@link SPOTTimer} for a given {@link StateMachine}.
	 * @param stateMachineId
	 * @param time Time before timeout. {@link long}
	 * @return 
	 */
	public synchronized String addTimer(String stateMachineId, long time) {
		TimerHandler handler = (TimerHandler)timerHandlers.get(stateMachineId);
		return handler.addNewTimer(time);
	}
	
	/**
	 * Starts a given {@link SPOTTimer}.
	 * @param stateMachineId
	 * @param timerId
	 * @param event
	 */
	public synchronized void startTimer(String stateMachineId, String timerId, Event event) {
		TimerHandler handler = (TimerHandler)timerHandlers.get(stateMachineId);
		handler.startTimer(timerId, event);
	}

	/**
	 * Returns control to the scheduler, allowing another event to be processed. If terminate is set,
	 * the scheduler terminates the {@link StateMachine} releasing control, in addition to its {@link EventQueue}
	 * and {@link TimerHandler}.
	 * @param terminate {@link boolean}
	 * @param stateMachineId
	 */
	public synchronized void returnControl(boolean terminate, String stateMachineId) {
		if (SunSpotApplication.output) {
			System.out.println("Control returned to scheduler");
		}
		if (terminate) {
			terminateStateMachine(stateMachineId);
		}
		if (SunSpotApplication.output) {
			System.out.println("SCHEDULER INTERRUPTING ITSELF!");
		}
		interrupt();
	}
	
	/**
	 * Terminates the specified {@link StateMachine}, and any {@link Thread}s, {@link EventQueue}s, {@link SPOTTimer}s
	 * or {@link TimerHandler}s associated with it.
	 * @param stateMachineId
	 */
	private synchronized void terminateStateMachine(String stateMachineId) {
		((StateMachine)activeStateMachines.get(stateMachineId)).deactivate();
		activeStateMachines.remove(stateMachineId);
		activeThreads.remove(stateMachineId);
		((TimerHandler)timerHandlers.get(stateMachineId)).killAllTimers();
		timerHandlers.remove(stateMachineId);
		eventQueues.remove(stateMachineId);
	}

	public synchronized void addStateMachine(StateMachine stateMachine) {
		activeStateMachines.put(stateMachine.getId(), stateMachine);
	}
	
	public synchronized void addStateMachineThread(Thread stateMachineThread, String stateMachineId) {
		activeThreads.put(stateMachineId, stateMachineThread);
	}

	public synchronized void addEventQueue(EventQueue eventQueue) {
		eventQueues.put(eventQueue.getStateMachineId(), eventQueue);
	}

	public synchronized void addTimerHandler(TimerHandler handler) {
		timerHandlers.put(handler.getStatemachineId(), handler);
	}
	
	/**
	 * Adds an internal event to it's proper queue, and starts event processing if no {@link StateMachine} is running.
	 * @param event
	 */
	public synchronized void addInternalEvent(Event event) {
		if (SunSpotApplication.output) {
			System.out.println("Adding internal event");
		}
		EventQueue queue = (EventQueue)eventQueues.get(event.getStateMachineId());
		queue.addInternalEvent(event);
		if (state == busy) {
			return;
		}
		if (SunSpotApplication.output) {
			System.out.println("SCHEDULER INTERRUPTING ITSELF!");
		}
		interrupt();
	}
	
	/**
	 * Adds an external event to it's proper queue, and starts event processing if no {@link StateMachine} is running.
	 * @param event
	 */
	public synchronized void addExternalEvent(Event event) {
		if (SunSpotApplication.output) {
			System.out.println("Adding external event");
		}
		EventQueue queue = (EventQueue)eventQueues.get(event.getStateMachineId());
		queue.addExternalEvent(event);
		if (state == busy) {
			return;
		}
		if (SunSpotApplication.output) {
			System.out.println("SCHEDULER INTERRUPTING ITSELF!");
		}
		interrupt();
	}
	
	/**
	 * Kills all {@link SPOTTimer}s for a given {@link StateMachine}.
	 * @param stateMachineId
	 */
	public synchronized void killAllTimers(String stateMachineId) {
		if (SunSpotApplication.output) {
			System.out.println("Killing timers for "+stateMachineId);
		}
		TimerHandler handler = (TimerHandler)timerHandlers.get(stateMachineId);
		handler.killAllTimers();
	}
	
	/**
	 * Kills a given {@link SPOTTimer}.
	 * @param stateMachineId
	 * @param timerId
	 */
	public synchronized void killTimer(String stateMachineId, String timerId) {
		if (SunSpotApplication.output) {
			System.out.println("Killing timer "+timerId);
		}
		TimerHandler handler = (TimerHandler)timerHandlers.get(stateMachineId);
		handler.killTimer(timerId);
	}


	public synchronized Enumeration getIDs() {
		return activeStateMachines.keys();
	}
	
	public synchronized Enumeration getActiveStateMachineElements(){
		return activeStateMachines.elements();
	}
	
	public synchronized Hashtable getActiveStateMachineConnections(){
		Enumeration elements = activeStateMachines.elements();
		int size = 0;
		Hashtable connections = new Hashtable();
		while (elements.hasMoreElements()) {
			StateMachine sm = (StateMachine) elements.nextElement();
			connections = sm.app.com.getRemoteAddressBook();
			size = connections.size();
			break;
		}
		return connections;
	}

	/**
	 * Resets a given {@link SPOTTimer}.
	 * @param stateMachineId
	 * @param currentTimer
	 */
	public synchronized void resetTimer(String stateMachineId, String currentTimer) {
		if (SunSpotApplication.output) {
			System.out.println("Resetting timer");
		}
		((TimerHandler)timerHandlers.get(stateMachineId)).resetTimer(currentTimer);
	}
	
	/**
	 * Checks if a given {@link StateMachine} exists, and is active.
	 * @param stateMachineId
	 * @return
	 */
	public synchronized boolean checkIfActive(String stateMachineId) {
		StateMachine stateMachine = (StateMachine)activeStateMachines.get(stateMachineId);
		if ( stateMachine != null) {
			return stateMachine.isActive();
		}
		return false;
	}
	
	
	/**
	 * 
	 */
	public synchronized boolean checkIfReceiver(String MAC) {
		Enumeration elements = activeStateMachines.elements();
		while (elements.hasMoreElements()) {
			try {
				SendingStateMachine sm = (SendingStateMachine) elements.nextElement();
				String smMAC = sm.getReceiver();
				if (MAC.equals(smMAC.substring(0,smMAC.indexOf(":")))) {
					return true;
				}
			} catch (ClassCastException e) {}
		}
		return false;
	}
}

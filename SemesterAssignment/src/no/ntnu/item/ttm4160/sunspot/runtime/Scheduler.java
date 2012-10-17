package no.ntnu.item.ttm4160.sunspot.runtime;

import java.util.Enumeration;
import java.util.Hashtable;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.SunSpotListener;
import no.ntnu.item.ttm4160.sunspot.communication.*;
import no.ntnu.item.ttm4160.sunspot.utils.*;

/**
 * Scheduler class for the SunSPOT. Handles messages from the {@link Communications} module,
 * action events from the {@link SunSpotApplication} module, timeout events from {@link SPOTTimer}s,
 * and assigns them to their respective {@link StateMachine}s in the proper order.
 *
 */
public class Scheduler implements ICommunicationLayerListener, SunSpotListener {
	
	private Hashtable activeStateMachines;
	private SunSpotApplication app;
	private Hashtable eventQueues;
	private Hashtable timerHandlers;
	private int state;
	private int nOfConnections;
	private int maxConnections = 5;
	
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
		nOfConnections = 0;
	}
	
	/**
	 * Initiates a new scheduler with no active {@link StateMachine} and a reference to the {@link SunSpotApplication}.
	 */
	public Scheduler(SunSpotApplication app) {
		this();
		this.app = app;
	}	
	
	/**
	 * Handles a {@link Message} from the {@link Communications} module. Generates an {@link Event} based on the message content,
	 * and places the event in the proper {@link EventQueue}. Creates a new {@link StateMachine} if applicable. Starts event
	 * processing if no state machine is running.
	 * @param message
	 */
	public synchronized void inputReceived(Message message) {
		Event event = generateEvent(message);
		if (message.getContent().equals(Message.CanYouDisplayMyReadings)) {
			ReceiveStateMachine receiveStateMachine = new ReceiveStateMachine(message.getSender(), this, app);
			nOfConnections++;
			EventQueue eventQueue = new EventQueue(receiveStateMachine.getId(), receiveStateMachine.getPriority());
			eventQueue.addEvent(event);
			eventQueues.put(eventQueue.getStateMachineId(), eventQueue);
		}
		else if (message.getContent().equals(Message.ICanDisplayReadings)) {
			nOfConnections++;
			EventQueue queue = (EventQueue)eventQueues.get(message.getReceiver());
			queue.addEvent(event);
		}
		else if(message.getContent().equals(Message.Approved)) {
			EventQueue queue = (EventQueue)eventQueues.get(message.getReceiver());
			queue.addEvent(event);
		}
		else if(message.getContent().equals(Message.Denied)) {
			nOfConnections--;
			EventQueue queue = (EventQueue)eventQueues.get(message.getReceiver());
			queue.addEvent(event);
		}
		else if(message.getContent().equals(Message.ReceiverDisconnect)) {
			nOfConnections--;
			EventQueue queue = (EventQueue)eventQueues.get(message.getReceiver());
			queue.addEvent(event);
		}
		else if(message.getContent().equals(Message.SenderDisconnect)) {
			nOfConnections--;
			EventQueue queue = (EventQueue)eventQueues.get(message.getReceiver());
			queue.addEvent(event);
		}
		if (state == idle) {
			getNextEvent();
		}
	}
	
	String id;
	
	/**
	 * Handles an action from the {@link Communications} module. Generates an {@link Event} based on the action,
	 * and places the event in the proper {@link EventQueue}. Creates a new {@link StateMachine} if applicable. Starts event
	 * processing if no state machine is running.
	 */
	public void actionReceived(String action) {
		if (action.equals(SunSpotApplication.button1)) {
			
			TestStateMachine test = new TestStateMachine(""+System.currentTimeMillis(), this, app);
			activeStateMachines.put(test.getId(), test);
			EventQueue eventQueue = new EventQueue(test.getId(), test.getPriority());
			id = test.getId();
			Event event = new Event(Event.testOn, test.getId(), System.currentTimeMillis());
			eventQueue.addEvent(event);
			eventQueues.put(test.getId(), eventQueue);
			TimerHandler handler = new TimerHandler(test.getId(), this, test.getPriority());
			timerHandlers.put(test.getId(), handler);
//			SendingStateMachine sendingStateMachine = new SendingStateMachine(""+System.currentTimeMillis(), this, app);
//			activeStateMachines.put(SendingStateMachine.getId(), SendingStateMachine);
//			EventQueue eventQueue = new EventQueue(sendingStateMachine.getId(), sendingStateMachine.getPriority());
//			Event event = generateEvent(action, sendingStateMachine.getId());
//			eventQueue.addEvent(event);
//			eventQueues.put(sendingStateMachine.getId(), eventQueue);
//			TimerHandler handler = new TimerHandler(sendingStateMachine.getId(), this, sendingStateMachine.getPriority());
//			timerHandlers.put(sendingStateMachine.getId(), handler);
		}
		else if (action.equals(SunSpotApplication.button2)) {
			Event event = new Event(Event.testOff, id, System.currentTimeMillis());
			EventQueue eventQueue = (EventQueue)eventQueues.get(id);
			eventQueue.addEvent(event);
		}
		if (state == idle) {
			getNextEvent();
		}
	}
	
	/**
	 * Generates an {@link Event} based on {@link Message} content.
	 * @param message
	 * @return {@link Event}
	 */
	private Event generateEvent(Message message) {
		if(message.getReceiver().equals(Message.BROADCAST_ADDRESS)) {
			return new Event(Event.broadcast, message.getSender(), System.currentTimeMillis());
		}
		else if(message.getContent().equals(Message.ICanDisplayReadings)) {
			return new Event(Event.broadcast_response, message.getReceiver(), message.getSender(), System.currentTimeMillis());
		}
		else if(message.getContent().equals(Message.Approved)) {
			return new Event(Event.connectionApproved, message.getReceiver(), System.currentTimeMillis());
		}
		else if(message.getContent().equals(Message.Denied)) {
			return new Event(Event.connectionDenied, message.getReceiver(), System.currentTimeMillis());
		}
		else if(message.getContent().equals(Message.ReceiverDisconnect)) {
			return new Event(Event.receiverDisconnect, message.getReceiver(), System.currentTimeMillis());
		}
		else if(message.getContent().equals(Message.SenderDisconnect)) {
			return new Event(Event.senderDisconnect, message.getReceiver(), System.currentTimeMillis());
		}
		return new Event(0, "", System.currentTimeMillis());
	}
	
	/**
	 * Generates an event based on an action.
	 * @param action {@link String}
	 * @param stateMachineId The ID of the {@link StateMachine} this event is created for.
	 * @return {@link Event}
	 */
	private Event generateEvent(String action, String stateMachineId) {
		if (action.equals(SunSpotApplication.button1)) {
			return new Event(Event.broadcast, stateMachineId, System.currentTimeMillis());
		}
		else if (action.equals(SunSpotApplication.button2)) {
			return new Event(Event.disconnect, stateMachineId, System.currentTimeMillis());
		}
		return new Event(0, "", System.currentTimeMillis());
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
	
}

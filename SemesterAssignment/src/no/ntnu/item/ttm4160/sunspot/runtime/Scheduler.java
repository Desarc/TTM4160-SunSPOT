package no.ntnu.item.ttm4160.sunspot.runtime;

import java.util.Enumeration;
import java.util.Hashtable;

import com.sun.spot.sensorboard.peripheral.LEDColor;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.SunSpotListener;
import no.ntnu.item.ttm4160.sunspot.communication.*;
import no.ntnu.item.ttm4160.sunspot.utils.*;

public class Scheduler implements ICommunicationLayerListener, SunSpotListener {
	
	private Hashtable activeStateMachines;
	private SunSpotApplication app;
	private Hashtable eventQueues;
	private Hashtable timerHandlers;
	private int state;
	private int nOfConnections;
	private int maxConnections = 5;
	
	public static final int idle = 0; //no events being processed by any state machine
	public static final int busy = 0; //a state machine is processing an event
	
	
	
	public Scheduler() {
		state = idle;
		eventQueues = new Hashtable();
		timerHandlers = new Hashtable();
		activeStateMachines = new Hashtable();
		nOfConnections = 0;
	}
	
	public Scheduler(SunSpotApplication app) {
		this();
		this.app = app;
	}	

	public synchronized void handleMessage(Message message) {
		Event event = generateEvent(message);
		if (message.getContent().equals(Message.CanYouDisplayMyReadings)) {
			ReceiveStateMachine receiveStateMachine = new ReceiveStateMachine(message.getSender(), this, app);
			nOfConnections++;
			EventQueue eventQueue = new EventQueue(receiveStateMachine.getId());
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
	public void actionReceived(String action) {
		if (action.equals(SunSpotApplication.button1)) {
			
			TestStateMachine test = new TestStateMachine(""+System.currentTimeMillis(), this, app);
			activeStateMachines.put(test.getId(), test);
			EventQueue eventQueue = new EventQueue(test.getId());
			id = test.getId();
			Event event = new Event(Event.testOn, test.getId(), System.currentTimeMillis());
			eventQueue.addEvent(event);
			eventQueues.put(test.getId(), eventQueue);
			TimerHandler handler = new TimerHandler(test.getId());
			timerHandlers.put(test.getId(), handler);
//			SendingStateMachine sendingStateMachine = new SendingStateMachine(""+System.currentTimeMillis(), this, app);
//			EventQueue eventQueue = new EventQueue(sendingStateMachine.getId());
//			Event event = generateEvent(action, sendingStateMachine.getId());
//			eventQueue.addEvent(event);
//			eventQueues.put(sendingStateMachine.getId(), eventQueue);
//			TimerHandler handler = new TimerHandler(sendingStateMachine.getId());
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
	
	private Event generateEvent(String action, String stateMachineId) {
		if (action.equals(SunSpotApplication.button1)) {
			return new Event(Event.broadcast, stateMachineId, System.currentTimeMillis());
		}
		else if (action.equals(SunSpotApplication.button2)) {
			return new Event(Event.disconnect, stateMachineId, System.currentTimeMillis());
		}
		return new Event(0, "", System.currentTimeMillis());
	}
	
	public synchronized void saveEvent(Event event, String stateMachineId) {
		EventQueue queue = (EventQueue)eventQueues.get(stateMachineId);
		queue.saveEvent(event);
		
	}
	
	

	public void inputReceived(Message message) {
		handleMessage(message);
	}
	
	public synchronized void getNextEvent() {
		
		state = busy;
		EventQueue currentQueue = null;
		TimerHandler currentHandler = null;
		Event currentEvent;
		long nextTime = Long.MAX_VALUE;
		
		//timeout events have priority
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
			return;
		}
		
		//checking for other events
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
			
			System.out.println(activeStateMachines.isEmpty());
			StateMachine currentMachine = (StateMachine)activeStateMachines.get(currentEvent.getStateMachineId());
			System.out.println(currentMachine.getId());
			
			currentMachine.assignEvent(currentEvent);
			return;
		}
		//no events in any queue
		state = idle;
		return;
	}
	
	public synchronized void addTimer(String stateMachineId, Event event, long time) {
		TimerHandler handler = (TimerHandler)timerHandlers.get(stateMachineId);
		handler.startNewTimer(time, event);
	}

	
	
}

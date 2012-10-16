package no.ntnu.item.ttm4160.sunspot.runtime;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.communication.*;
import no.ntnu.item.ttm4160.sunspot.runtime.*;
import no.ntnu.item.ttm4160.sunspot.utils.*;

public class Scheduler extends Thread implements ICommunicationLayerListener{
	
	ICommunicationLayerListener listener;
	
	private Hashtable activeStateMachines;
	private SunSpotApplication app;
	private Hashtable eventQueues;
	private Hashtable timerHandlers;
	private int state;
	
	public static final int idle = 0; //no events being processed by any state machine
	public static final int busy = 0; //a state machine is processing an event
	
	public Scheduler() {
		state = idle;
		listener = new ICommunicationLayerListener() {
			public void inputReceived(Message msg) {
//				handleMessage(msg);
//				saveEvent(event, app.MAC);
			}
		};
		eventQueues = new Hashtable();
		timerHandlers = new Hashtable();
		activeStateMachines = new Hashtable();
	}
	
	public Scheduler(SunSpotApplication app) {
		this();
		this.app = app;
	}	
	
	public ICommunicationLayerListener getListener() {
		return this.listener;
	}
	
	public synchronized void handleMessage(Message message) {
		if(message.getReceiver().equals(Message.BROADCAST_ADDRESS)) {
			ReceiveStateMachine receiveStateMachine = new ReceiveStateMachine(message.getSender(), this, app);
			EventQueue eventQueue = new EventQueue(receiveStateMachine.getId());
			Event event = generateEvent(message);
			eventQueue.addEvent(event);
			eventQueues.put(eventQueue.getStateMachineId(), eventQueue);
			
		}
		if (state == idle) {
			getNextEvent();
		}
	}
	
	private Event generateEvent(Message message) {
		if(message.getReceiver().equals(Message.BROADCAST_ADDRESS)) {
			return new Event(Event.broadcast, message.getSender(), message.getSenderMAC(), System.currentTimeMillis());
		}
		return new Event(0, "", System.currentTimeMillis());
	}
	
	public synchronized void saveEvent(Event event, String stateMachineId) {
		EventQueue queue = (EventQueue)eventQueues.get(stateMachineId);
		queue.saveEvent(event);
		
	}
	
	

	public void inputReceived(Message message) {
		// TODO Auto-generated method stub
		
	}
	
	public synchronized void getNextEvent() {
		EventQueue currentQueue = null;
		TimerHandler currentHandler = null;
		Event currentEvent;
		long nextTime = Long.MAX_VALUE;
		
		//timeout events have priority
		for (Enumeration e = timerHandlers.keys(); e.hasMoreElements() ;) {
			TimerHandler handler = (TimerHandler)timerHandlers.get(e.nextElement());
			long time = handler.checkTimeoutQueue();
			if (time != 0 && time < nextTime) {
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
			if (time != 0 && time < nextTime) {
				nextTime = time;
				currentQueue = queue;
			}
		}
		if (nextTime < Long.MAX_VALUE) {
			currentEvent = currentQueue.getNextEvent();
			StateMachine currentMachine = (StateMachine)activeStateMachines.get(currentEvent.getStateMachineId());
			currentMachine.assignEvent(currentEvent);
			return;
		}
		//no events in any queue
		return;
	}
	
}

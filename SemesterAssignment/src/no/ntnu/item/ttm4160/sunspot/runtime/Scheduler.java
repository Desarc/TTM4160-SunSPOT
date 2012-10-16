package no.ntnu.item.ttm4160.sunspot.runtime;

import java.util.Hashtable;
import java.util.Vector;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.communication.*;
import no.ntnu.item.ttm4160.sunspot.runtime.*;
import no.ntnu.item.ttm4160.sunspot.utils.*;

public class Scheduler extends Thread implements ICommunicationLayerListener{
	
	ICommunicationLayerListener listener;
	
	private Vector activeBroadcast;
	private Vector activeReceive;
	private Vector activeReadings;
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
				handleMessage(msg);
				saveEvent(event, app.MAC);
			}
		};
		eventQueues = new Hashtable();
		timerHandlers = new Hashtable();
		activeReceive = new Vector();
		activeReadings = new Vector();
		activeBroadcast = new Vector();
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
			ReceiveStateMachine receiveStateMachine = new ReceiveStateMachine(this, app);
			EventQueue eventQueue = new EventQueue(message.getSender());
			Event event = generateEvent(message);
			eventQueue.addEvent(event);
			eventQueues.put(eventQueue.getStateMachineId(), eventQueue);
			if (state == idle) {
				getNextEvent();
			}
		}
	}
	
	private Event generateEvent(Message message) {
		if(message.getReceiver().equals(Message.BROADCAST_ADDRESS)) {
			return new Event(Event.broadcast, message.getReceiver(), message.getSenderMAC());
		}
		return new Event(0, "");
	}
	
	public synchronized void saveEvent(Event event, String stateMachineId) {
		EventQueue queue = (EventQueue)eventQueues.get(stateMachineId);
		queue.saveEvent(event);
		
	}
	
	

	public void inputReceived(Message msg) {
		// TODO Auto-generated method stub
		
	}
	
	public synchronized void getNextEvent() {
		
	}
	
}

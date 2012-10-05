package no.ntnu.item.ttm4160.sunspot.runtime;

import java.util.Hashtable;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.communication.*;
import no.ntnu.item.ttm4160.sunspot.runtime.*;
import no.ntnu.item.ttm4160.sunspot.utils.*;

public class Scheduler{
	
	ICommunicationLayerListener listener;
	EventQueue eventQueue;
	Event event;
	TimerHandler timerHandler;
	StateMachine stateMachine;
	BroadcastStateMachine broadcastStateMachine;
	ReceiveStateMachine receiveStateMachine;
	SunSpotApplication app;
	Hashtable eventQueues;
	
	
	public Scheduler() {
		listener = new ICommunicationLayerListener() {
			public void inputReceived(Message msg) {
				handleMessage(msg);
				saveEvent(event, app.MAC);
			}
		};
		eventQueues = new Hashtable();
	}
	
	public Scheduler(SunSpotApplication app) {
		this.app = app;
	}
	
	public ICommunicationLayerListener getListener() {
		return this.listener;
	}
	
	public void handleMessage(Message msg) {
		if(msg.getReceiver().equals(Message.BROADCAST_ADDRESS)) {
			eventQueue = new EventQueue(msg.getSenderMAC());
			eventQueues.put(msg.getSenderMAC(), eventQueue);
			receiveStateMachine = new ReceiveStateMachine(this, app);
			
			receiveStateMachine.start();
		}
	}
	
	public void saveEvent(Event event, String MAC) {
		eventQueue.saveEvent(event);
		
	}
	
}

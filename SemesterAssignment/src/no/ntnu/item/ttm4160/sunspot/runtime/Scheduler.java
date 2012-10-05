package no.ntnu.item.ttm4160.sunspot.runtime;

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
	
	
	public Scheduler() {
		listener = new ICommunicationLayerListener() {
			public void inputReceived(Message msg) {
				saveMessage(msg);
			}
		};
		eventQueue = new EventQueue();
		
		
	}
	
	public ICommunicationLayerListener getListener() {
		return this.listener;
	}
	
	public void saveMessage (Message msg) {
		eventQueue.saveMessage(msg);
		
	}
	
}

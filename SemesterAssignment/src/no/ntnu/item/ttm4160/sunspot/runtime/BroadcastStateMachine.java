package no.ntnu.item.ttm4160.sunspot.runtime;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.communication.Message;
import no.ntnu.item.ttm4160.sunspot.utils.Event;

public class BroadcastStateMachine extends StateMachine {
	
	
	public static final int idle = 0;
	
	public BroadcastStateMachine(Scheduler scheduler, SunSpotApplication app) {
		this.stateMachineId = this.toString();
		this.state = idle;
		this.scheduler = scheduler;
		this.app = app;
	}
	
	public void run() {
		if (currentEvent.getType() == Event.broadcast) {
			broadcast();
		}
		else {
			returnControlToScheduler();
		}
	}
	
	public void broadcast() {
		Message message = new Message(app.MAC+":"+stateMachineId, Message.BROADCAST_ADDRESS, Message.button1Pressed);
		app.com.sendRemoteMessage(message);
		returnControlToScheduler();
	}

}

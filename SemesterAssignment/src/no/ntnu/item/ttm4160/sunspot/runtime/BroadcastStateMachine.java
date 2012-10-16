package no.ntnu.item.ttm4160.sunspot.runtime;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.communication.Message;
import no.ntnu.item.ttm4160.sunspot.utils.Event;

public class BroadcastStateMachine extends StateMachine {
	
	
	public static final int idle = 0;
	
	public BroadcastStateMachine(String stateMachineId, Scheduler scheduler, SunSpotApplication app) {
		super(stateMachineId, scheduler, app);
		this.state = idle;
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
		Message message = new Message(Message.BROADCAST_ADDRESS+":"+stateMachineId, Message.BROADCAST_ADDRESS, Message.button1Pressed);
		app.com.sendRemoteMessage(message);
		returnControlToScheduler();
	}

}

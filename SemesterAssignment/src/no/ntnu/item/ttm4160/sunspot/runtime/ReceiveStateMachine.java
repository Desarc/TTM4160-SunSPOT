package no.ntnu.item.ttm4160.sunspot.runtime;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.utils.Event;

/**
 * State machine for receiving readings from another SunSPOT.
 *
 */
public class ReceiveStateMachine extends StateMachine {
	
	public static final int idle = 0;
	
	public ReceiveStateMachine(String stateMachineId, Scheduler scheduler, SunSpotApplication app) {
		super(stateMachineId, scheduler, app);
		this.state = idle;
	}
	
	public ReceiveStateMachine(String stateMachineId, Scheduler scheduler, SunSpotApplication app, int priority) {
		super(stateMachineId, scheduler, app, priority);
		this.state = idle;
	}
	
	
	public void run() {
		if (currentEvent.getType() == Event.receiveReadings) {
			displayReadings();
		}
		else {
			returnControlToScheduler(false);
		}
	}
	
	public void displayReadings() {
		app.showLightreadings(Integer.parseInt(currentEvent.getData()));
		returnControlToScheduler(false);
	}
	
	
	
	
}

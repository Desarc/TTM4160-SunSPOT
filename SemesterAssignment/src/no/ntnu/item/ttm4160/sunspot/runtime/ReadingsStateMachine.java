package no.ntnu.item.ttm4160.sunspot.runtime;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.utils.Event;

public class ReadingsStateMachine extends StateMachine {

	public static final int idle = 0;
	
	public ReadingsStateMachine(String stateMachineId, Scheduler scheduler, SunSpotApplication app) {
		super(stateMachineId, scheduler, app);
		this.state = idle;
	}
	
	public void assignEvent(Event event) {
		// TODO Auto-generated method stub
		
	}

}

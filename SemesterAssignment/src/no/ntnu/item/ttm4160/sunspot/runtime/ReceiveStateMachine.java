package no.ntnu.item.ttm4160.sunspot.runtime;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.utils.Event;

public class ReceiveStateMachine extends StateMachine {
	
	private Scheduler scheduler;
	private SunSpotApplication app;
	public static final int idle = 0;
	
	public ReceiveStateMachine(Scheduler scheduler, SunSpotApplication app) {
		this.stateMachineId = this.toString();
		this.state = idle;
		this.scheduler = scheduler;
		this.app = app;
	}
	
	public void assignEvent(Event event) {
		// TODO Auto-generated method stub
		
	}

}

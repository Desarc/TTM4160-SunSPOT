package no.ntnu.item.ttm4160.sunspot.runtime;

import no.ntnu.item.ttm4160.sunspot.utils.Event;

public interface StateMachine {
	
	public void assignEvent(Event e);
	
	public void returnControlToScheduler();
	

}

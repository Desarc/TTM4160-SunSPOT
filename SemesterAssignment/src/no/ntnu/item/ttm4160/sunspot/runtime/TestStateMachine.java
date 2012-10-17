package no.ntnu.item.ttm4160.sunspot.runtime;

import com.sun.spot.sensorboard.peripheral.LEDColor;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.utils.*;

public class TestStateMachine extends StateMachine {

	int state;
	
	public static final int idle = 0;
	public static final int on = 1;
	public static final int off = 2;
	
	public TestStateMachine(String stateMachineId, Scheduler scheduler, SunSpotApplication app) {
		super(stateMachineId, scheduler, app);
		state = idle;
	}
	
	public void run() {
		if (currentEvent.getType() == Event.testOn && (state == idle || state == off) ) {
			for (int i = 0; i < app.leds.length; i++) {
				app.leds[i].setColor(LEDColor.BLUE);
				app.leds[i].setOn();
			}
			state = on;
			scheduler.addTimer(stateMachineId, new Event(Event.testOff, stateMachineId, System.currentTimeMillis()), 1000);
		}
		else if (currentEvent.getType() == Event.testOff && state == on) {
			for (int i = 0; i < app.leds.length; i++) {
				app.leds[i].setOff();
			}
			state = off;
			scheduler.addTimer(stateMachineId, new Event(Event.testOn, stateMachineId, System.currentTimeMillis()), 1000);
		}
		
	}

}

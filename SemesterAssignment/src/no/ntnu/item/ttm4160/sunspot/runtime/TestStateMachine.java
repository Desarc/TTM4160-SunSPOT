package no.ntnu.item.ttm4160.sunspot.runtime;

import com.sun.spot.sensorboard.peripheral.LEDColor;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.utils.*;

/**
 * Simple state machine for testing the scheduler.
 *
 */
public class TestStateMachine extends StateMachine {

	private int state;
	private String activeTimer;
	
	public static final int idle = 0;
	public static final int on = 1;
	public static final int off = 2;
	
	public TestStateMachine(String stateMachineId, Scheduler scheduler, SunSpotApplication app) {
		super(stateMachineId, scheduler, app);
		state = idle;
	}
	
	public void run() {
		activeTimer = scheduler.addTimer(stateMachineId, 100);
		while (true) {
			if (currentEvent == null) {
				
			}
			else if (currentEvent.getType() == Event.testOn && (state == idle || state == off) ) {
				for (int i = 0; i < app.leds.length; i++) {
					app.leds[i].setColor(LEDColor.BLUE);
					app.leds[i].setOn();
				}
				state = on;
				scheduler.startTimer(stateMachineId, activeTimer, new Event(Event.testOff, stateMachineId, System.currentTimeMillis()));
				returnControlToScheduler(false);
			}
			else if (currentEvent.getType() == Event.testOff && state == on) {
				for (int i = 0; i < app.leds.length; i++) {
					app.leds[i].setOff();
				}
				state = off;
				scheduler.startTimer(stateMachineId, activeTimer, new Event(Event.testOn, stateMachineId, System.currentTimeMillis()));
				returnControlToScheduler(false);
			}
			try {
				sleep(Inf);
			} catch (InterruptedException e) {}
		}
	}
}

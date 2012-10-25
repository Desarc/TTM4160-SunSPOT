package no.ntnu.item.ttm4160.sunspot.runtime;

import com.sun.spot.sensorboard.peripheral.LEDColor;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.utils.*;

/**
 * Simple state machine for testing the scheduler.
 *
 */
public class TestStateMachine extends StateMachine {

	private String state;
	private LEDColor color;
	private String activeTimer;
	private long speed;
	private boolean slow;
	
	public static final String idle = "idle";
	public static final String on = "on";
	public static final String off = "off";
	
	public TestStateMachine(String stateMachineId, Scheduler scheduler, SunSpotApplication app, LEDColor color, long speed, boolean slow) {
		super(stateMachineId, scheduler, app);
		state = idle;
		this.color = color;
		this.speed = speed;
		this.slow = slow;
	}
	
	public void run() {
		activeTimer = scheduler.addTimer(stateMachineId, speed);
		while (true) {
			if (SunSpotApplication.output) {	
				System.out.println(Thread.currentThread());
			}
			if (currentEvent == null) {
				if (SunSpotApplication.output) {	
					System.out.println("No event.");
				}
			}
			else if (currentEvent.getType().equals(Event.testOn) && (state == idle || state == off) ) {
				for (int i = 0; i < app.leds.length; i++) {
					app.leds[i].setColor(color);
					app.leds[i].setOn();
				}
				state = on;
				Event off = new Event(Event.testOff, stateMachineId, System.currentTimeMillis());
				scheduler.startTimer(stateMachineId, activeTimer, off);
				long time = System.currentTimeMillis();
				while (System.currentTimeMillis() < time+200 && slow) {}
				currentEvent = null;
				returnControlToScheduler(false);
			}
			else if (currentEvent.getType() == Event.testOff && state == on) {
				for (int i = 0; i < app.leds.length; i++) {
					app.leds[i].setOff();
				}
				state = off;
				Event on = new Event(Event.testOn, stateMachineId, System.currentTimeMillis());
				scheduler.startTimer(stateMachineId, activeTimer, on);
				long time = System.currentTimeMillis();
				while (System.currentTimeMillis() < time+200 && slow) {}
				currentEvent = null;
				returnControlToScheduler(false);
			}
			try {
				sleep(Inf);
			} catch (InterruptedException e) {}
		}
	}
}

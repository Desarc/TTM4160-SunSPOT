package no.ntnu.item.ttm4160.sunspot.utils;

import java.util.Timer;
import java.util.TimerTask;

import no.ntnu.item.ttm4160.sunspot.runtime.TimerHandler;

/**
 * General timer class for state machines. Contains an event to be processed at timeout, and a reference to the {@link TimerHandler}
 * (and implicitly {@link StateMachine}) it belongs to, in addition to the time it should run.
 *
 */
public class SPOTTimer extends Timer {
	
	private Event event;
	private long time;
	private TimerHandler handler;
	
	/**
	 * 
	 * @param time {@link long} Time before timeout.
	 * @param event The {@link Event} to be processed at timeout.
	 * @param handler The {@link TimerHandler} to notify at timeout.
	 */
	
	public SPOTTimer(long time, Event event, TimerHandler handler) {
		this.event = event;
		this.time = time;
		this.handler = handler;
	}
	
	/**
	 * Class specifying what to do in the case of a timeout.
	 * @author Øyvin
	 *
	 */
	class Timeout extends TimerTask {
		SPOTTimer timer;
		
		public Timeout(SPOTTimer timer) {
			this.timer = timer;
		}
		
		/**
		 * Notifies the handler that this timer has expired.
		 */
		public void run() {
			handler.timeout(timer);
		}
		
	}
	
	/**
	 * Starts the timer.
	 */
	public void start() {
		schedule(new Timeout(this), time);
	}

	public Event getEvent() {
		return event;
	}
}

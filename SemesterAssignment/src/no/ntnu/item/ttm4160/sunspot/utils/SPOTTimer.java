package no.ntnu.item.ttm4160.sunspot.utils;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.runtime.TimerHandler;

/**
 * General timer class for state machines. Contains an event to be processed at timeout, and a reference to the {@link TimerHandler}
 * (and implicitly {@link StateMachine}) it belongs to, in addition to the time it should run.
 *
 */
public class SPOTTimer extends Thread {
	
	private String timerId;
	private Event event;
	private long time;
	private TimerHandler handler;
	private boolean running;
	private boolean active;
	
	public static final long Inf = Integer.MAX_VALUE;
	
	/**
	 * 
	 * @param time {@link long} Time before timeout.
	 * @param event The {@link Event} to be processed at timeout.
	 * @param handler The {@link TimerHandler} to notify at timeout.
	 */
	
	public SPOTTimer(long time, TimerHandler handler) {
		this.timerId = ""+System.currentTimeMillis();
		this.time = time;
		this.handler = handler;
		running = false;
		active = true;
	}
	
	/**
	 * Starts this {@link SPOTTimer}, and returns its {@link Thread} instance.
	 * @return
	 */
	public Thread startThread() {
		Thread timerThread = new Thread(this);
		timerThread.start();
		return timerThread;
	}
	
	/**
	 * Timer sleeps 'infinitely' by default, and starts a timed sleep if interrupted.
	 * If the timer wakes on its own, it sends a timeout to its {@link TimerHandler}.
	 * If the timer is interrupted while 'running' is true, it resets.
	 * IF the timer is interrupted while 'running' is false, the timer stops and goes back
	 * to 'infinite' sleep.
	 */
	public void run() {
		while (active) {
			if (SunSpotApplication.output) {	
				System.out.println("Timer thread: "+Thread.currentThread());
			}
			try {
				sleep(Inf);					//sleep by default
			} catch (InterruptedException e) {
				running = true;				//if interrupted, start timer
				while (running && active) {
					try {
						sleep(time);		//sleep for 'time' milliseconds (timer)
					} catch (InterruptedException e1) {
						continue;			//if interrupted, reset timer
					}
					running = false;
					timeout();				//if not interrupted, timeout and go back to sleep
				}	
			}
		}
	}
	
	public void timeout() {
		handler.timeout(this);
	}

	public Event getEvent() {
		return event;
	}
	
	public void setEvent(Event event) {
		this.event = event;
	}
	
	public String getTimerId() {
		return timerId;
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void deactivate() {
		active = false;
	}
	
	public void stop() {
		running = false;
	}
}

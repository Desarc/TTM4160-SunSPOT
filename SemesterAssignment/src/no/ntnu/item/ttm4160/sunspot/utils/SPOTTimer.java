package no.ntnu.item.ttm4160.sunspot.utils;

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
	
	public Thread startThread() {
		Thread timerThread = new Thread(this);
		timerThread.start();
		return timerThread;
	}
	
	public void run() {
		while (active) {
			System.out.println("Timer thread: "+Thread.currentThread());
			try {
				sleep(Inf);					//sleep by default
			} catch (InterruptedException e) {
				running = true;				//if interrupted, start timer
				while (running && active) {
					//System.out.println(Thread.currentThread());
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

	public synchronized Event getEvent() {
		return event;
	}
	
	public synchronized void setEvent(Event event) {
		this.event = event;
	}
	
	public String getTimerId() {
		return timerId;
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public synchronized void deactivate() {
		active = false;
	}
}

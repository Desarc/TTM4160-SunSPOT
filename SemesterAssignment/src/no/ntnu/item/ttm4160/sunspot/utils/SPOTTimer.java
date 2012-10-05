package no.ntnu.item.ttm4160.sunspot.utils;

import java.util.Timer;
import java.util.TimerTask;

import no.ntnu.item.ttm4160.sunspot.runtime.TimerHandler;

public class SPOTTimer extends Timer {
	
	private Event event;
	private long time;
	private TimerHandler handler;
	

	public SPOTTimer(long time, Event event, TimerHandler handler) {
		this.event = event;
		this.time = time;
		this.handler = handler;
	}
	
	class Timeout extends TimerTask {
		SPOTTimer timer;
		
		public Timeout(SPOTTimer timer) {
			this.timer = timer;
		}
		public void run() {
			handler.timeout(timer);
		}
		
	}

	public void start() {
		schedule(new Timeout(this), time);
	}

	public Event getEvent() {
		return event;
	}

}

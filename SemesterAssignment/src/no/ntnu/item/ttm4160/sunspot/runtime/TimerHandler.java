package no.ntnu.item.ttm4160.sunspot.runtime;

import java.util.Vector;

import no.ntnu.item.ttm4160.sunspot.utils.Event;
import no.ntnu.item.ttm4160.sunspot.utils.SPOTTimer;

import com.sun.spot.util.Queue;

public class TimerHandler extends Thread {
	
	private Queue timeoutEventQueue;
	private Vector activeTimers;
	private String stateMachineId;
	private Event nextEvent;
	
	public TimerHandler(String stateMachineId) {
		this.stateMachineId = stateMachineId;
		activeTimers = new Vector();
		timeoutEventQueue = new Queue();
		
	}
	
	public synchronized void startNewTimer(long time, Event event) {
		SPOTTimer timer = new SPOTTimer(time, event, this);
		activeTimers.addElement(timer);
		timer.start();
	}
	
	public synchronized void timeout(SPOTTimer timer) {
		activeTimers.removeElement(timer);
		Event timeout = timer.getEvent();
		timeout.setTimeStamp(System.currentTimeMillis());
		if (nextEvent == null) {
			nextEvent = timeout;
		}
		else {
			timeoutEventQueue.put(timeout);
		}
	}
	
	public synchronized void killAllTimers() {
		for (int i = 0; i < activeTimers.size(); i++) {
			Object element = activeTimers.elementAt(i);
			((SPOTTimer) element).cancel();
		}
		activeTimers = new Vector();
		timeoutEventQueue = new Queue();
	}
	
	public synchronized long checkTimeoutQueue() {
		if (nextEvent == null) {
			return 0;
		}
		return nextEvent.getTimeStamp();
	}

	public synchronized Event getNextEvent() {
		Event next;
		if (timeoutEventQueue.size() > 0) {
			next = nextEvent;
			nextEvent = (Event)timeoutEventQueue.get();
		}
		else {
			next = nextEvent;
			nextEvent = null;
		}
		return next;
	}
	
	public String getStateMachineId() {
		return stateMachineId;
	}
}

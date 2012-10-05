package no.ntnu.item.ttm4160.sunspot.runtime;

import java.util.Vector;

import no.ntnu.item.ttm4160.sunspot.utils.Event;
import no.ntnu.item.ttm4160.sunspot.utils.SPOTTimer;

import com.sun.spot.util.Queue;

public class TimerHandler {
	
	private Queue timeoutEventQueue;
	private Vector activeTimers;
	private String stateMachineId;
	
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
		timeoutEventQueue.put(timer.getEvent());
	}
	
	public synchronized void killAllTimers() {
		for (int i = 0; i < activeTimers.size(); i++) {
			Object element = activeTimers.elementAt(i);
			((SPOTTimer) element).cancel();
		}
		activeTimers = new Vector();
		timeoutEventQueue = new Queue();
	}
	
	public synchronized Event checkTimeoutQueue() {
		if (timeoutEventQueue.size() == 0) {
			return new Event(Event.noEvent, stateMachineId);
		}
		else {
			return (Event)timeoutEventQueue.get();
		}
		
	}

	
}

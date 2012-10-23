package no.ntnu.item.ttm4160.sunspot.runtime;

import com.sun.spot.sensorboard.peripheral.LEDColor;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.utils.Event;

/**
 * State machine for receiving readings from another SunSPOT.
 *
 */
public class ReceiveStateMachine extends StateMachine {
	
	public static final int free = 0;
	public static final int busy = 1;
	public static final int wait_approved = 2;
	
	public ReceiveStateMachine(String stateMachineId, Scheduler scheduler, SunSpotApplication app) {
		super(stateMachineId, scheduler, app);
		this.state = free;
	}
	
	public ReceiveStateMachine(String stateMachineId, Scheduler scheduler, SunSpotApplication app, int priority) {
		super(stateMachineId, scheduler, app, priority);
		this.state = free;
	}
	
	
	public void run() {
		if (currentEvent.getType() == Event.broadcast) {
			System.out.println("Broadcast received!");
			if (state == free) {
				sendBroadcastResponse();
				state = wait_approved;
				returnControlToScheduler(false);
			}
			else if (state == wait_approved) {
				System.out.println("Broadcast received, but already waiting. Saving for later.");
				scheduler.saveEvent(currentEvent, stateMachineId);
				state = wait_approved;
				returnControlToScheduler(false);
			}
		}
		else if (currentEvent.getType() == Event.connectionApproved) {
			System.out.println("Connection approved!");
			if (state == wait_approved) {
				scheduler.addTimer(stateMachineId, new Event(Event.giveUp, stateMachineId, System.currentTimeMillis()), 5000);
				state = busy;
				returnControlToScheduler(false);
			}
		}
		else if (currentEvent.getType() == Event.connectionDenied) {
			System.out.println("Connection denied!");
			if (state == wait_approved) {
				state = free;
				returnControlToScheduler(false);
			}
		}
		else if (currentEvent.getType() == Event.senderDisconnect) {
			System.out.println("Sender disconnected.");
			if (state == busy) {
				blinkLEDs();
				state = free;
				returnControlToScheduler(true);
			}
		}
		else if (currentEvent.getType() == Event.disconnect) {
			System.out.println("Disconnecting...");
			if (state == busy) {
				sendDisconnect();
				blinkLEDs();
				state = free;
				returnControlToScheduler(true);
			}
		}
		else if (currentEvent.getType() == Event.receiveReadings) {
			System.out.println("Readings received.");
			if (state == busy) {
				displayReadings();
				resetGiveUpTimer();
				state = busy;
				returnControlToScheduler(false);
			}
			else {
				System.out.println("Readings received, but wrong context, disonnecting from this sender...");
				sendDisconnect();
				returnControlToScheduler(false);
			}
		}
		else if (currentEvent.getType() == Event.giveUp) {
			System.out.println("Timeout! Assuming connection has died.");
			if (state == busy) {
				state = free;
				returnControlToScheduler(false);
			}
		}
		else {
			System.out.println("Event type not recognized.");
			returnControlToScheduler(false);
		}
	}
	
	private void resetGiveUpTimer() {
		// TODO Auto-generated method stub
		
	}

	private void sendDisconnect() {
		// TODO Auto-generated method stub
		
	}

	private void blinkLEDs() {
		app.blinkLEDs(LEDColor.RED, 200, 3);
	}

	private void sendBroadcastResponse() {
		// TODO Auto-generated method stub
		
	}

	private void displayReadings() {
		app.showLightreadings(Integer.parseInt(currentEvent.getData()));
		returnControlToScheduler(false);
	}
	
	
	
	
}

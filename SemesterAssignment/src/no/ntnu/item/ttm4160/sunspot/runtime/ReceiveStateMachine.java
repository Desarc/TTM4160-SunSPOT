package no.ntnu.item.ttm4160.sunspot.runtime;

import com.sun.spot.sensorboard.peripheral.LEDColor;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.communication.Message;
import no.ntnu.item.ttm4160.sunspot.utils.Event;

/**
 * State machine for receiving readings from another SunSPOT.
 *
 */
public class ReceiveStateMachine extends StateMachine {
	
	public static final int free = 0;
	public static final int busy = 1;
	public static final int wait_approved = 2;
	
	private String sender;
	
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
			if (state == free) {
				System.out.println("\nBroadcast received!\n");
				sendBroadcastResponse();
				state = wait_approved;
				returnControlToScheduler(false);
			}
			else if (state == wait_approved) {
				System.out.println("\nBroadcast received, but already waiting. Saving for later.\n");
				scheduler.saveEvent(currentEvent, stateMachineId);
				state = wait_approved;
				returnControlToScheduler(false);
			}
		}
		else if (currentEvent.getType() == Event.connectionApproved) {
			if (state == wait_approved) {
				System.out.println("\nConnection approved!\n");
				scheduler.addTimer(stateMachineId, new Event(Event.giveUp, stateMachineId, System.currentTimeMillis()), 5000);
				state = busy;
				returnControlToScheduler(false);
			}
		}
		else if (currentEvent.getType() == Event.connectionDenied) {
			if (state == wait_approved) {
				System.out.println("\nConnection denied!\n");
				state = free;
				returnControlToScheduler(false);
			}
		}
		else if (currentEvent.getType() == Event.senderDisconnect) {
			if (state == busy) {
				System.out.println("\nSender disconnected.\n");
				blinkLEDs();
				state = free;
				returnControlToScheduler(true);
			}
		}
		else if (currentEvent.getType() == Event.disconnect) {
			if (state == busy) {
				System.out.println("\nDisconnecting...\n");
				sendDisconnect();
				blinkLEDs();
				state = free;
				returnControlToScheduler(true);
			}
		}
		else if (currentEvent.getType() == Event.receiveReadings) {
			if (state == busy) {
				System.out.println("\nReadings received.\n");
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
			if (state == busy) {
				System.out.println("\nTimeout! Assuming connection has died.\n");
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
		Message disconnect = new Message(app.MAC+":"+stateMachineId, sender, Message.ReceiverDisconnect);
		app.com.sendRemoteMessage(disconnect);
	}

	private void blinkLEDs() {
		app.blinkLEDsDynamic(LEDColor.RED, 200, 0, 3);
	}

	private void sendBroadcastResponse() {
		sender = currentEvent.getStateMachineId();
		System.out.println(sender);
		Message response = new Message(app.MAC+":"+stateMachineId, sender, Message.ICanDisplayReadings);
		app.com.sendRemoteMessage(response);
	}

	private void displayReadings() {
		app.showLightreadings(Integer.parseInt(currentEvent.getData()));
	}
	
	
	
	
}

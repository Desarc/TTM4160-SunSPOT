package no.ntnu.item.ttm4160.sunspot.runtime;

import java.io.IOException;

import com.sun.spot.sensorboard.peripheral.LEDColor;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.communication.Message;
import no.ntnu.item.ttm4160.sunspot.utils.Event;
/**
 * State machine for broadcasting and sending readings to another SunSPOT.
 *
 */
public class SendingStateMachine extends StateMachine {

	public static final int ready = 0;
	public static final int wait_response = 1;
	public static final int sending = 2;
	
	
	private int readings;
	private String receiver;
	
	public SendingStateMachine(String stateMachineId, Scheduler scheduler, SunSpotApplication app) {
		super(stateMachineId, scheduler, app);
		this.state = ready;
	}
	
	public SendingStateMachine(String stateMachineId, Scheduler scheduler, SunSpotApplication app, int priority) {
		super(stateMachineId, scheduler, app, priority);
		this.state = ready;
	}
	
	
	public void run() {
		if (currentEvent.getType() == Event.broadcast) {
			
			if (state == ready) {
				System.out.println("------------------------------------------");
				System.out.println("\nBroadcasting request...\n");
				System.out.println("------------------------------------------");
				scheduler.addTimer(stateMachineId, new Event(Event.giveUp, stateMachineId, System.currentTimeMillis()), 1000);
				sendBroadcast();
				state = wait_response;
				returnControlToScheduler(false);
			}
		}
		else if (currentEvent.getType() == Event.broadcast_response) {
			if (state == wait_response) {
				System.out.println("------------------------------------------");
				System.out.println("\nReceived broadcast response, approving...\n");
				System.out.println("------------------------------------------");
				scheduler.addTimer(stateMachineId, new Event(Event.sendReadings, stateMachineId, System.currentTimeMillis()), 400);
				sendApproved();
				state = sending;
				returnControlToScheduler(false);
			}
			else {
				System.out.println("------------------------------------------");
				System.out.println("\nReceived broadcast response in wrong context, denying...\n");
				System.out.println("------------------------------------------");
				sendDenied();
				returnControlToScheduler(false);
			}
		}
		else if (currentEvent.getType() == Event.sendReadings) {
			
			if (state == sending) {
				System.out.println("------------------------------------------");
				System.out.println("\nSending light readings...\n");
				System.out.println("------------------------------------------");
				scheduler.addTimer(stateMachineId, new Event(Event.sendReadings, stateMachineId, System.currentTimeMillis()), 400);
				sendReadings();
				state = sending;
				returnControlToScheduler(false);
			}
		}
		else if (currentEvent.getType() == Event.giveUp) {
			
			if (state == wait_response) {
				System.out.println("------------------------------------------");
				System.out.println("\nNo responses received, giving up.\n");
				System.out.println("------------------------------------------");
				blinkLEDs();
				state = ready;
				returnControlToScheduler(false);
			}
		}
		else if (currentEvent.getType() == Event.disconnect) {
			
			if (state == sending) {
				System.out.println("------------------------------------------");
				System.out.println("\nDisconnecting...\n");
				System.out.println("------------------------------------------");
				sendDisconnect();
				blinkLEDs();
				state = ready;
				returnControlToScheduler(false);
			}
		}
		else if (currentEvent.getType() == Event.receiverDisconnect) {
			
			if (state == sending) {
				System.out.println("------------------------------------------");
				System.out.println("\nReceiver disconnected.\n");
				System.out.println("------------------------------------------");
				blinkLEDs();
				state = ready;
				returnControlToScheduler(false);
			}
		}
		else {
			returnControlToScheduler(false);
		}
	}
	
	private void sendDenied() {
		Message denied = new Message(app.MAC+":"+stateMachineId, currentEvent.getData() , Message.Denied);
		app.com.sendRemoteMessage(denied);
	}

	private void sendDisconnect() {
		Message disconnect = new Message(app.MAC+":"+stateMachineId, receiver , Message.SenderDisconnect);
		app.com.sendRemoteMessage(disconnect);
	}

	private void sendApproved() {
		receiver = currentEvent.getData();
		Message approved = new Message(app.MAC+":"+stateMachineId, receiver , Message.Approved);
		app.com.sendRemoteMessage(approved);
	}

	private void blinkLEDs() {
		app.blinkLEDsDynamic(LEDColor.RED, 200, 0, 3);
	}

	private void sendBroadcast() {
		Message message = new Message(app.MAC+":"+stateMachineId, Message.BROADCAST_ADDRESS, Message.CanYouDisplayMyReadings);
		app.com.sendRemoteMessage(message);
	}
	
	private void sendReadings() {
		Message message = new Message(app.MAC+":"+stateMachineId, receiver, Message.Reading+registerReadings());
		app.com.sendRemoteMessage(message);
	}
	
	private String registerReadings() {
		readings = -1;
		try {
			readings = app.lightSensor.getAverageValue();
		} catch (IOException e) {
			System.out.println("Failed to read from box");
			e.printStackTrace();
		}
		return Integer.toString(readings);
	}
	
}

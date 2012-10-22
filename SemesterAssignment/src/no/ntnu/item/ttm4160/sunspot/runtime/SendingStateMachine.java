package no.ntnu.item.ttm4160.sunspot.runtime;

import java.io.IOException;

import com.sun.spot.sensorboard.peripheral.LEDColor;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.communication.Message;
import no.ntnu.item.ttm4160.sunspot.utils.Event;
import no.ntnu.item.ttm4160.sunspot.utils.SPOTTimer;

/**
 * State machine for broadcasting and sending readings to another SunSPOT.
 *
 */
public class SendingStateMachine extends StateMachine {

	public static final int ready = 0;
	public static final int wait_response = 1;
	public static final int sending = 2;
	
	
	private int readings;
	
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
			System.out.println("Broadcasting request...");
			if (state == ready) {
				scheduler.addTimer(stateMachineId, new Event(Event.giveUp, stateMachineId, System.currentTimeMillis()), 500);
				sendBroadcast();
				state = wait_response;
				returnControlToScheduler(false);
			}
		}
		else if (currentEvent.getType() == Event.broadcast_response) {
			if (state == wait_response) {
				System.out.println("Received broadcast response, approving...");
				scheduler.addTimer(stateMachineId, new Event(Event.sendReadings, stateMachineId, System.currentTimeMillis()), 100);
				sendApproved();
				state = sending;
				returnControlToScheduler(false);
			}
			else {
				System.out.println("Received broadcast response in wrong context, denying...");
				sendDenied();
				returnControlToScheduler(false);
			}
		}
		else if (currentEvent.getType() == Event.sendReadings) {
			System.out.println("Sending light readings...");
			if (state == sending) {
				scheduler.addTimer(stateMachineId, new Event(Event.sendReadings, stateMachineId, System.currentTimeMillis()), 100);
				sendReadings();
				state = sending;
				returnControlToScheduler(false);
			}
		}
		else if (currentEvent.getType() == Event.giveUp) {
			System.out.println("No responses received, giving up.");
			if (state == wait_response) {
				blinkLEDs();
				state = ready;
				returnControlToScheduler(false);
			}
		}
		else if (currentEvent.getType() == Event.disconnect) {
			System.out.println("Disconnecting...");
			if (state == sending) {
				sendDisconnect();
				blinkLEDs();
				state = ready;
				returnControlToScheduler(false);
			}
		}
		else if (currentEvent.getType() == Event.receiverDisconnect) {
			System.out.println("Receiver disconnected.");
			if (state == sending) {
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
		// TODO Auto-generated method stub
		
	}

	private void sendDisconnect() {
		// TODO Auto-generated method stub
		
	}

	private void sendApproved() {
		// TODO Auto-generated method stub
		
	}

	private void blinkLEDs() {
		app.blinkLEDs(LEDColor.RED);
	}

	private void sendBroadcast() {
		Message message = new Message(app.MAC+":"+stateMachineId, Message.BROADCAST_ADDRESS, Message.CanYouDisplayMyReadings);
		app.com.sendRemoteMessage(message);
		returnControlToScheduler(false);
	}
	
	private void sendReadings() {
		Message message = new Message(app.MAC+":"+stateMachineId, Message.DATAGRAM_PORT, Message.Reading+registerReadings());
		app.com.sendRemoteMessage(message);
		returnControlToScheduler(false);
		
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

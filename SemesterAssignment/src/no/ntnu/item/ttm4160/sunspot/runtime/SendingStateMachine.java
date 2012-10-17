package no.ntnu.item.ttm4160.sunspot.runtime;

import java.io.IOException;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.communication.Message;
import no.ntnu.item.ttm4160.sunspot.utils.Event;

/**
 * State machine for broadcasting and sending readings to another SunSPOT.
 *
 */
public class SendingStateMachine extends StateMachine {

	public static final int idle = 0;
	private int readings;
	
	public SendingStateMachine(String stateMachineId, Scheduler scheduler, SunSpotApplication app) {
		super(stateMachineId, scheduler, app);
		this.state = idle;
	}
	
	public SendingStateMachine(String stateMachineId, Scheduler scheduler, SunSpotApplication app, int priority) {
		super(stateMachineId, scheduler, app, priority);
		this.state = idle;
	}
	
	
	public void run() {
		if (currentEvent.getType() == Event.broadcast) {
			broadcast();
		}
		else if (currentEvent.getType() == Event.sendReadings) {
			sendReadings();
		}
		else {
			returnControlToScheduler(false);
		}
	}
	
	public void broadcast() {
		Message message = new Message(app.MAC+":"+stateMachineId, Message.BROADCAST_ADDRESS, Message.CanYouDisplayMyReadings);
		app.com.sendRemoteMessage(message);
		returnControlToScheduler(false);
	}
	
	public void sendReadings() {
		Message message = new Message(app.MAC+":"+stateMachineId, Message.DATAGRAM_PORT, Message.Reading+registerReadings());
		app.com.sendRemoteMessage(message);
		returnControlToScheduler(false);
	}
	
	public String registerReadings() {
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

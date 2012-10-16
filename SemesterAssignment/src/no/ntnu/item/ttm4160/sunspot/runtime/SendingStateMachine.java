package no.ntnu.item.ttm4160.sunspot.runtime;

import java.io.IOException;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.communication.Message;
import no.ntnu.item.ttm4160.sunspot.utils.Event;

public class SendingStateMachine extends StateMachine {

	public static final int idle = 0;
	private int readings;
	
	public SendingStateMachine(String stateMachineId, Scheduler scheduler, SunSpotApplication app) {
		super(stateMachineId, scheduler, app);
		this.state = idle;
	}
	
	public void sendReadings() {
		Message message = new Message(app.MAC+":"+registerReadings(), Message.DATAGRAM_PORT, Message.CanYouDisplayMyReadings);
		app.com.sendRemoteMessage(message);
		returnControlToScheduler();
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

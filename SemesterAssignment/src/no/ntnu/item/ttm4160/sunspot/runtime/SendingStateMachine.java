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

	public static final String ready = "ready";
	public static final String wait_response = "wait_response";
	public static final String sending = "sending";
	
	
	private int readings;
	private String receiverId;
	private String giveUpTimerId;
	private String sendingTimerId;
	private String broadcastTimerId;
	private long frequency = 200;
	private long giveUpTime = 5000;
	private long broadcastTime = 1000;
	
	public SendingStateMachine(String stateMachineId, Scheduler scheduler, TimerHandler timerHandler, SunSpotApplication app) {
		super(stateMachineId, scheduler, timerHandler, app);
		this.state = ready;
	}
	
	public SendingStateMachine(String stateMachineId, Scheduler scheduler, TimerHandler timerHandler, SunSpotApplication app, int priority) {
		super(stateMachineId, scheduler, timerHandler, app, priority);
		this.state = ready;
	}
	
	/**
	 * Sleeps until interrupted, and handles/consumees the current {@link Event} when interrupted according to
	 * state and event type.
	 */
	public void run() {
		while (active) {
			if (SunSpotApplication.output) {	
				System.out.println("Sending thread: "+Thread.currentThread());
			}
			if (currentEvent == null) {
				if (SunSpotApplication.output) {	
					System.out.println("Sending state machine interrupted without event");
				}
				returnControlToScheduler(false);
			}
			else if (currentEvent.getType() == Event.broadcast) {
				
				if (state == ready) {
					if (SunSpotApplication.output) {	
						System.out.println("------------------------------------------");
						System.out.println("\nBroadcasting request...\n");
						System.out.println("------------------------------------------");
					}
					Event giveUp = new Event(Event.broadcastGiveUp, stateMachineId, System.currentTimeMillis());
					broadcastTimerId = timerHandler.addNewTimer(broadcastTime);
					timerHandler.startTimer(broadcastTimerId, giveUp);
					sendBroadcast();
					state = wait_response;
					returnControlToScheduler(false);
				}
			}
			else if (currentEvent.getType() == Event.broadcast_response) {
				if (state == wait_response) {
					if (SunSpotApplication.output) {	
						System.out.println("------------------------------------------");
						System.out.println("\nReceived broadcast response, approving...\n");
						System.out.println("This SPOT: "+app.MAC+stateMachineId+", receiving SPOT: "+receiverId);
						System.out.println("------------------------------------------");
					}
					timerHandler.stopTimer(broadcastTimerId);
					receiverId = currentEvent.getData();
					Event timeout = new Event(Event.sendReadings, stateMachineId, System.currentTimeMillis());
					Event giveUp = new Event(Event.broadcastGiveUp, stateMachineId, System.currentTimeMillis());
					sendingTimerId = timerHandler.addNewTimer(frequency);
					giveUpTimerId = timerHandler.addNewTimer(giveUpTime);
					timerHandler.startTimer(giveUpTimerId, giveUp);
					timerHandler.startTimer(sendingTimerId, timeout);
					sendApproved();
					state = sending;
					returnControlToScheduler(false);
				}
				else {
					if (SunSpotApplication.output) {	
						System.out.println("------------------------------------------");
						System.out.println("\nReceived broadcast response in wrong context, denying...\n");
						System.out.println("------------------------------------------");
					}
					sendDenied();
					returnControlToScheduler(false);
				}
			}
			else if (currentEvent.getType() == Event.sendReadings) {
				
				if (state == sending) {
					if (SunSpotApplication.output) {	
						System.out.println("------------------------------------------");
						System.out.println("\nSending light readings...\n");
						System.out.println("This SPOT: "+app.MAC+stateMachineId+", receiving SPOT: "+receiverId);
						System.out.println("------------------------------------------");
					}
					timerHandler.resetTimer(sendingTimerId);
					timerHandler.resetTimer(giveUpTimerId);
					sendReadings();
					state = sending;
					returnControlToScheduler(false);
				}
			}
			else if (currentEvent.getType() == Event.broadcastGiveUp) {
				if (state == wait_response) {
					if (SunSpotApplication.output) {	
						System.out.println("------------------------------------------");
						System.out.println("\nNo responses received, giving up.\n");
						System.out.println("------------------------------------------");
					}
					blinkLEDs();
					state = ready;
					returnControlToScheduler(true);
				}
			}
			else if (currentEvent.getType() == Event.disconnect) {
				
				if (state == sending) {
					if (SunSpotApplication.output) {	
						System.out.println("------------------------------------------");
						System.out.println("\nDisconnecting...\n");
						System.out.println("This SPOT: "+app.MAC+stateMachineId+", receiving SPOT: "+receiverId);
						System.out.println("------------------------------------------");
					}
					sendDisconnect();
					blinkLEDs();
					state = ready;
					returnControlToScheduler(true);
				}
			}
			else if (currentEvent.getType() == Event.receiverDisconnect) {
				
				if (state == sending) {
					if (SunSpotApplication.output) {	
						System.out.println("------------------------------------------");
						System.out.println("\nReceiver disconnected.\n");
						System.out.println("This SPOT: "+app.MAC+stateMachineId+", receiving SPOT: "+receiverId);
						System.out.println("------------------------------------------");
					}
					blinkLEDs();
					state = ready;
					returnControlToScheduler(true);
				}
			}
			else {
				returnControlToScheduler(false);
			}
			currentEvent = null;		//making sure the event is consumed
			if (active) {
				try {
					if (SunSpotApplication.output) {	
						System.out.println("Sending state machine going to sleep...");
					}
					sleep(Inf);
				} catch (InterruptedException e) {	
					if (SunSpotApplication.output) {	
						System.out.println("Sending state machine interrupted");
					}
				}
			}
		}
	}
	
	/**
	 * Sends a 'Denied' message to the SunSPOT being denied a connection.
	 */
	private void sendDenied() {
		Message denied = new Message(app.MAC+":"+stateMachineId, currentEvent.getData(), Message.Denied);
		app.com.sendRemoteMessage(denied);
	}

	/**
	 * Sends a 'SenderDisconnect' message to the connected receiver.
	 */
	private void sendDisconnect() {
		Message disconnect = new Message(app.MAC+":"+stateMachineId, receiverId, Message.SenderDisconnect);
		app.com.sendRemoteMessage(disconnect);
	}

	/**
	 * Sends an 'Approved' message to the connected receiver.
	 */
	private void sendApproved() {
		Message approved = new Message(app.MAC+":"+stateMachineId, receiverId, Message.Approved);
		app.com.sendRemoteMessage(approved);
	}

	/**
	 * Blinks LEDs to indicate a disconnect.
	 */
	private void blinkLEDs() {
		app.blinkLEDsDynamic(LEDColor.RED, 200, 0, 3);
	}

	/**
	 * Sends a broadcast request to any listening SunSPOTs.
	 */
	private void sendBroadcast() {
		Message message = new Message(app.MAC+":"+stateMachineId, Message.BROADCAST_ADDRESS, Message.CanYouDisplayMyReadings);
		app.com.sendRemoteMessage(message);
	}
	
	/**
	 * Sends a message with the current light readings.
	 */
	private void sendReadings() {
		Message message = new Message(app.MAC+":"+stateMachineId, receiverId, Message.Reading+getReadings());
		app.com.sendRemoteMessage(message);
	}
	
	/**
	 * Fetches light readings from this SunSPOT.
	 */
	private String getReadings() {
		readings = -1;
		try {
			readings = app.lightSensor.getAverageValue();
		} catch (IOException e) {
			if (SunSpotApplication.output) {	
				System.out.println("Failed to read from box");
			}
			e.printStackTrace();
		}
		return Integer.toString(readings);
	}
	
	public String getReceiverId() {
		return receiverId;
	}
	
}

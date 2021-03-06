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
	
	public static final String free = "free";
	public static final String busy = "busy";
	public static final String wait_approved = "wait_approved";
	
	private String senderId;
	private String giveUpTimerId;
	private long giveUpTime = 5000;
	
	public ReceiveStateMachine(String stateMachineId, Scheduler scheduler, TimerHandler timerHandler, SunSpotApplication app) {
		super(stateMachineId, scheduler, timerHandler, app);
		this.state = free;
	}
	
	public ReceiveStateMachine(String stateMachineId, Scheduler scheduler, TimerHandler timerHandler, SunSpotApplication app, int priority) {
		super(stateMachineId, scheduler, timerHandler, app, priority);
		this.state = free;
	}
	
	/**
	 * Sleeps until interrupted, and handles/consumees the current {@link Event} when interrupted according to
	 * state and event type.
	 */
	public void run() {
		while (active) {
			if (SunSpotApplication.output) {	
				System.out.println("Receiving thread: "+Thread.currentThread());
			}
			if (currentEvent == null) {
				if (SunSpotApplication.output) {	
					System.out.println("Receive state machine interrupted without event");
				}
				returnControlToScheduler(false);
			}
			else if (currentEvent.getType() == Event.broadcast) {
				if (state == free) {
					if (SunSpotApplication.output) {	
						System.out.println("------------------------------------------");
						System.out.println("\nBroadcast received!\n");
						System.out.println("This SPOT: "+app.MAC+", sending SPOT: "+senderId);
						System.out.println("------------------------------------------");
					}
					Event giveUp = new Event(Event.receiverGiveUp, stateMachineId, System.currentTimeMillis());
					giveUpTimerId = timerHandler.addNewTimer(giveUpTime);
					timerHandler.startTimer(giveUpTimerId, giveUp);
					sendBroadcastResponse();
					state = wait_approved;
					returnControlToScheduler(false);
				}
				else if (state == wait_approved) {
					if (SunSpotApplication.output) {	
						System.out.println("------------------------------------------");
						System.out.println("\nBroadcast received, but already waiting. Saving for later.\n");
						System.out.println("This SPOT: "+app.MAC+", sending SPOT: "+senderId);
						System.out.println("------------------------------------------");
					}
					scheduler.saveEvent(currentEvent, stateMachineId);
					state = wait_approved;
					returnControlToScheduler(false);
				}
			}
			else if (currentEvent.getType() == Event.connectionApproved) {
				if (state == wait_approved) {
					if (SunSpotApplication.output) {	
						System.out.println("------------------------------------------");
						System.out.println("\nConnection approved!\n");
						System.out.println("This SPOT: "+app.MAC+", sending SPOT: "+senderId);
						System.out.println("------------------------------------------");
					}
					state = busy;
					returnControlToScheduler(false);
				}
			}
			else if (currentEvent.getType() == Event.connectionDenied) {
				if (state == wait_approved) {
					if (SunSpotApplication.output) {	
						System.out.println("------------------------------------------");
						System.out.println("\nConnection denied!\n");
						System.out.println("This SPOT: "+app.MAC+", sending SPOT: "+senderId);
						System.out.println("------------------------------------------");
					}
					blinkLEDs();
					state = free;
					returnControlToScheduler(true);
				}
			}
			else if (currentEvent.getType() == Event.senderDisconnect) {
				if (state == busy) {
					if (SunSpotApplication.output) {	
						System.out.println("------------------------------------------");
						System.out.println("\nSender disconnected.\n");
						System.out.println("This SPOT: "+app.MAC+", sending SPOT: "+senderId);
						System.out.println("------------------------------------------");
					}
					blinkLEDs();
					state = free;
					returnControlToScheduler(true);
				}
			}
			else if (currentEvent.getType() == Event.disconnect) {
				if (state == busy) {
					if (SunSpotApplication.output) {	
						System.out.println("------------------------------------------");
						System.out.println("\nDisconnecting...\n");
						System.out.println("This SPOT: "+app.MAC+", sending SPOT: "+senderId);
						System.out.println("------------------------------------------");
					}
					sendDisconnect();
					blinkLEDs();
					state = free;
					returnControlToScheduler(true);
				}
			}
			else if (currentEvent.getType() == Event.receiveReadings) {
				if (state == busy) {
					if (SunSpotApplication.output) {	
						System.out.println("------------------------------------------");
						System.out.println("\nReadings received.\n");
						System.out.println("This SPOT: "+app.MAC+", sending SPOT: "+currentEvent.getStateMachineId());
						System.out.println("------------------------------------------");
					}
					displayReadings();
					state = busy;
					returnControlToScheduler(false);
				}
				else {
					if (SunSpotApplication.output) {	
						System.out.println("------------------------------------------");
						System.out.println("\nReadings received, but wrong context, disonnecting from this sender...\n");
						System.out.println("------------------------------------------");
					}
					sendDisconnect();
					returnControlToScheduler(false);
				}
				currentEvent = null;		//making sure the event is consumed
			}
			else if (currentEvent.getType() == Event.receiverGiveUp) {
				if (state == busy) {
					if (SunSpotApplication.output) {	
						System.out.println("------------------------------------------");
						System.out.println("\nTimeout! Assuming connection has died.\n");
						System.out.println("------------------------------------------");
					}
					blinkLEDs();
					state = free;
					returnControlToScheduler(true);
				}
			}
			else {
				if (SunSpotApplication.output) {	
					System.out.println("Event type not recognized.");
				}
				returnControlToScheduler(false);
			}
			currentEvent = null;		//making sure the event is consumed
			if (active) {
				try {
					sleep(Inf);
				} catch (InterruptedException e) { 					
					if (SunSpotApplication.output) {	
						System.out.println("Receive state machine interrupted");
					}
				}
			}
		}
	}

	/**
	 * Sends a 'ReceiverDisconnect' message to the connected sender.
	 */
	private void sendDisconnect() {
		Message disconnect = new Message(app.MAC+":"+stateMachineId, senderId, Message.ReceiverDisconnect);
		app.com.sendRemoteMessage(disconnect);
	}

	/**
	 * Blinks LEDs to indicate a disconnect.
	 */
	private void blinkLEDs() {
		app.blinkLEDsDynamic(LEDColor.RED, 200, 0, 3);
	}

	/**
	 * Sends an 'ICanDisplayReadings' response to a broadcaster.
	 */
	private void sendBroadcastResponse() {
		senderId = currentEvent.getStateMachineId();
		Message response = new Message(app.MAC+":"+stateMachineId, senderId, Message.ICanDisplayReadings);
		app.com.sendRemoteMessage(response);
	}

	/**
	 * Displays a LED representation of the received light readings on this SunSPOT. 
	 */
	private void displayReadings() {
		app.showLightreadings(Integer.parseInt(currentEvent.getData()));
	}
}

package no.ntnu.item.ttm4160.sunspot.runtime;

import java.util.Enumeration;

import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.ISwitch;
import com.sun.spot.sensorboard.peripheral.ISwitchListener;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.communication.Communications;
import no.ntnu.item.ttm4160.sunspot.communication.ICommunicationLayerListener;
import no.ntnu.item.ttm4160.sunspot.communication.Message;
import no.ntnu.item.ttm4160.sunspot.utils.Event;

/**
 * Class for handling communication messages and action events, and passing them to the scheduler.
 * This class is application-specific.
 */
public class EventHandler implements ICommunicationLayerListener, ISwitchListener {

	private Scheduler scheduler;
	private SunSpotApplication app;
	private ISwitch sw1, sw2;
	private int maxReceiveConnections = 1;
	private int maxSendConnections = 1;
	private int activeReceiveConnections;
	private int activeSendConnections;
	private boolean sendAndReceive = false;
	
	public static final String button1 = "button1";
	public static final String button2 = "button2";
	
	/**
	 * 
	 * @param scheduler Reference to the {@link Scheduler}
	 * @param app Reference to the {@link SunSpotApplication}
	 */
	public EventHandler(Scheduler scheduler, SunSpotApplication app) {
		this.scheduler = scheduler;
		this.app = app;
		 sw1 = EDemoBoard.getInstance().getSwitches()[0];  
	     sw2 = EDemoBoard.getInstance().getSwitches()[1];
	     sw1.addISwitchListener(this);
	     sw2.addISwitchListener(this);
	     activeSendConnections = 0;
	     activeReceiveConnections = 0;
	}

	/**
	 * Handles an action from the {@link Communications} module. Generates an {@link Event} based on the action,
	 * and passes the event to the {@link Scheduler}. Creates a new {@link StateMachine} if applicable.
	 */
	public void actionReceived(String action) {
		if (SunSpotApplication.output) {
			System.out.println("Button pressed.");
		}
		if (action.equals(button1)) {
			if (activeSendConnections >= maxSendConnections) {
				if (SunSpotApplication.output) {
					System.out.println("Too many connections, skipping broadcast.");
				}
				return;
			}
			String stateMachineId = ""+System.currentTimeMillis();
			int priority = 0;
			TimerHandler timerHandler = new TimerHandler(stateMachineId, scheduler, this, priority);
			SendingStateMachine sendingStateMachine = new SendingStateMachine(stateMachineId, scheduler, timerHandler, app, priority);
			EventQueue eventQueue = new EventQueue(stateMachineId, priority);
			Event event = generateEvent(action, stateMachineId);
			
			Thread stateMachineThread = sendingStateMachine.startThread();
			activeSendConnections++;
			if (SunSpotApplication.output) {
				System.out.println("Number of active send connections: "+activeSendConnections);
			}
			scheduler.addStateMachine(sendingStateMachine);
			scheduler.addStateMachineThread(stateMachineThread, stateMachineId);
			scheduler.addEventQueue(eventQueue);
			scheduler.addTimerHandler(timerHandler);
			scheduler.addInternalEvent(event);
		}
		else if (action.equals(button2)) {
			if (SunSpotApplication.output) {
				System.out.println("Disconnecting all.");
			}
			disconnectAll();
		}
	}

	/**
	 * Stops all running timers.
	 * Creates a disconnect {@link Event} for each active {@link StateMachine}.
	 */
	private void disconnectAll() {
		activeReceiveConnections = 0;
		activeSendConnections = 0;
		Enumeration ids = scheduler.getIDs();
		while (ids.hasMoreElements()) {
			String id = ids.nextElement().toString();
			scheduler.killAllTimers(id);
			Event event = generateEvent(button2, id);
			scheduler.addInternalEvent(event);
		}
	}

	/**
	 * Handles a {@link Message} from the {@link Communications} module. Generates an {@link Event} based on the message content,
	 * and p {@link Scheduler}. Creates a new {@link StateMachine} if applicable.
	 * @param message
	 */
	public synchronized void inputReceived(Message message) {
		if (SunSpotApplication.output) {
			System.out.println("Input received.");
		}
		Event event = generateEvent(message);
		if (!scheduler.checkIfActive(event.getStateMachineId()) && !message.getContent().equals(Message.CanYouDisplayMyReadings)) {
			if (SunSpotApplication.output) {
				System.out.println("Event received for inactive state machine, discarding.");
			}
		}
		else if (message.getContent().equals(Message.CanYouDisplayMyReadings)) {
			if (SunSpotApplication.output) {
				System.out.println("Broadcast received by event handler.");				
			}
			
			if (activeReceiveConnections >= maxReceiveConnections) {
				if (SunSpotApplication.output) {	
					System.out.println("Too many connections, discarding broadcast.");
				}
				return;
			}
			else if (!sendAndReceive && (scheduler.checkIfActive(event.getStateMachineId()) || scheduler.checkIfReceiver(message.getSenderMAC()))) {	// 
				if (SunSpotApplication.output) {					
					System.out.println("Already communication with this SPOT, discarding broadcast.");
				}
				return;
			}
			
			String stateMachineId = message.getSender();
			int priority = 0;
			TimerHandler timerHandler = new TimerHandler(stateMachineId, scheduler, this, priority);
			ReceiveStateMachine receiveStateMachine = new ReceiveStateMachine(stateMachineId, scheduler, timerHandler, app);
			EventQueue eventQueue = new EventQueue(stateMachineId, priority);
			Thread stateMachineThread = receiveStateMachine.startThread();
			activeReceiveConnections++;
			if (SunSpotApplication.output) {	
				System.out.println("Number of active receive connections: "+activeReceiveConnections);
			}
			scheduler.addStateMachine(receiveStateMachine);
			scheduler.addStateMachineThread(stateMachineThread, stateMachineId);
			scheduler.addEventQueue(eventQueue);
			scheduler.addTimerHandler(timerHandler);
			scheduler.addExternalEvent(event);
		}
		else if (message.getContent().equals(Message.Approved)) {
			scheduler.resetAllTimers(event.getStateMachineId());
			scheduler.addExternalEvent(event);
		}
		else if (message.getContent().equals(Message.Denied)) {
			activeReceiveConnections--;
			scheduler.killAllTimers(event.getStateMachineId());
			scheduler.addExternalEvent(event);
		}
		else if (message.getContent().indexOf(Message.Reading) != -1) {
			scheduler.resetAllTimers(event.getStateMachineId());
			scheduler.addExternalEvent(event);
		}
		else if (message.getContent().equals(Message.SenderDisconnect)) {
			activeReceiveConnections--;
			scheduler.killAllTimers(event.getStateMachineId());
			scheduler.addExternalEvent(event);
		}
		else if (message.getContent().equals(Message.ReceiverDisconnect)) {
			activeSendConnections--;
			scheduler.killAllTimers(event.getStateMachineId());
			scheduler.addExternalEvent(event);
		}
		else {
			scheduler.addExternalEvent(event);
		}
	}
	
	/**
	 * Generates an event based on an action.
	 * @param action {@link String}
	 * @param stateMachineId The ID of the {@link StateMachine} this event is created for.
	 * @return {@link Event}
	 */
	private Event generateEvent(String action, String stateMachineId) {
		if (action.equals(button1)) {
			return new Event(Event.broadcast, stateMachineId, System.currentTimeMillis());
		}
		else if (action.equals(button2)) {
			return new Event(Event.disconnect, stateMachineId, System.currentTimeMillis());
		}
		return new Event(Event.noEvent, "", System.currentTimeMillis());
	}
	
	/**
	 * Generates an {@link Event} based on {@link Message} content.
	 * @param message
	 * @return {@link Event}
	 */
	private Event generateEvent(Message message) {
		if(message.getReceiver().equals(Message.BROADCAST_ADDRESS)) {
			return new Event(Event.broadcast, message.getSender(), System.currentTimeMillis());
		}
		else if(message.getContent().equals(Message.ICanDisplayReadings)) {
			return new Event(Event.broadcast_response, message.getReceiverId(), message.getSender(), System.currentTimeMillis());
		}
		else if(message.getContent().equals(Message.Approved)) {
			return new Event(Event.connectionApproved, message.getReceiverId(), message.getReceiver(), System.currentTimeMillis());
		}
		else if(message.getContent().equals(Message.Denied)) {
			return new Event(Event.connectionDenied, message.getReceiverId(), message.getReceiver(), System.currentTimeMillis());
		}
		else if(message.getContent().equals(Message.ReceiverDisconnect)) {
			return new Event(Event.receiverDisconnect, message.getReceiverId(), System.currentTimeMillis());
		}
		else if(message.getContent().equals(Message.SenderDisconnect)) {
			return new Event(Event.senderDisconnect, message.getReceiverId(), System.currentTimeMillis());
		}
		else if(message.getContent().indexOf(Message.Reading) != -1) {
			String reading = message.getContent().substring(message.getContent().indexOf(":")+1);
			return new Event(Event.receiveReadings, message.getReceiverId(), reading, System.currentTimeMillis());
		}
		return new Event(Event.noEvent, "", System.currentTimeMillis());
	}

	/**
     * Listens for button actions, and notifies listeners.
     */
	public void switchPressed(ISwitch sw) {		
		if (sw == sw1) {
			actionReceived(button1);
		}
		else {
			actionReceived(button2);
		}
	}

	public void switchReleased(ISwitch sw) {
		
	}
	
	public synchronized void decreaseActiveSendConnections() {
		activeSendConnections--;
		if (SunSpotApplication.output) {	
			System.out.println("Number of active send connections: "+activeSendConnections);
		}
	}
	
	public synchronized void decreaseActiveReceiveConnections() {
		activeReceiveConnections--;
		if (SunSpotApplication.output) {	
			System.out.println("Number of active receive connections: "+activeReceiveConnections);
		}
	}


}

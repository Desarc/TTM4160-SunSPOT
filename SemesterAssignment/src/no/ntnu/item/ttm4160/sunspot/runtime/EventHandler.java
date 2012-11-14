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
	private int maxSendConnections = 5;
	private int activeReceiveConnections;
	private int activeSendConnections;
	
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
//			TestStateMachine test = new TestStateMachine(""+System.currentTimeMillis(), scheduler, app, LEDColor.BLUE, 100, false);
//			EventQueue eventQueue = new EventQueue(test.getId(), test.getStateMachinePriority());
//			Event event = new Event(Event.testOn, test.getId(), System.currentTimeMillis());
//			TimerHandler handler = new TimerHandler(test.getId(), scheduler, this, test.getStateMachinePriority());
//			Thread stateMachineThread = test.startThread();
//			scheduler.addStateMachine(test);
//			scheduler.addStateMachineThread(stateMachineThread, test.getId());
//			scheduler.addEventQueue(eventQueue);
//			scheduler.addTimerHandler(handler);
//			scheduler.addInternalEvent(event);
			if (activeSendConnections >= maxSendConnections) {
				if (SunSpotApplication.output) {
					System.out.println("Too many connections, skipping broadcast.");
				}
				return;
			}
			SendingStateMachine sendingStateMachine = new SendingStateMachine(""+System.currentTimeMillis(), scheduler, app);
			EventQueue eventQueue = new EventQueue(sendingStateMachine.getStateMachineId(), sendingStateMachine.getStateMachinePriority());
			Event event = generateEvent(action, sendingStateMachine.getStateMachineId());
			TimerHandler handler = new TimerHandler(sendingStateMachine.getStateMachineId(), scheduler, this, sendingStateMachine.getStateMachinePriority());
			Thread stateMachineThread = sendingStateMachine.startThread();
			activeSendConnections++;
			if (SunSpotApplication.output) {
				System.out.println("Number of active send connections: "+activeSendConnections);
			}
			scheduler.addStateMachine(sendingStateMachine);
			scheduler.addStateMachineThread(stateMachineThread, sendingStateMachine.getStateMachineId());
			scheduler.addEventQueue(eventQueue);
			scheduler.addTimerHandler(handler);
			scheduler.addInternalEvent(event);
		}
		else if (action.equals(button2)) {
			if (SunSpotApplication.output) {
				//System.out.println("number of active statemachines "+scheduler.getActiveStateMachineConnections());
			}
			disconnectAll();
//			TestStateMachine test = new TestStateMachine(""+System.currentTimeMillis(), scheduler, app, LEDColor.GREEN, 1000, true);
//			EventQueue eventQueue = new EventQueue(test.getId(), test.getStateMachinePriority());
//			Event event = new Event(Event.testOn, test.getId(), System.currentTimeMillis());
//			TimerHandler handler = new TimerHandler(test.getId(), scheduler, this, test.getStateMachinePriority());
//			Thread stateMachineThread = test.startThread();
//			scheduler.addStateMachine(test);
//			scheduler.addStateMachineThread(stateMachineThread, test.getId());
//			scheduler.addEventQueue(eventQueue);
//			scheduler.addTimerHandler(handler);
//			scheduler.addInternalEvent(event);
		}
	}

	/**
	 * Creates a disconnect {@link Event} for each active {@link StateMachine}.
	 */
	private void disconnectAll() {
		activeReceiveConnections = 0;
		activeSendConnections = 0;
		Enumeration ids = scheduler.getIDs();
		while (ids.hasMoreElements()) {
			Event event = generateEvent(button2, ids.nextElement().toString());
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
//			if (scheduler.checkIfActive(message.getSender()) ) {	// || scheduler.checkIfReceiver(message.getSenderMAC())
//				if (SunSpotApplication.output) {					
//					System.out.println("Already communication with this SPOT, discarding broadcast.");
//				}
//				return;
//			}
			if (activeReceiveConnections >= maxReceiveConnections) {
				if (SunSpotApplication.output) {	
					System.out.println("Too many connections, discarding broadcast.");
				}
				return;
			}
			ReceiveStateMachine receiveStateMachine = new ReceiveStateMachine(message.getSender(), scheduler, app);
			EventQueue eventQueue = new EventQueue(receiveStateMachine.getStateMachineId(), receiveStateMachine.getStateMachinePriority());
			TimerHandler handler = new TimerHandler(receiveStateMachine.getStateMachineId(), scheduler, this, receiveStateMachine.getStateMachinePriority());
			Thread stateMachineThread = receiveStateMachine.startThread();
			activeReceiveConnections++;
			if (SunSpotApplication.output) {	
				System.out.println("Number of active receive connections: "+activeReceiveConnections);
			}
			scheduler.addStateMachine(receiveStateMachine);
			scheduler.addStateMachineThread(stateMachineThread, receiveStateMachine.getStateMachineId());
			scheduler.addEventQueue(eventQueue);
			scheduler.addTimerHandler(handler);
			scheduler.addExternalEvent(event);
		}
		else if (message.getContent().equals(Message.ICanDisplayReadings)) {
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
			decreaseActiveReceiveConnections();
			return new Event(Event.connectionDenied, message.getReceiverId(), message.getReceiver(), System.currentTimeMillis());
		}
		else if(message.getContent().equals(Message.ReceiverDisconnect)) {
			decreaseActiveSendConnections();
			scheduler.killAllTimers(message.getReceiverId());
			return new Event(Event.receiverDisconnect, message.getReceiverId(), System.currentTimeMillis());
		}
		else if(message.getContent().equals(Message.SenderDisconnect)) {
			decreaseActiveReceiveConnections();
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

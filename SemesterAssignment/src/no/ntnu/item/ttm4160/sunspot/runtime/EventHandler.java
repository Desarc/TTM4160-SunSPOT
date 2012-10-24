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
	}

	/**
	 * Handles an action from the {@link Communications} module. Generates an {@link Event} based on the action,
	 * and passes the event to the {@link Scheduler}. Creates a new {@link StateMachine} if applicable.
	 */
	public void actionReceived(String action) {
		if (action.equals(button1)) {
//			TestStateMachine test = new TestStateMachine(""+System.currentTimeMillis(), scheduler, app);
//			EventQueue eventQueue = new EventQueue(test.getId(), test.getStateMachinePriority());
//			Event event = new Event(Event.testOn, test.getId(), System.currentTimeMillis());
//			TimerHandler handler = new TimerHandler(test.getId(), scheduler, test.getStateMachinePriority());
//			Thread stateMachineThread = test.startThread();
//			scheduler.addStateMachine(test);
//			scheduler.addStateMachineThread(stateMachineThread, test.getId());
//			scheduler.addEventQueue(eventQueue);
//			scheduler.addTimerHandler(handler);
//			scheduler.addEvent(event);
			SendingStateMachine sendingStateMachine = new SendingStateMachine(""+System.currentTimeMillis(), scheduler, app);
			EventQueue eventQueue = new EventQueue(sendingStateMachine.getId(), sendingStateMachine.getStateMachinePriority());
			Event event = generateEvent(action, sendingStateMachine.getId());
			TimerHandler handler = new TimerHandler(sendingStateMachine.getId(), scheduler, sendingStateMachine.getStateMachinePriority());
			Thread stateMachineThread = sendingStateMachine.startThread();
			scheduler.addStateMachine(sendingStateMachine);
			scheduler.addStateMachineThread(stateMachineThread, sendingStateMachine.getId());
			scheduler.addEventQueue(eventQueue);
			scheduler.addTimerHandler(handler);
			scheduler.addEvent(event);
		}
		else if (action.equals(button2)) {
			disconnectAll();
		}
	}

	private void disconnectAll() {
		Enumeration ids = scheduler.getIDs();
		while (ids.hasMoreElements()) {
			Event event = generateEvent(button2, ids.nextElement().toString());
			scheduler.addEvent(event);
		}
	}

	/**
	 * Handles a {@link Message} from the {@link Communications} module. Generates an {@link Event} based on the message content,
	 * and p {@link Scheduler}. Creates a new {@link StateMachine} if applicable.
	 * @param message
	 */
	public synchronized void inputReceived(Message message) {
		System.out.println("Input received");
		Event event = generateEvent(message);
		if (message.getContent().equals(Message.CanYouDisplayMyReadings)) {
			ReceiveStateMachine receiveStateMachine = new ReceiveStateMachine(message.getSender(), scheduler, app);
			EventQueue eventQueue = new EventQueue(receiveStateMachine.getId(), receiveStateMachine.getStateMachinePriority());
			TimerHandler handler = new TimerHandler(receiveStateMachine.getId(), scheduler, receiveStateMachine.getStateMachinePriority());
			Thread stateMachineThread = receiveStateMachine.startThread();
			scheduler.addStateMachine(receiveStateMachine);
			scheduler.addStateMachineThread(stateMachineThread, receiveStateMachine.getId());
			scheduler.addEventQueue(eventQueue);
			scheduler.addTimerHandler(handler);
			scheduler.addEvent(event);
		}
		else if (message.getContent().equals(Message.ICanDisplayReadings)) {
			if (scheduler.checkIfActive(event.getStateMachineId())) {
				scheduler.killAllTimers(event.getStateMachineId());
				scheduler.addEvent(event);
			}
			else {
				System.out.println("Event received for inactive state machine, discarding.");
			}
		}
		else if(message.getContent().equals(Message.Approved)) {
			if (scheduler.checkIfActive(event.getStateMachineId())) {
				scheduler.addEvent(event);
			}
			else {
				System.out.println("Event received for inactive state machine, discarding.");
			}
		}
		else if(message.getContent().equals(Message.Denied)) {
			if (scheduler.checkIfActive(event.getStateMachineId())) {
				scheduler.addEvent(event);
			}
			else {
				System.out.println("Event received for inactive state machine, discarding.");
			}
		}
		else if(message.getContent().equals(Message.ReceiverDisconnect)) {
			if (scheduler.checkIfActive(event.getStateMachineId())) {
				scheduler.addEvent(event);
			}
			else {
				System.out.println("Event received for inactive state machine, discarding.");
			}
		}
		else if(message.getContent().equals(Message.SenderDisconnect)) {
			if (scheduler.checkIfActive(event.getStateMachineId())) {
				scheduler.addEvent(event);
			}
			else {
				System.out.println("Event received for inactive state machine, discarding.");
			}
		}
		else if(message.getContent().indexOf(Message.Reading) != -1) {
			if (scheduler.checkIfActive(event.getStateMachineId())) {
				scheduler.addEvent(event);
			}
			else {
				System.out.println("Event received for inactive state machine, discarding.");
			}
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
			return new Event(Event.connectionDenied, message.getReceiverId(), message.getSender(), System.currentTimeMillis());
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


}

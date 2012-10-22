package no.ntnu.item.ttm4160.sunspot.runtime;

import no.ntnu.item.ttm4160.sunspot.SunSpotApplication;
import no.ntnu.item.ttm4160.sunspot.SunSpotListener;
import no.ntnu.item.ttm4160.sunspot.communication.Communications;
import no.ntnu.item.ttm4160.sunspot.communication.ICommunicationLayerListener;
import no.ntnu.item.ttm4160.sunspot.communication.Message;
import no.ntnu.item.ttm4160.sunspot.utils.Event;

/**
 * Class for handling communication messages and action events, and passing them to the scheduler.
 * This class is application-specific.
 */
public class EventHandler implements ICommunicationLayerListener, SunSpotListener {

	private Scheduler scheduler;
	private SunSpotApplication app;
	
	/**
	 * 
	 * @param scheduler Reference to the {@link Scheduler}
	 * @param app Reference to the {@link SunSpotApplication}
	 */
	public EventHandler(Scheduler scheduler, SunSpotApplication app) {
		this.scheduler = scheduler;
		this.app = app;
	}
	
	String id;

	/**
	 * Handles an action from the {@link Communications} module. Generates an {@link Event} based on the action,
	 * and passes the event to the {@link Scheduler}. Creates a new {@link StateMachine} if applicable.
	 */
	public void actionReceived(String action) {
		if (action.equals(SunSpotApplication.button1)) {
			TestStateMachine test = new TestStateMachine(""+System.currentTimeMillis(), scheduler, app);
			EventQueue eventQueue = new EventQueue(test.getId(), test.getPriority());
			id = test.getId();
			Event event = new Event(Event.testOn, test.getId(), System.currentTimeMillis());
			TimerHandler handler = new TimerHandler(test.getId(), scheduler, test.getPriority());
			scheduler.addStateMachine(test);
			scheduler.addEventQueue(eventQueue);
			scheduler.addTimerHandler(handler);
			scheduler.addEvent(event);
//			SendingStateMachine sendingStateMachine = new SendingStateMachine(""+System.currentTimeMillis(), this, app);
//			activeStateMachines.put(SendingStateMachine.getId(), SendingStateMachine);
//			EventQueue eventQueue = new EventQueue(sendingStateMachine.getId(), sendingStateMachine.getPriority());
//			Event event = generateEvent(action, sendingStateMachine.getId());
//			eventQueue.addEvent(event);
//			eventQueues.put(sendingStateMachine.getId(), eventQueue);
//			TimerHandler handler = new TimerHandler(sendingStateMachine.getId(), this, sendingStateMachine.getPriority());
//			timerHandlers.put(sendingStateMachine.getId(), handler);
		}
		else if (action.equals(SunSpotApplication.button2)) {
			Event event = new Event(Event.testOff, id, System.currentTimeMillis());
			scheduler.addEvent(event);
		}
	}

	/**
	 * Handles a {@link Message} from the {@link Communications} module. Generates an {@link Event} based on the message content,
	 * and p {@link Scheduler}. Creates a new {@link StateMachine} if applicable.
	 * @param message
	 */
	public synchronized void inputReceived(Message message) {
		Event event = generateEvent(message);
		if (message.getContent().equals(Message.CanYouDisplayMyReadings)) {
			ReceiveStateMachine receiveStateMachine = new ReceiveStateMachine(message.getSender(), scheduler, app);
			EventQueue eventQueue = new EventQueue(receiveStateMachine.getId(), receiveStateMachine.getPriority());
			eventQueue.addEvent(event);
			scheduler.addEventQueue(eventQueue);
		}
		else if (message.getContent().equals(Message.ICanDisplayReadings)) {
			scheduler.addEvent(event);
		}
		else if(message.getContent().equals(Message.Approved)) {
			scheduler.addEvent(event);
		}
		else if(message.getContent().equals(Message.Denied)) {
			scheduler.addEvent(event);
		}
		else if(message.getContent().equals(Message.ReceiverDisconnect)) {
			scheduler.addEvent(event);
		}
		else if(message.getContent().equals(Message.SenderDisconnect)) {
			scheduler.addEvent(event);
		}
	}
	
	/**
	 * Generates an event based on an action.
	 * @param action {@link String}
	 * @param stateMachineId The ID of the {@link StateMachine} this event is created for.
	 * @return {@link Event}
	 */
	private Event generateEvent(String action, String stateMachineId) {
		if (action.equals(SunSpotApplication.button1)) {
			return new Event(Event.broadcast, stateMachineId, System.currentTimeMillis());
		}
		else if (action.equals(SunSpotApplication.button2)) {
			return new Event(Event.disconnect, stateMachineId, System.currentTimeMillis());
		}
		return new Event(0, "", System.currentTimeMillis());
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
			return new Event(Event.broadcast_response, message.getReceiver(), message.getSender(), System.currentTimeMillis());
		}
		else if(message.getContent().equals(Message.Approved)) {
			return new Event(Event.connectionApproved, message.getReceiver(), System.currentTimeMillis());
		}
		else if(message.getContent().equals(Message.Denied)) {
			return new Event(Event.connectionDenied, message.getReceiver(), System.currentTimeMillis());
		}
		else if(message.getContent().equals(Message.ReceiverDisconnect)) {
			return new Event(Event.receiverDisconnect, message.getReceiver(), System.currentTimeMillis());
		}
		else if(message.getContent().equals(Message.SenderDisconnect)) {
			return new Event(Event.senderDisconnect, message.getReceiver(), System.currentTimeMillis());
		}
		return new Event(0, "", System.currentTimeMillis());
	}

}

package no.ntnu.item.ttm4160.sunspot.communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;


import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.io.j2me.radiostream.RadiostreamConnection;
import com.sun.spot.peripheral.NoRouteException;

public class Communications implements ICommunicationLayer{
	
	private static int STATE_INITIALIZING=6;
	private static int STATE_ACTIVE=5;

	RadiogramListener serverListener;
	/**
	 * Key: String MAc address of other end
	 * Value: ReliableConnection object
	 */
	Hashtable remoteAddressBook;
	String myMACAddress;
	Vector listeners;
	
	
	
	public Communications(String myMACAddress) {
		this.myMACAddress=myMACAddress;
		remoteAddressBook=new Hashtable();
		listeners=new Vector();
		try {
			startServer();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void destroy(){
		serverListener.destroy();
		for (Enumeration iterator = remoteAddressBook.elements(); iterator.hasMoreElements();) {
			ReliableConnection rc = (ReliableConnection) iterator.nextElement();
			rc.destroy();
		}
	}
	
	/* (non-Javadoc)
	 * @see no.ntnu.item.ttm4160.sunspot.ICommunication#sendRemoteMessage(no.ntnu.item.ttm4160.sunspot.Message)
	 */
	public void sendRemoteMessage(Message msg){
		try {
			if(remoteAddressBook.containsKey(msg.getReceiverMAC())){
				ReliableConnection rConn=(ReliableConnection)remoteAddressBook.get(msg.getReceiverMAC());
				rConn.sendStreamMessage(msg);
			}
			else if(msg.getReceiverMAC().equals(Message.BROADCAST_ADDRESS)){
				sendBroadcast(msg.serializeMessage());
			}
			else{
				//We do not yet have a reliable connection to this receiver
				openReliableConnection(msg, true);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	private synchronized void addMessage(Message msg){
		for (int i = 0; i < listeners.size(); i++) {
			ICommunicationLayerListener listener=(ICommunicationLayerListener)listeners.elementAt(i);
			listener.inputReceived(msg);
		}
	}
	
	
	
	
	private void sendBroadcast(String content) throws IOException{
		DatagramConnection clientConn = (DatagramConnection)Connector.open("radiogram://"+Message.BROADCAST_ADDRESS+":"+Message.DATAGRAM_PORT);
		Datagram clientdg = clientConn.newDatagram(clientConn.getMaximumLength()); 
    	clientdg.writeUTF(content); 
    	clientConn.send(clientdg); 
    	clientConn.close(); 
    }
	    
	private void startServer() throws IOException{
    	System.out.println("Starting server");
		RadiogramConnection rgc = (RadiogramConnection) Connector.open("radiogram://:"+Message.DATAGRAM_PORT);
		serverListener=new RadiogramListener(rgc);
		new Thread(serverListener).start();
    }
	
	
	
	
    
    private void sendDatagram(String address, Message message) throws IOException{
    	RadiogramConnection conn =  
    		(RadiogramConnection)Connector.open("radiogram://"+address+":"+Message.DATAGRAM_PORT); 
    		Datagram dg = conn.newDatagram(conn.getMaximumLength()); 
    		try { 
    			System.out.println("Sending datagram: "+message);
    		 dg.writeUTF(message.serializeMessage()); 
    		 conn.send(dg); 
    		} catch (NoRouteException e) { 
    		 System.out.println ("No route to "+address); 
    		} finally { 
    		 conn.close(); 
    		}
    }
    
    private void openReliableConnection(Message initialMsg, boolean initiator){
    	ReliableConnection rConn=new ReliableConnection(initialMsg, initiator);
    	String connectionAddress;
    	if(initiator){
			connectionAddress=initialMsg.getReceiverMAC();
		}
		else{
			connectionAddress=initialMsg.getSenderMAC();
		}
    	remoteAddressBook.put(connectionAddress, rConn);
    }
    
    
    private class RadiogramListener implements Runnable{

    	RadiogramConnection dgConn;
    	private boolean keepRunning;
    	
    	private RadiogramListener(RadiogramConnection dgConn) {
    		this.dgConn=dgConn;
    		keepRunning=true;
    	}
    	
    	public void run() {
    		while(keepRunning){
    			try {
        			Datagram dg = dgConn.newDatagram(dgConn.getMaximumLength());
        			dgConn.receive(dg); 
        	    	String rawString = dg.readUTF(); 
        	    	System.out.println("Received datagram: "+rawString);
        	    	Message msg=Message.deSerializeMessage(rawString);
        	    	processIncomingMessage(msg);
        		} catch (IOException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		} 
    		}
    	}
    	
    	private void processIncomingMessage(Message msg){
    		//TODO check that it is really meant for us?
    		if(msg.getContent().equals(Message.DATAGRAM_SYN)){
    			if(!remoteAddressBook.containsKey(msg.getSenderMAC())){
    				openReliableConnection(msg, false);
    			}
    		}
    		else if(msg.getReceiver().equals(Message.BROADCAST_ADDRESS)){
    			addMessage(msg);
    		}
    		else{
    			System.out.println("Received datagram that is not broadcast or SYN!");
    		}
    	}
    	
    	private void destroy(){
    		keepRunning=false;
    		try {
				dgConn.close();
			} catch (IOException e) {
			}
    	}
    }
    
    private class ReliableConnection implements Runnable{

    	private boolean keepRunning;
    	private int state;
    	private RadiostreamConnection radioConn;
    	private Vector sendQueue=new Vector();
    	private String connectionAddress;
    	
    	private DataInputStream inStream;
    	private DataOutputStream outStream;
    	
    	private ReliableConnection(Message initialMsg, boolean initiator) {
    		state=STATE_INITIALIZING;
    		keepRunning=true;
    		if(initiator){
    			connectionAddress=initialMsg.getReceiverMAC();
    			sendQueue.addElement(initialMsg);
    		}
    		else{
    			connectionAddress=initialMsg.getSenderMAC();
    		}
			try {
				System.out.println("Opening radiostream to "+connectionAddress);
				this.radioConn=openRadiostream(connectionAddress);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			if(initialMsg.getContent().equals(Message.DATAGRAM_SYN)){
				//The other side initiated the setup of a reliable connection
				Message ackMsg=new Message(myMACAddress, connectionAddress, Message.STREAM_ACK);
				try {
					sendLowLevelMessage(ackMsg);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else{
				//We have to initiate a reliable connection
				Message synMsg=new Message(myMACAddress, connectionAddress, Message.DATAGRAM_SYN);
				try {
					sendDatagram(connectionAddress, synMsg);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
    	
    	/**
         * 
         * @param address Should only contain MAC-address
         * @throws IOException 
         */
        private RadiostreamConnection openRadiostream(String address) throws IOException{
        	RadiostreamConnection conn = (RadiostreamConnection) Connector.open("radiostream://"+address+":"+Message.STREAM_PORT); 
    	    new Thread(this).start();
    	    return conn;
    	}
    	
		public void run() {
			try {
				inStream = radioConn.openDataInputStream();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			while(keepRunning){
				try {
					String rawString=inStream.readUTF();
        	    	System.out.println("Received: "+rawString);
        	    	Message msg=Message.deSerializeMessage(rawString);
        	    	processIncomingMessage(msg);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		}
		
		private void processIncomingMessage(Message msg){
			if(msg.getContent().equals(Message.STREAM_ACK)){
				state=STATE_ACTIVE;
				Message synAckMsg=new Message(myMACAddress, connectionAddress, Message.STREAM_SYN_ACK);
				try {
					sendLowLevelMessage(synAckMsg);
					sendAllMessagesInQueue();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if(msg.getContent().equals(Message.STREAM_SYN_ACK)){
				state=STATE_ACTIVE;
				try {
					sendAllMessagesInQueue();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else{
				addMessage(msg);
			}
			
		}
		
		private void sendLowLevelMessage(Message msg) throws IOException{
			System.out.println("Sending low level message on stream: "+msg);
			outStream=radioConn.openDataOutputStream();
			outStream.writeUTF(msg.serializeMessage()); 
		    outStream.flush(); 
		    outStream.close();
		    outStream=null;
		}
		
		private void sendStreamMessage(Message msg) throws IOException{
			if(state==STATE_ACTIVE){
				outStream=radioConn.openDataOutputStream();
				System.out.println("sending message on stream: "+msg);
			    outStream.writeUTF(msg.serializeMessage()); 
			    outStream.flush(); 
			    outStream.close();
			    outStream=null;
			}
			else{
				sendQueue.addElement(msg);
			}
	    }
		
		private void sendAllMessagesInQueue() throws IOException{
			for (int i = 0; i < sendQueue.size(); i++) {
				Message msg=(Message)sendQueue.elementAt(0);
				sendQueue.removeElementAt(0);
				sendStreamMessage(msg);
			}
		}
		
		private void destroy(){
			keepRunning=false;
			try {
				inStream.close();
				outStream.close();
				radioConn.close();
			} catch (IOException e) {
				
			}
    	}
    }

	public void registerListener(ICommunicationLayerListener listener) {
		listeners.addElement(listener);
	}

}



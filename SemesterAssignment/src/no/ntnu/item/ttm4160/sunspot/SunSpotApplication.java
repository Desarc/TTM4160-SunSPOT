/*
 * Copyright (c) 2006 Sun Microsystems, Inc.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package no.ntnu.item.ttm4160.sunspot;


import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import no.ntnu.item.ttm4160.sunspot.communication.Communications;
import no.ntnu.item.ttm4160.sunspot.communication.ICommunicationLayer;
import no.ntnu.item.ttm4160.sunspot.runtime.EventHandler;
import no.ntnu.item.ttm4160.sunspot.runtime.Scheduler;

import com.sun.spot.peripheral.Spot;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.ILightSensor;
import com.sun.spot.sensorboard.peripheral.ITriColorLED;
import com.sun.spot.sensorboard.peripheral.LEDColor;
import com.sun.spot.util.BootloaderListener;
import com.sun.spot.util.IEEEAddress;

/*
 * The startApp method of this class is called by the VM to start the
 * application.
 *
 * The manifest specifies this class as MIDlet-1, which means it will
 * be selected for execution.
 */
public class SunSpotApplication extends MIDlet {
	
	public Scheduler scheduler;
	public ICommunicationLayer com;
	public ITriColorLED [] leds = EDemoBoard.getInstance().getLEDs();
    public ILightSensor lightSensor = EDemoBoard.getInstance().getLightSensor();
    public String MAC;
    public EventHandler eventHandler;
    
    public static final boolean output = false;
	
    protected void startApp() throws MIDletStateChangeException {
    	if (SunSpotApplication.output) {	
    		System.out.println("Main thread: "+Thread.currentThread());
		}
        new BootloaderListener().start();   // monitor the USB (if connected) and recognize commands from host
        // So you don't have to reset SPOT to deploy new code on it.

        scheduler = new Scheduler();
        //scheduler.setPriority(4);
        scheduler.start();
        eventHandler = new EventHandler(scheduler, this);
        MAC = new IEEEAddress(Spot.getInstance().getRadioPolicyManager().getIEEEAddress()).asDottedHex();
        com = new Communications(MAC);
        com.registerListener(eventHandler);        
    }
    
    public synchronized void showLightreadings(int value) {
    	int intensity = (int)(2/175.0*value);
    	
		/**
	     * Den har verdier fra 0-730. Dette var et tips jeg fikk fra internett.
	     * Man deler 2 p� 175 og ganger med average, da f�r man en verdi fra 0-8.
	     * Dette kan da representere intensiteten og skru p� de tilh�rende ledsene.
	     */
		for(int i = 0; i<intensity; i++) {
			leds[i].setColor(LEDColor.RED);
			leds[i].setOn();
		}
		for(int j=intensity; j<8; j++) {
			leds[j].setColor(LEDColor.GREEN);
			leds[j].setOn();
		}
    }
    
    public void blinkLEDsDynamic(LEDColor color, long gap1, long gap2, int blinks){
    	if (SunSpotApplication.output) {	
    		System.out.println("Blinking LEDS!");
		}
    	for(int i = 0; i < leds.length; i++) {
    		leds[i].setColor(color);
    	}
    	for(int i = 0; i < blinks; i++) {
    		setLEDsOnDynamic(gap2);
    		long time = System.currentTimeMillis();
    		while (System.currentTimeMillis() < time+gap1) {	}
    		time = System.currentTimeMillis();
    		setLEDsOffDynamic(gap2);
    		while (System.currentTimeMillis() < time+gap1) {	}
    	}
    }
    
    protected void pauseApp() {
        // This will never be called by the Squawk VM
    }
    
    /**
     * Called if the MIDlet is terminated by the system.
     * I.e. if startApp throws any exception other than MIDletStateChangeException,
     * if the isolate running the MIDlet is killed with Isolate.exit(), or
     * if VM.stopVM() is called.
     * 
     * It is not called if MIDlet.notifyDestroyed() was called.
     *
     * @param unconditional If true when this method is called, the MIDlet must
     *    cleanup and release all resources. If false the MIDlet may throw
     *    MIDletStateChangeException  to indicate it does not want to be destroyed
     *    at this time.
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    	
    	
    }
    
    private void setLEDsOffDynamic(long gap) {
    	for (int i = 0; i < leds.length; i++) {	
    		leds[i].setOff();
    		long time = System.currentTimeMillis();
    		while (System.currentTimeMillis() < time+gap) {	}
    	}
    }
    
    private void setLEDsOnDynamic(long gap) {
    	for (int i = 0; i < leds.length; i++) {		
    		leds[i].setOn();
    		long time = System.currentTimeMillis();
    		while (System.currentTimeMillis() < time+gap) {	}
    	}
    }
    
}

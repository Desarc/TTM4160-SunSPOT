/*
* Copyright (c) 2006 Sun Microsystems, Inc.
* 
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to 
* deal in the Software without restriction, including without limitation the 
* rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
* sell copies of the Software, and to permit persons to whom the Software is 
* furnished to do so, subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be included in 
* all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
* DEALINGS IN THE SOFTWARE.
 **/       
package org.sunspotworld.demo;

/*
 * LEDSampleCode.java
 *
 * Some simple uses of the LEDs on the Sun SPOT.
 *
 * author: Ron Goldman  
 * date: August 14, 2006 
 */

import java.io.IOException;
import java.util.*;

import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.ILightSensor;
import com.sun.spot.sensorboard.peripheral.ITriColorLED;
import com.sun.spot.sensorboard.peripheral.LEDColor;
import com.sun.spot.sensorboard.peripheral.LightSensor;
import com.sun.spot.util.Utils;
import com.sun.squawk.util.Arrays;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * Sample code snippet to show how to use the tricolor LEDs
 * on the Sun SPOT General Purpose Sensor Board.
 *
 * @author Ron Goldman
 */
public class LEDSampleCode extends MIDlet {

    private ITriColorLED [] leds = EDemoBoard.getInstance().getLEDs();
    private ILightSensor sensor = EDemoBoard.getInstance().getLightSensor();
    
    int intensity = 0;

    public void demoLEDs() {
    	
        for (int i = 0; i < 8; i++) {
        	leds[i].setColor(LEDColor.GREEN);
        }
       
        while (true) {
        	try {
        		/**
        	     * Den har verdier fra 0-730. Dette var et tips jeg fikk fra internett.
        	     * Man deler 2 på 175 og ganger med average, da får man en verdi fra 0-8.
        	     * Dette kan da representere intensiteten og skru på de tilhørende ledsene.
        	     */
				intensity =(int)(2/175.0*sensor.getAverageValue());
				for(int i = 0; i<intensity; i++) {
					leds[i].setColor(LEDColor.RED);
					leds[i].setOn();
				}
				for(int j=intensity; j<8; j++) {
					leds[j].setColor(LEDColor.GREEN);
					leds[j].setOn();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        	/**
             * Simple LED demo that repeats forever.
             *
             * First, blink the leftmost LED on & off 5 times. 
             * Second, have a moving lit LED sweep from left to right.
             * Third, pulse one LED from dim to bright, repeat 3 times.
             */
            // first demo - blink LED 0 on & off 5 times
//            leds[0].setColor(LEDColor.BLUE);    // set it to one of the predefined colors
//            for (int i = 0; i < 5; i++ ) {
//                leds[0].setOn();
//                Utils.sleep(250);               // on for 1/4 second
//                leds[0].setOff();
//                Utils.sleep(750);               // off for 3/4 second
//            }
//
//            // second demo - move the lit LED - go from LED 0 to LED 7
//            for (int i = 0; i < 8; i++) {
//                leds[i].setColor(LEDColor.MAGENTA);
//                leds[i].setOn();
//                Utils.sleep(200);               // on for 1/5 second
//                leds[i].setOff();
//            }
//
//            // third demo - pulse LED 3 so it gets brighter - do so 3 times
//            for (int i = 0; i < 3; i++) {
//                leds[3].setRGB(0, 0, 0);	// start it off dim
//                leds[3].setOn();
//                Utils.sleep(100);
//                for (int j = 0; j < 255; j += 5) {
//                    leds[3].setRGB(j, 0, 0);	// make it get brighter red
//                    Utils.sleep(50);	        // change every 1/20 second
//                }
//            }
//            leds[3].setOff();
        }
    }

    /**
     * MIDlet call to start our application.
     */
    protected void startApp() throws MIDletStateChangeException {
        demoLEDs();
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
        for (int i = 0; i < 8; i++) {
            leds[i].setOff();
        }
    }
}

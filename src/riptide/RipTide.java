


/*
 * This is #RipTide, Because at some point the waves need to get pulled back in...
 * 
 * This app was written in Proclipsing although can be modified for both standalone Eclipse and Processing
 * Proclipsing can be found here: https://code.google.com/p/proclipsing/
 * 
 * The point of this applet is to: 
 * -Provide multiple easy to use methods to acquire Mindwave Mobile Data
 * -Provide OSC and JSON output
 * -Broadcast the OSC output for programs like Supercollider and Max MSP
 * -Work around having to pay money to develop for the blue toothiness
 * 
 * (c) 2013
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 *  This library follows some of the same designs of Jorge C. S. Cordoso (his lib included in repo, unused)
 *  See http://jorgecardoso.eu/processing/MindSetProcessing/
 *  That library was made for for the NeuroSky MindSet, which allows for easy parsing in Java.
 *  MindSetProcessing was modified by Andreas Borg  (https://github.com/borg/ThinkGear-Java-socket) 
 *  and implemented by github.com/haiyan who gave me the idea to do this through processing
 *  
 *  The above people and many others made this possible, thank you to everyone including NeuroSky and http://grand.st 
 *  
 *  Programs for the People, MindWaves for the Masses
 *   
*/

package riptide;

import java.net.*;

import processing.*;
import processing.core.PApplet;
import processing.dxf.*;
import processing.net.*;

//import com.NeuroSky.*;

import neurosky.*;

import oscP5.*;
import netP5.*;

import pt.citar.*;
import pt.citar.diablu.processing.mindset.MindSet;

import org.json.*;

public class RipTide extends PApplet {

	Server myServer;
	
	//Important variables
	int appWidth = 400;
	int appHeight = 200;
	int fps = 60;
	
	int _jsonPort = 32000;
	int _oscPort = 5204;
	
	//These guys are used to connect to the thinkgear connector
	int _TGPort = 13854; //This is the default TG Port
	String localHost = "127.0.0.1"; //Default local host
	
	
	ThinkGearSocket mindWave; //To Connect Through Andreas Borg's updated Java Socket
	Client mindWaveClient; //To Retrieve the data in a JSON stream, 
	
	int delta, theta, low_alpha, high_alpha, low_beta, high_beta, low_gamma, mid_gamma;
	int attention,meditation;
	
	boolean isConnected = false;
	
	//Mindwave Setup Functions
	public void setupTGSocket(){// To use the Think Gear Socket Class Methods
		 ThinkGearSocket mindWave = new ThinkGearSocket(this);
		 try {
			 mindWave.start();
		  }
		  catch (ConnectException c) {
			  println("Make sure ThinkGear is running and Mindwave Mobile Connected");
		  }
	}
	
	public void setupJSONClient(){ //To collect JSON data directly from the mindWave
		//if(mindWave.isRunning()){//The thinkgear socket calls for JSON format, so if it is running we don't need to tell Mindwave to format JSON
			mindWaveClient = new Client(this, localHost, _TGPort);
		//}
		//else{
			//Make sure your ThinkGear server is running bro
		//	mindWaveClient = new Client(this, localHost, _TGPort);
			mindWaveClient.write("{\"enableRawOutput\": false, \"format\": \"Json\"}"); //This tells ThinkGear NO raw data, Format to JSON
		//}
	}
	
	public void setupOSC(){
		
	}
	
	//Processing calls
	public void setup() {
		 size(appWidth, appHeight);
		 frameRate(fps);
		 setupTGSocket();
		 setupJSONClient();
		 
		 // Starts a myServer on port 5204
		 myServer = new Server(this, _oscPort); 
		 NetAddress myBroadcastLocation = new NetAddress("127.0.0.1",32000);

	}
	public void draw() {
		 //println(mindWave.toString());
		 int val = 0;
		 val = (val + 1) % 255;
		 background(val);
		 myServer.write(val);
		 printJSON();
	}
	
	public void stop(){
		myServer.stop();
		mindWave.stop();
		super.stop();
	}
	
	//Think Gear Socket Public Class Methods. ONLY AVAILABLE IF ThinkGearSocket mindWave IS INITIALIZED
	public void eegEvent(int delta, int theta, int low_alpha, int high_alpha, int low_beta, int high_beta, int low_gamma, int mid_gamma) 
	{
		
	}
	
	public void poorSignalEvent(int sig) {
		if(sig < 50){
			println("Currently connecting or Disconnected. You wearing your headset bro?");
		}
		if (sig < 50) {
			isConnected = true;
		}
	}

	public void attentionEvent(int attentionLevel) {
	  println("Attention Level: " + attentionLevel);
	  attention = attentionLevel;
	}

	void meditationEvent(int meditationLevel) {
	  println("Meditation Level: " + meditationLevel);
	  meditation = meditationLevel;
	}

	void blinkEvent(int blinkStrength) {
		println("blinkStrength: " + blinkStrength);
	}
	
	//JSON Methods
	public void printJSON(){
		if (mindWaveClient.available() > 0) {
			println(mindWaveClient.readString());
		}
	}

}

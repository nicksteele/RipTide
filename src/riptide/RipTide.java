


/*
 * This is #RipTide, Because at some point the waves need to get pulled back in...
 * 
 * This app was written in Proclipsing although can be modified for both standalone Eclipse and Processing
 * Proclipsing can be found here: https://code.google.com/p/proclipsing/
 * 
 * The point of this applet is to: 
 * -Provide multiple easy to use methods to acquire Mindwave Mobile Data
 * -Provide OSC and JSON output options.
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
 *  The OSC classes are provided by http://www.sojamo.de/oscP5 and Andreas Schlegel helped me figure them out
 *  
 *  The above people and many others made this possible, thank you to everyone including NeuroSky and http://grand.st 
 *  
 *  Programs for the People, MindWaves for the Masses
 *  
 *  Yours,
 *  nick@grandst.com
 *  (github.com/nicksteele)
 *   
*/

package riptide;

import java.net.*;

import java.util.*;

import processing.*;
import processing.core.PApplet;
import processing.core.PFont;
import processing.dxf.*;
import processing.net.*;

import neurosky.*;

import oscP5.*;
import netP5.*;

import pt.citar.*;
import pt.citar.diablu.processing.mindset.MindSet;

import org.json.*;

public class RipTide extends PApplet {

	//Display Variables, used to draw levels
	int attLevel;
	int medLevel;
	int blinkLevel;
	int sigLevel;
	PFont font;
	
	
	//Important variables
	int appWidth = 500;
	int appHeight = 300;
	int fps = 60;
	
	int _oscPort = 12000; //this is our broadcast port for osc
	int _oscListenPort = 12000; //So we can listen to ourselves
	
	//This is used to access the OSC send/receive methods
	OscP5 oscP5;
	OscBundle myBundle;
	//This Net Address takes two params, ip and port, the variable is used to send OSC 
	//packs via oscP5.send() and can be sent to other comps, devices, and apps.
	NetAddress myBroadcastLocation;
	
	HashMap oscData;
	
	//These guys are used to connect to the thinkgear connector
	int _TGPort = 13854; //This is the default TG Port - Remember this guy!
	String localHost = "127.0.0.1"; //Default local host
	
	ThinkGearSocket mindWave; //To Connect Through Andreas Borg's updated Java Socket
	Client mindWaveClient; //To Retrieve the data in a JSON stream, 
	
	int delta, theta, lowAlpha, highAlpha, lowBeta, highBeta, lowGamma, highGamma;//Parsed by parseJSON
	int attention,meditation; //ParseJSON
	int blinkStrength;
	
	boolean isConnected = false;//Unused but useful variable to have
	
	//Debug
	boolean displayJSON = true;//Display JSON in Console
	boolean displayTGS = false;//Display eegEvent data
	boolean displayOSC = false;//Display OSC Broadcast
	boolean displaySig = true;//Display Signal Strength
	boolean displayErrors = false;//Display Catch Errors
	
	//Decisions!
	boolean inactiveBroadcastMethod = false;
	boolean activeBroadcastMethod = true;
	
	boolean getOscFromJSON = inactiveBroadcastMethod;
	boolean getOscFromTGS = activeBroadcastMethod;
	
	//Mindwave Setup Functions
	public void setupTGSocket(){// To use the Think Gear Socket Class Methods
		 ThinkGearSocket mindWave = new ThinkGearSocket(this);
		 try {
			 mindWave.start();
		  }
		  catch (ConnectException c) {
			  if(displayErrors){
			  	println("Make sure ThinkGear is running and Mindwave Mobile Connected");
			  }
		  }
	}
	
	public void setupJSONClient(){ //To collect JSON data directly from the mindWave
		//ThinkGearSocket calls for JSON format, but let's call it just to be safe
		mindWaveClient = new Client(this, localHost, _TGPort); //This is the location of the mindwave mobile server, see values above!!
		mindWaveClient.write("{\"enableRawOutput\": false, \"format\": \"Json\"}"); //This tells ThinkGear NO raw data, Format to JSON
	}
	
	public void setupOSCBroadcast(){
		 myBroadcastLocation = new NetAddress(localHost,_oscPort); //Broadcast our OSC to this local port
	}
	
	public void setupOSCReceive(){
		//oscP5 starts listening for the  OSC data (in this case, for this PApplet) on port _oscListenPort.
		oscP5 = new OscP5(this,_oscListenPort);
	}
	
	//Processing calls
	
	public void keyPressed() {
		if (key == 'x') {
			stop();
		}
	}
	
	public void setup() {
		 size(appWidth, appHeight);
		 frameRate(fps);
		 smooth();
		 
		 font = createFont("Helvetica", 16);
		 textFont(font);
		  
		 setupOSCBroadcast();
		 setupOSCReceive();
		 
		 setupTGSocket();
		 
		 setupJSONClient();
		 

	}
	public void draw() {
		background (244);
		fill(0);
		smooth();
		text("OSC: Connected to " + localHost + " on port " + _oscPort, 100 , 80);
		text("OSC: Listening to " + localHost + " on port " + _oscListenPort, 100 , 100);
		if(getOscFromJSON){
			text("Getting OSC from JSON output", 100 , 60);
		}
		if(getOscFromTGS){
			text("Getting OSC from Thinkgear Socket output", 100 , 60);
		}
		if(isConnected){
			background (244);
			text("OSC: Connected to " + localHost + " on port " + _oscPort, 100 , 80);
			text("OSC: Listening to " + localHost + " on port " + _oscListenPort, 100 , 100);
			text("Connected. Strength = " + sigLevel, 100 , 40);
			text("Attention :" + attLevel + " Meditation " + meditation, 100 , 200);
		}
		else {text("Connecting", 100 , 40);}
		
		if (mindWaveClient.available() > 0) { //If we're receiveing bytes, read stream
			parseJSON(mindWaveClient.readString());
		}
	}
	
	public void stop(){
		//mindWave.stop();
		super.stop();
	}
	
	//OSC Methods
	
	public void broadcastBlink(int blinkStrength){ //For TGS Broadcast
		myBundle = new OscBundle();
		OscMessage oscMsg = new OscMessage("/BlinkEvent");
		oscMsg.add(blinkStrength);
		myBundle.add(oscMsg);
		myBundle.setTimetag(myBundle.now() + 10000);
		oscP5.send(myBundle,myBroadcastLocation);
	}
	
	public void broadcastAtt(int attention){ //For TGS Broadcast
		myBundle = new OscBundle();
		OscMessage oscMsg = new OscMessage("/AttEvent");
		oscMsg.add(attention);
		myBundle.add(oscMsg);
		myBundle.setTimetag(myBundle.now() + 10000);
		oscP5.send(myBundle,myBroadcastLocation);
	}
	
	public void broadcastMed(int meditation){ //For TGS Broadcast
		myBundle = new OscBundle();
		OscMessage oscMsg = new OscMessage("/MedEvent");
		oscMsg.add(meditation);
		myBundle.add(oscMsg);
		myBundle.setTimetag(myBundle.now() + 10000);
		oscP5.send(myBundle,myBroadcastLocation);
	}
	
	public void broadcastOSC(HashMap oscData){
		Iterator i = oscData.entrySet().iterator();
		while(i.hasNext()){
			Map.Entry wave = (Map.Entry)i.next();
			OscMessage oscMsg = new OscMessage("/"+wave.getKey());
			oscMsg.add(Integer.parseInt(wave.getValue().toString()));
			oscP5.send(oscMsg,myBroadcastLocation);
			oscMsg.clear();
		}
	}
	//broadcast by Bundle
	public void sendOscFromJSON(HashMap oscData){ //For when we send a broadcast from JSON
		
		//We could broadcast by bundle if we wanted to
		myBundle = new OscBundle();
		Iterator i = oscData.entrySet().iterator();
		
		OscMessage oscMsg = new OscMessage("/MindwaveOutput");
		oscMsg.add("JSONoutput");
		myBundle.add(oscMsg);
		oscMsg.clear();
		
		while(i.hasNext()){
			Map.Entry wave = (Map.Entry)i.next();
			oscMsg.setAddrPattern("/"+wave.getKey());
			oscMsg.add(Integer.parseInt(wave.getValue().toString()));
			myBundle.add(oscMsg);
		}
		
		myBundle.setTimetag(myBundle.now() + 10000);
		oscP5.send(myBundle,myBroadcastLocation);
	}
	//broadcast by Bundle
	public void sendOscFromTGS(HashMap oscData){ //from TGS eegEvent()
		myBundle = new OscBundle();
		Iterator i = oscData.entrySet().iterator();
		
		OscMessage oscMsg = new OscMessage("/MindwaveOutput");
		oscMsg.add("TGSOutput");
		myBundle.add(oscMsg);
		
		while(i.hasNext()){
			Map.Entry wave = (Map.Entry)i.next();
			oscMsg.clear();
			oscMsg.setAddrPattern("/"+wave.getKey());
			oscMsg.add(Integer.parseInt(wave.getValue().toString()));
			myBundle.add(oscMsg);
		}
		
		myBundle.setTimetag(myBundle.now() + 10000);
		oscP5.send(myBundle,myBroadcastLocation);
	}
	
	public void oscEvent(OscMessage theOscMessage) {
		/* print the address pattern and the typetag of the received OscMessage */
		if(displayOSC){
			print("### received an osc message.");
			print(" addrpattern: "+theOscMessage.addrPattern());
			print(" typetag: "+theOscMessage.typetag());
			println(" timetag: "+theOscMessage.timetag());
			theOscMessage.printData();
		}
	}
	
	//Think Gear Socket Public Class Methods. ONLY AVAILABLE IF ThinkGearSocket mindWave IS INITIALIZED
	public void eegEvent(int delta, int theta, int low_alpha, int high_alpha, int low_beta, int high_beta, int low_gamma, int mid_gamma) 
	{
		//This only gets us the main eeg data, so it's only kind of useful. Getting the JSON is better since it has more objects
		if(getOscFromTGS){
			oscData = new HashMap();
			oscData.put("delta", delta);
			oscData.put("theta", theta);
			oscData.put("lowAlpha", low_alpha);
			oscData.put("highAlpha", high_alpha);
			oscData.put("lowBeta", low_beta);
			oscData.put("highBeta", high_beta);
			oscData.put("lowGamma", low_gamma);
			oscData.put("highGamma", mid_gamma);
			
			//sendOscFromTGS(oscData);
			broadcastOSC(oscData);
		}
	}
	
	public void poorSignalEvent(int sig) {
		sigLevel = sig;
		if(sig == 200 && displaySig){
			println("Connecting... (" + sig + ")");
		}
		if (sig < 50) {
			isConnected = true;
		}
	}

	public void attentionEvent(int attentionLevel) {
		//println("Attention Level: " + attentionLevel);
		attLevel = attentionLevel;
		if(getOscFromTGS){
			broadcastAtt(attentionLevel);
		}
	}

	void meditationEvent(int meditationLevel) { //TGSocket is poopy at printing this
		println("Meditation Level: " + meditationLevel);
		medLevel = meditationLevel;
		if(getOscFromTGS){
			broadcastMed(meditationLevel);
		}
	}

	void blinkEvent(int blinkStrength) {
		//println("blinkStrength: " + blinkStrength);
		blinkLevel = blinkStrength;
		if(getOscFromTGS){
			broadcastBlink(blinkStrength);
		}
	}
	
	//JSON Methods
	public void printJSON(){
		if (mindWaveClient.available() > 0) { //If we're receiveing bytes, read stream
			println(mindWaveClient.readString());
			String dataIn = mindWaveClient.readString();
			parseJSON(dataIn);
		}
	}
	
	public void parseJSON(String dataIn){ //Parses to corresponding values and adds them to a HashMap for OSC output
		try {
			//parse JSON object from dataIn string
			JSONObject headsetData = new JSONObject(dataIn);
			//parse individual datasets from main JSON object
			JSONObject results = headsetData.getJSONObject("eegPower"); //eegPower dataset
			JSONObject resultsM = headsetData.getJSONObject("eSense"); //eSense dataset
			
			//parse rawEeg data, need to change drivers mode to enable this
			//JSONObject rawData = nytData.getJSONObject("rawEeg");
			//parse blink data. also off by default.
			JSONObject resultsB = headsetData.getJSONObject("blinkStrength");
			//pull individual values from eSense and eegPower JSON objects
			//this is the eegPower stuff
			delta = results.getInt("delta");
			theta = results.getInt("theta");
			lowAlpha = results.getInt("lowAlpha");
			highAlpha = results.getInt("highAlpha");
			lowBeta = results.getInt("lowBeta");
			highBeta = results.getInt("highBeta");
			lowGamma = results.getInt("lowGamma");
			highGamma = results.getInt("highGamma");
			
			//this is the eSense stuff
			attention = resultsM.getInt("attention");
			meditation = resultsM.getInt("meditation");

			//this is the blinkStrength
			blinkStrength = resultsB.getInt("blinkStrength");
			if(getOscFromJSON) broadcastBlink(blinkStrength);
			}
			catch (JSONException e) {
				if (displayErrors) {
					println ("There was an error parsing the JSONObject.");
					println(e);
				}
			}
		if(getOscFromJSON){
			oscData = new HashMap();
			oscData.put("delta", delta);
			oscData.put("theta", theta);
			oscData.put("lowAlpha", lowAlpha);
			oscData.put("highAlpha", highAlpha);
			oscData.put("lowBeta", lowBeta);
			oscData.put("highBeta", highBeta);
			oscData.put("lowGamma", lowGamma);
			oscData.put("highGamma", highGamma);
			
			oscData.put("attention", attention);
			oscData.put("meditation", meditation);
			
			//sendOscFromJSON(oscData);
			broadcastOSC(oscData);
		}
	}

}

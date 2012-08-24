/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2012  Karl-Peter Fuchs
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package evaluation.loadGenerator.fixedSchedule;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import evaluation.loadGenerator.ClientTrafficScheduleWriter;
import evaluation.loadGenerator.applicationLevelTraffic.requestReply.ALRR_ReplyReceiver;
import evaluation.loadGenerator.applicationLevelTraffic.requestReply.ApplicationLevelMessage;
import evaluation.loadGenerator.applicationLevelTraffic.requestReply.ALRR_BasicWriter;
import evaluation.loadGenerator.applicationLevelTraffic.requestReply.ALRR_ClientWrapper;
import evaluation.loadGenerator.applicationLevelTraffic.requestReply.EndOfFileReachedException;
import evaluation.loadGenerator.scheduler.ScheduleTarget;
import evaluation.loadGenerator.scheduler.Scheduler;
import framework.core.AnonNode;
import framework.core.config.Paths;
import framework.core.config.Settings;
import framework.core.launcher.ToolName;
import framework.core.routing.RoutingMode;
import framework.core.socket.socketInterfaces.AnonSocketOptions.CommunicationMode;


public class ALM_FS_Tracefile implements ClientTrafficScheduleWriter<ApplicationLevelMessage> {

	private Settings settings;
	private ScheduleTarget<ApplicationLevelMessage> scheduleTarget;
	private HashMap<Integer, ALRR_ClientWrapper> clientReferences;
	private ALRR_ClientWrapper[] clientsArray;
	private long sumOfPlanedDelays = 0; // in nanosec
	private long experimentStart; // in nanosec
	private AnonNode client;
	private BufferedReader traceReader;
	private int lineCounter = 0;
	private ALRR_ReplyReceiver replyReceiver;
	
	
	public ALM_FS_Tracefile(AL_FixedScheduleLoadGenerator owner) {
		this.settings = owner.getSettings();
		
		this.experimentStart = owner.getScheduler().now() + TimeUnit.SECONDS.toNanos(2);
		System.out.println("LOAD_GENERATOR: start at " +experimentStart);
		
		// try to load trace-file:
		System.out.println("TRACE_READER: loading trace file"); 
		String traceFilePath = Paths.getProperty("LG_TRACE_FILE_PATH") + settings.getProperty("AL-TRACE_FILE-NAME");
		resetReader(traceFilePath);
				
		// create client
		owner.getLoadGenerator().commandLineParameters.gMixTool = ToolName.CLIENT;
		this.client = new AnonNode(owner.getLoadGenerator().commandLineParameters);
		this.scheduleTarget = new ALRR_BasicWriter(this, client.IS_DUPLEX);
		
		// determine number of clients and lines; create ClientWrapper objects:
		this.clientReferences = new HashMap<Integer, ALRR_ClientWrapper>(1000); // TODO: dynamic
		String line;
		try {
			while ((line = traceReader.readLine()) != null) {
				Integer id = Integer.parseInt(line.split("(,|;)")[1]);
				ALRR_ClientWrapper cw = clientReferences.get(id);
				if (cw == null)
					clientReferences.put(id, new ALRR_ClientWrapper(id));	
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("TRACE_READER: error traversing trace file"); 
		}
		System.out.println("TRACE_READER: found traces for " +clientReferences.size() +" clients in the trace file");
		resetReader(traceFilePath);
				
		// create array etc.:
		this.clientsArray = new ALRR_ClientWrapper[clientReferences.size()];
		int i = 0;
		for (ALRR_ClientWrapper cw: clientReferences.values()) {
			clientsArray[i++] = cw;
			//cw.requestQueue = new ArrayBlockingQueue<ArrayIterator<ApplicationLevelMessage>>(3);
			//if (settings.getPropertyAsBoolean("GLOBAL_IS_DUPLEX"))
			//	cw.replyQueue = new ConcurrentLinkedQueue<ApplicationLevelMessage> ();
		} 
						
		// generate and connect sockets
		CommunicationMode cm = client.IS_DUPLEX ? CommunicationMode.DUPLEX : CommunicationMode.SIMPLEX_SENDER;
		for (ALRR_ClientWrapper cw: clientsArray) // generate sockets
			cw.socket = client.createStreamSocket(cm, client.ROUTING_MODE != RoutingMode.CASCADE);
		// connect sockets:
		int port = settings.getPropertyAsInt("SERVICE_PORT1");
		System.out.println("LOAD_GENERATOR: connecting clients..."); 
		for (ALRR_ClientWrapper cw: clientsArray) 
			try {
				cw.socket.connect(port);
				cw.outputStream = new BufferedOutputStream(cw.socket.getOutputStream());
				if (client.IS_DUPLEX)
					cw.inputStream = cw.socket.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (client.IS_DUPLEX) {
			this.replyReceiver = new ALRR_ReplyReceiver(clientsArray, settings);
			//this.replyReceiver.registerObserver(this);
			this.replyReceiver.start();
		}	
	}
	
	
	@Override
	public boolean scheduleRecords(int numberOfRecords, Scheduler<ApplicationLevelMessage> scheduler) {
		ApplicationLevelMessage currentEntry = null;
		for (int i=0; i<numberOfRecords; i++) {
			try {
				currentEntry = new ApplicationLevelMessage(traceReader, ++lineCounter);
			} catch (EndOfFileReachedException e) {
				lineCounter--;
				return false;
			}
			sumOfPlanedDelays += currentEntry.getSendDelayInNanoSec();
			long timeToSend = experimentStart + sumOfPlanedDelays;
			currentEntry.setPlanedSendTime(timeToSend);
			scheduler.executeAt(
					currentEntry.getPlanedSendTime(),
					scheduleTarget,
					currentEntry
					);
		} 
		return true;
	}

	
	@Override
	public ALRR_ClientWrapper getClientWrapper(int identifier) {
		return clientReferences.get(identifier);
	}
	
	
	private void resetReader(String traceFilePath) {
		try {
			this.traceReader = new BufferedReader(new FileReader(traceFilePath));
		} catch (FileNotFoundException e) {
			System.err.println("TRACE_READER: could not find trace file: " + traceFilePath);
		}
	}
	
}

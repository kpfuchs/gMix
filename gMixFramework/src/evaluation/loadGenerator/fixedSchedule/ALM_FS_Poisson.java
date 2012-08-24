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
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math.random.RandomDataImpl;

import evaluation.loadGenerator.ClientTrafficScheduleWriter;
import evaluation.loadGenerator.applicationLevelTraffic.requestReply.ALRR_ReplyReceiver;
import evaluation.loadGenerator.applicationLevelTraffic.requestReply.ApplicationLevelMessage;
import evaluation.loadGenerator.applicationLevelTraffic.requestReply.ALRR_BasicWriter;
import evaluation.loadGenerator.applicationLevelTraffic.requestReply.ALRR_ClientWrapper;
import evaluation.loadGenerator.randomVariable.FakeRandom;
import evaluation.loadGenerator.randomVariable.RandomVariable;
import evaluation.loadGenerator.scheduler.ScheduleTarget;
import evaluation.loadGenerator.scheduler.Scheduler;
import framework.core.AnonNode;
import framework.core.config.Settings;
import framework.core.launcher.ToolName;
import framework.core.routing.RoutingMode;
import framework.core.socket.socketInterfaces.AnonSocketOptions.CommunicationMode;


public class ALM_FS_Poisson implements ClientTrafficScheduleWriter<ApplicationLevelMessage> {

	private Settings settings;
	private ScheduleTarget<ApplicationLevelMessage> scheduleTarget;
	private ALRR_ClientWrapper[] clientsArray;
	private long experimentStart; // in nanosec
	private long startOfPeriod; // in nanosec
	private AnonNode client;
	private ALRR_ReplyReceiver replyReceiver;
	private RandomVariable REQUEST_PAYLOAD_SIZE;
	private RandomVariable REPLY_PAYLOAD_SIZE;
	private RandomVariable AVG_SENDS_PER_PERIOD;
	private long PULSE_LENGTH; // in ns
	private RandomVariable REPLY_DELAY; // in seconds
	private RandomDataImpl randomDataImpl;
	private SecureRandom random;
	
	
	public ALM_FS_Poisson(AL_FixedScheduleLoadGenerator owner) {
		this.settings = owner.getSettings();
		this.experimentStart = owner.getScheduler().now() + TimeUnit.SECONDS.toNanos(2);
		this.startOfPeriod = experimentStart;
		int numberOfClients = settings.getPropertyAsInt("AL-POISSON-NUMBER_OF_CLIENTS");
		String str_avgSendsPerPulse = settings.getProperty("AL-POISSON-AVERAGE_SEND_OPERATIONS_PER_PULSE");
		if (RandomVariable.isRandomVariable(str_avgSendsPerPulse)) {
			this.AVG_SENDS_PER_PERIOD = RandomVariable.createRandomVariable(str_avgSendsPerPulse);
		} else {
			float float_avgSendsPerPulse = Float.parseFloat(str_avgSendsPerPulse);
			float_avgSendsPerPulse = float_avgSendsPerPulse * (float)numberOfClients;
			if (float_avgSendsPerPulse < 1f)
				this.AVG_SENDS_PER_PERIOD = new FakeRandom(1);
			else
				this.AVG_SENDS_PER_PERIOD = new FakeRandom(Math.round(float_avgSendsPerPulse));
		}
		String str_ReplyDelay = settings.getProperty("AL-POISSON-REPLY_DELAY");
		if (RandomVariable.isRandomVariable(str_ReplyDelay))
			this.REPLY_DELAY = RandomVariable.createRandomVariable(str_ReplyDelay);
		else
			this.REPLY_DELAY = new FakeRandom(Double.parseDouble(str_ReplyDelay));
		this.PULSE_LENGTH = (long) (settings.getPropertyAsFloat("AL-POISSON-PULSE_LENGTH")*1000000000f);
		this.random = new SecureRandom();
		this.randomDataImpl = new RandomDataImpl();
		this.randomDataImpl.reSeed(this.random.nextLong());
		System.out.println("LOAD_GENERATOR: start at " +experimentStart);
		// create client
		owner.getLoadGenerator().commandLineParameters.gMixTool = ToolName.CLIENT;
		this.client = new AnonNode(owner.getLoadGenerator().commandLineParameters);
		this.scheduleTarget = new ALRR_BasicWriter(this, client.IS_DUPLEX);
		// determine number of clients and lines; create ClientWrapper objects etc
		this.clientsArray = new ALRR_ClientWrapper[numberOfClients];
		CommunicationMode cm = client.IS_DUPLEX ? CommunicationMode.DUPLEX : CommunicationMode.SIMPLEX_SENDER;
		int port = settings.getPropertyAsInt("SERVICE_PORT1");
		System.out.println("LOAD_GENERATOR: connecting clients..."); 
		for (int i=0; i<numberOfClients; i++) {
			clientsArray[i] = new ALRR_ClientWrapper(i);
			clientsArray[i].socket = client.createStreamSocket(cm, client.ROUTING_MODE != RoutingMode.CASCADE);
			try {
				clientsArray[i].socket.connect(port);
				clientsArray[i].outputStream = new BufferedOutputStream(clientsArray[i].socket.getOutputStream());
				if (client.IS_DUPLEX)
					clientsArray[i].inputStream = clientsArray[i].socket.getInputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}String str_requestPayloadSize = settings.getProperty("AL-POISSON-REQUEST_PAYLOAD_SIZE");
		if (RandomVariable.isRandomVariable(str_requestPayloadSize)) {
			this.REQUEST_PAYLOAD_SIZE = RandomVariable.createRandomVariable(str_requestPayloadSize);
		} else {
			if (str_requestPayloadSize.equalsIgnoreCase("AUTO"))
				this.REQUEST_PAYLOAD_SIZE = new FakeRandom(clientsArray[0].socket.getMTU());
			else
				this.REQUEST_PAYLOAD_SIZE = new FakeRandom(Integer.parseInt(str_requestPayloadSize));
		}
		String str_replyPayloadSize = settings.getProperty("AL-POISSON-REPLY_PAYLOAD_SIZE");
		if (RandomVariable.isRandomVariable(str_replyPayloadSize)) {
			this.REPLY_PAYLOAD_SIZE = RandomVariable.createRandomVariable(str_replyPayloadSize);
		} else {
			if (str_replyPayloadSize.equalsIgnoreCase("AUTO"))
				this.REPLY_PAYLOAD_SIZE = new FakeRandom(clientsArray[0].socket.getMTU());
			else
				this.REPLY_PAYLOAD_SIZE = new FakeRandom(Integer.parseInt(str_replyPayloadSize));
		}
		if (client.IS_DUPLEX) {
			this.replyReceiver = new ALRR_ReplyReceiver(clientsArray, settings);
			//this.replyReceiver.registerObserver(this);
			this.replyReceiver.start();
		}	
	}
	
	
	@Override
	public boolean scheduleRecords(int numberOfRecords, Scheduler<ApplicationLevelMessage> scheduler) {
		int periods = (int) (numberOfRecords / AVG_SENDS_PER_PERIOD.drawIntSample());
		if (periods == 0)
			periods = 1;
		int ctr = 0;
		ApplicationLevelMessage currentEntry;
		for (int j=0; j<periods; j++) { // for each period
			long requestsInThisPeriod = randomDataImpl.nextPoisson(AVG_SENDS_PER_PERIOD.drawIntSample());
			long[] delays = new long[(int) requestsInThisPeriod];
			for (int l=0; l<delays.length; l++) // distribute delays at (uniform) random (for this period)
				delays[l] = (long) (random.nextDouble() * PULSE_LENGTH);
			Arrays.sort(delays);
			for (int l=0; l<delays.length; l++) {
				ctr++;
				// choose client to send this request at (uniform) random:
				int clientId = (int) Math.round(random.nextDouble() * (double)(clientsArray.length-1)); 
				currentEntry = new ApplicationLevelMessage(
						((float)delays[l]/1000000000f),
						clientId,
						clientId,
						REQUEST_PAYLOAD_SIZE.drawIntSample(),
						REPLY_PAYLOAD_SIZE.drawIntSample(),
						(float) REPLY_DELAY.drawDoubleSample()
						);
				long timeToSend = startOfPeriod + delays[l];
				currentEntry.setPlanedSendTime(timeToSend);
				scheduler.executeAt(
						currentEntry.getPlanedSendTime(),
						scheduleTarget,
						currentEntry
						);
			}
			startOfPeriod += PULSE_LENGTH;
		} 
		System.out.println("ALM_Poisson: scheduled " +ctr +" requests"); 
		return true;
	}

	
	@Override
	public ALRR_ClientWrapper getClientWrapper(int identifier) {
		return clientsArray[identifier];
	}
	
}
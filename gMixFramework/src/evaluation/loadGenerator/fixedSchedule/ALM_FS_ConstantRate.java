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
import java.util.concurrent.TimeUnit;

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


public class ALM_FS_ConstantRate implements ClientTrafficScheduleWriter<ApplicationLevelMessage> {

	private Settings settings;
	private ScheduleTarget<ApplicationLevelMessage> scheduleTarget;
	private ALRR_ClientWrapper[] clientsArray;
	private long experimentStart; // in nanosec
	private long sumOfPlanedDelays = 0; // in nanosec
	private AnonNode client;
	private ALRR_ReplyReceiver replyReceiver;
	//private int PERIOD_LENGTH; // in ms
	//private int MESSAGES_PER_PERIOD;
	private RandomVariable REQUEST_PAYLOAD_SIZE;
	private RandomVariable REPLY_PAYLOAD_SIZE;
	private long TIME_BETWEEN_SENDS; // in nanosec
	private RandomVariable REPLY_DELAY; // in sec
	
	
	public ALM_FS_ConstantRate(AL_FixedScheduleLoadGenerator owner) {
		this.settings = owner.getSettings();
		this.experimentStart = owner.getScheduler().now() + TimeUnit.SECONDS.toNanos(2);
		int numberOfClients = settings.getPropertyAsInt("AL-CONSTANT_RATE-NUMBER_OF_CLIENTS");
		float float_periodLength = settings.getPropertyAsFloat("AL-CONSTANT_RATE-PERIOD");
		float float_messagesPerPeriod = settings.getPropertyAsFloat("AL-CONSTANT_RATE-MESSAGES_PER_PERIOD");
		float float_timeBetweenSends = (float_periodLength*1000000000f) / (float_messagesPerPeriod * (float)numberOfClients);
		if (float_timeBetweenSends < 1f)
			this.TIME_BETWEEN_SENDS = 1;
		else
			this.TIME_BETWEEN_SENDS = Math.round(float_timeBetweenSends);
		String str_constantRateReplyDelay = settings.getProperty("AL-CONSTANT_RATE-REPLY_DELAY");
		if (RandomVariable.isRandomVariable(str_constantRateReplyDelay))
			this.REPLY_DELAY = RandomVariable.createRandomVariable(str_constantRateReplyDelay);
		else
			this.REPLY_DELAY = new FakeRandom(Double.parseDouble(str_constantRateReplyDelay));
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
		}
		String str_requestPayloadSize = settings.getProperty("AL-CONSTANT_RATE-REQUEST_PAYLOAD_SIZE");
		if (RandomVariable.isRandomVariable(str_requestPayloadSize)) {
			this.REQUEST_PAYLOAD_SIZE = RandomVariable.createRandomVariable(str_requestPayloadSize);
		} else {
			if (str_requestPayloadSize.equalsIgnoreCase("AUTO"))
				this.REQUEST_PAYLOAD_SIZE = new FakeRandom(clientsArray[0].socket.getMTU());
			else
				this.REQUEST_PAYLOAD_SIZE = new FakeRandom(Integer.parseInt(str_requestPayloadSize));
		}
		String str_replyPayloadSize = settings.getProperty("AL-CONSTANT_RATE-REPLY_PAYLOAD_SIZE");
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
		ApplicationLevelMessage currentEntry;
		int nextClientId = -1;
		for (int i=0; i<numberOfRecords; i++) {
			if (++nextClientId == clientsArray.length) // round robin choose clients
				nextClientId = 0;
			currentEntry = new ApplicationLevelMessage(
					TIME_BETWEEN_SENDS,
					nextClientId,
					nextClientId,
					REQUEST_PAYLOAD_SIZE.drawIntSample(),
					REPLY_PAYLOAD_SIZE.drawIntSample(),
					(float) REPLY_DELAY.drawDoubleSample()
					);
			sumOfPlanedDelays += TIME_BETWEEN_SENDS;
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
		return clientsArray[identifier];
	}
	
}
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
package evaluation.loadGenerator.mixPacketLevelTraffic;

import java.util.concurrent.TimeUnit;

import evaluation.loadGenerator.ClientTrafficScheduleWriter;
import evaluation.loadGenerator.fixedSchedule.MPL_FixedScheduleLoadGenerator;
import evaluation.loadGenerator.scheduler.ScheduleTarget;
import evaluation.loadGenerator.scheduler.Scheduler;
import framework.core.AnonNode;
import framework.core.config.Settings;
import framework.core.launcher.ToolName;
import framework.core.routing.RoutingMode;
import framework.core.socket.socketInterfaces.AnonSocketOptions.CommunicationMode;


public class MPL_FS_ConstantRate implements ClientTrafficScheduleWriter<MPL_ClientWrapper> {

	private Settings settings;
	private ScheduleTarget<MPL_ClientWrapper> scheduleTarget;
	private MPL_ClientWrapper[] clientsArray;
	private long experimentStart; // in nanosec
	private long sumOfPlanedDelays = 0; // in nanosec
	private AnonNode client;
	private MPL_ReplyReceiver replyReceiver;
	private long TIME_BETWEEN_SENDS; // in nanosec
	
	
	public MPL_FS_ConstantRate(MPL_FixedScheduleLoadGenerator owner) {
		this.settings = owner.getSettings();
		this.experimentStart = owner.getScheduler().now() + TimeUnit.SECONDS.toNanos(2);
		int numberOfClients = settings.getPropertyAsInt("MPL-CONSTANT_RATE-NUMBER_OF_CLIENTS");
		float float_periodLength = settings.getPropertyAsFloat("MPL-CONSTANT_RATE-PERIOD");
		float float_packetsPerPeriod = settings.getPropertyAsFloat("MPL-CONSTANT_RATE-PACKET_PER_PERIOD");
		float float_timeBetweenSends = (float_periodLength*1000000000f) / (float_packetsPerPeriod * (float)numberOfClients);
		if (float_timeBetweenSends < 1f)
			this.TIME_BETWEEN_SENDS = 1;
		else
			this.TIME_BETWEEN_SENDS = Math.round(float_timeBetweenSends);
		System.out.println("LOAD_GENERATOR: start at " +experimentStart);
		// create client
		owner.getLoadGenerator().commandLineParameters.gMixTool = ToolName.CLIENT;
		this.client = new AnonNode(owner.getLoadGenerator().commandLineParameters);
		int dstPort = settings.getPropertyAsInt("SERVICE_PORT1");
		this.scheduleTarget = new MPL_BasicWriter(this, client.IS_DUPLEX, dstPort);
		// determine number of clients and lines; create ClientWrapper objects etc
		this.clientsArray = new MPL_ClientWrapper[numberOfClients];
		CommunicationMode cm = client.IS_DUPLEX ? CommunicationMode.DUPLEX : CommunicationMode.SIMPLEX_SENDER;
		for (int i=0; i<numberOfClients; i++) {
			clientsArray[i] = new MPL_ClientWrapper(i);
			clientsArray[i].socket = client.createDatagramSocket(cm, true, true, client.ROUTING_MODE != RoutingMode.CASCADE);
		}
		if (client.IS_DUPLEX) {
			this.replyReceiver = new MPL_ReplyReceiver(clientsArray, settings);
			//this.replyReceiver.registerObserver(this);
			this.replyReceiver.start();
		}	
	}
	
	
	@Override
	public boolean scheduleRecords(int numberOfRecords, Scheduler<MPL_ClientWrapper> scheduler) {
		MPL_ClientWrapper currentClient;
		int nextClientId = -1;
		for (int i=0; i<numberOfRecords; i++) {
			if (++nextClientId == clientsArray.length) // round robin choose clients
				nextClientId = 0;
			currentClient = clientsArray[nextClientId];
			sumOfPlanedDelays += TIME_BETWEEN_SENDS;
			long timeToSend = experimentStart + sumOfPlanedDelays;
			scheduler.executeAt(
					timeToSend,
					scheduleTarget,
					currentClient
					);
		}
		return true;
	}
	
	
	@Override
	public MPL_ClientWrapper getClientWrapper(int identifier) {
		return clientsArray[identifier];
	}
	
}

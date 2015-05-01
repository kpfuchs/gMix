/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
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
 *******************************************************************************/
package staticContent.evaluation.loadGenerator.mixPacketLevelTraffic;

import staticContent.evaluation.loadGenerator.ClientTrafficScheduleWriter;
import staticContent.evaluation.loadGenerator.fixedSchedule.MPL_FixedScheduleLoadGenerator;
import staticContent.evaluation.loadGenerator.scheduler.ScheduleTarget;
import staticContent.evaluation.loadGenerator.scheduler.Scheduler;
import staticContent.framework.AnonNode;
import staticContent.framework.config.Settings;
import staticContent.framework.launcher.ToolName;
import staticContent.framework.routing.RoutingMode;
import staticContent.framework.socket.socketInterfaces.AnonSocketOptions.CommunicationDirection;


public class MPL_FS_ConstantRate implements ClientTrafficScheduleWriter<MPL_ClientWrapper> {

	private Settings settings;
	private ScheduleTarget<MPL_ClientWrapper> scheduleTarget;
	private MPL_ClientWrapper[] clientsArray;
	private long experimentStart; // in nanosec
	private long sumOfPlanedDelays = 0; // in nanosec
	private AnonNode client;
	private MPL_ReplyReceiver replyReceiver;
	private long TIME_BETWEEN_SENDS; // in nanosec
	private MPL_FixedScheduleLoadGenerator owner;
	
	
	public MPL_FS_ConstantRate(MPL_FixedScheduleLoadGenerator owner) {
		this.owner = owner;
		this.settings = owner.getSettings();
		this.experimentStart = owner.getScheduler().now();
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
		CommunicationDirection cm = client.IS_DUPLEX ? CommunicationDirection.DUPLEX : CommunicationDirection.SIMPLEX_SENDER;
		for (int i=0; i<numberOfClients; i++) {
			clientsArray[i] = new MPL_ClientWrapper(i);
			clientsArray[i].socket = client.createDatagramSocket(cm, true, true, client.ROUTING_MODE != RoutingMode.GLOBAL_ROUTING);
		}
		if (client.IS_DUPLEX) {
			this.replyReceiver = new MPL_ReplyReceiver(clientsArray, settings);
			//this.replyReceiver.registerObserver(this);
			this.replyReceiver.start();
		}	
	}
	
	private boolean isFirst = true;
	
	@Override
	public boolean scheduleRecords(int numberOfRecords, Scheduler<MPL_ClientWrapper> scheduler) {
		if (this.isFirst) {
			experimentStart = owner.getScheduler().now();
			this.isFirst = false;
		}
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

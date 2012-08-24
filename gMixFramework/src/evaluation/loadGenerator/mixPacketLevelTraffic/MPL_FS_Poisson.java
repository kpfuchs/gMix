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

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math.random.RandomDataImpl;

import evaluation.loadGenerator.ClientTrafficScheduleWriter;
import evaluation.loadGenerator.fixedSchedule.MPL_FixedScheduleLoadGenerator;
import evaluation.loadGenerator.randomVariable.FakeRandom;
import evaluation.loadGenerator.randomVariable.RandomVariable;
import evaluation.loadGenerator.scheduler.ScheduleTarget;
import evaluation.loadGenerator.scheduler.Scheduler;
import framework.core.AnonNode;
import framework.core.config.Settings;
import framework.core.launcher.ToolName;
import framework.core.routing.RoutingMode;
import framework.core.socket.socketInterfaces.AnonSocketOptions.CommunicationMode;


public class MPL_FS_Poisson implements ClientTrafficScheduleWriter<MPL_ClientWrapper> {

	private Settings settings;
	private ScheduleTarget<MPL_ClientWrapper> scheduleTarget;
	private MPL_ClientWrapper[] clientsArray;
	private long experimentStart; // in nanosec
	private long startOfPeriod; // in nanosec
	private AnonNode client;
	private MPL_ReplyReceiver replyReceiver;
	private RandomVariable AVG_SENDS_PER_PERIOD;
	private long PULSE_LENGTH; // in ns
	private RandomDataImpl randomDataImpl;
	private SecureRandom random;
	
	
	public MPL_FS_Poisson(MPL_FixedScheduleLoadGenerator owner) {
		this.settings = owner.getSettings();
		this.experimentStart = owner.getScheduler().now() + TimeUnit.SECONDS.toNanos(2);
		this.startOfPeriod = experimentStart;
		int numberOfClients = settings.getPropertyAsInt("MPL-POISSON-NUMBER_OF_CLIENTS");
		
		String str_avgSendsPerPulse = settings.getProperty("MPL-POISSON-AVERAGE_PACKETS_PER_PULSE");
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
		this.PULSE_LENGTH = (long) (settings.getPropertyAsFloat("MPL-POISSON-PULSE_LENGTH")*1000000000f);
		this.random = new SecureRandom();
		this.randomDataImpl = new RandomDataImpl();
		this.randomDataImpl.reSeed(this.random.nextLong());
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
		int periods = (int) (numberOfRecords / AVG_SENDS_PER_PERIOD.drawIntSample());
		if (periods == 0)
			periods = 1;
		int ctr = 0;
		
		for (int j=0; j<periods; j++) { // for each period
			long packetsInThisPeriod = randomDataImpl.nextPoisson(AVG_SENDS_PER_PERIOD.drawIntSample());
			System.out.println("packetsInThisPeriod: " +packetsInThisPeriod); // TODO: remove
			long[] delays = new long[(int) packetsInThisPeriod];
			for (int l=0; l<delays.length; l++) // distribute delays at (uniform) random (for this period)
				delays[l] = (long) (random.nextDouble() * PULSE_LENGTH);
			Arrays.sort(delays);
			for (int l=0; l<delays.length; l++) {
				ctr++;
				// choose client to send this request at (uniform) random:
				int clientId = (int) Math.round(random.nextDouble() * (double)(clientsArray.length-1)); 
				MPL_ClientWrapper client = clientsArray[clientId];
				long timeToSend = startOfPeriod + delays[l];
				scheduler.executeAt(
						timeToSend,
						scheduleTarget,
						client
						);
			}
			startOfPeriod += PULSE_LENGTH;
		} 
		System.out.println("MPL_Poisson: scheduled " +ctr +" requests"); 
		return true;
	}
	
	
	@Override
	public MPL_ClientWrapper getClientWrapper(int identifier) {
		return clientsArray[identifier];
	}
	
}

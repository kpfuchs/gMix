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
package userGeneratedContent.simulatorPlugIns.plugins.trafficSource;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.annotations.plugin.Plugin;
import staticContent.evaluation.simulator.annotations.property.BoolSimulationProperty;
import staticContent.evaluation.simulator.annotations.property.IntSimulationProperty;
import staticContent.evaluation.simulator.annotations.property.StringSimulationProperty;
import staticContent.evaluation.simulator.annotations.property.requirements.TrafficSourceTraceFileClientLimitRequirement;
import staticContent.evaluation.simulator.core.networkComponent.AbstractClient;
import staticContent.evaluation.traceParser.engine.dataStructure.Flow;
import staticContent.evaluation.traceParser.engine.fileReader.FlowReader;


@Plugin(pluginKey = "TRACE_FILE", pluginName = "Tracefile")
public class TraceFileModel extends TrafficSourceImplementation {

	private TraceReplayClient[] clients;
	
	@StringSimulationProperty(
			name = "Path to trace file",
			key = "PATH_TO_TRACE",
			info = "Please provide a relative path")
	private String pathToTrace;
	
	@BoolSimulationProperty(
			name = "Limit client number",
			key = "LIMIT_CLIENT_NUMBER")
	private boolean limitClients;
	
	@IntSimulationProperty(
			name = "Client limit",
			key = "CLIENT_LIMIT",
			min = 1,enable_requirements=TrafficSourceTraceFileClientLimitRequirement.class)
	private int limit;
	
	
	@BoolSimulationProperty(
			name = "Choose Clients at random",
			key = "CHOOSE_RANDOM_CLIENTS")
	private boolean chooseClientsAtRandom;
	
	
	@Override
	public AbstractClient[] createClientsArray() {
		this.pathToTrace = Simulator.settings.getProperty("PATH_TO_TRACE");
		this.limitClients = Simulator.settings.getPropertyAsBoolean("LIMIT_CLIENT_NUMBER");
		this.limit = limitClients ? Simulator.settings.getPropertyAsInt("CLIENT_LIMIT") : 0;
		this.chooseClientsAtRandom = Simulator.settings.getPropertyAsBoolean("CHOOSE_RANDOM_CLIENTS");
		Vector<TraceReplayClient> clients = new Vector<TraceReplayClient>(1000);
		HashSet<Integer> chosenClients = null;
		int cid = -1;
		try {
			if (chooseClientsAtRandom)
				chosenClients = getChosenClientIds(pathToTrace, limit);
			FlowReader flowReader = new FlowReader(pathToTrace, null, false);
			int currentClientId = -1;
			while (flowReader.hasNextFlow()) {
				Flow flow = flowReader.readFlow();
				if (currentClientId != flow.senderId) {
					currentClientId ++;
					if (limitClients && !chooseClientsAtRandom && currentClientId == limit)
						break;
					if (flow.senderId != currentClientId)
						throw new RuntimeException("the trace file " +pathToTrace +" seems to be not " +
								"ordered correctly.\nmake sure to use an Extractor (package evaluation." +
								"traceParser.scenarioExtractor) to generate the trace file and specify " +
								"the path of the created trace file in the experiment config file correctly.)" );
					long offset = flowReader.getOffsetOfLastFlow();
					FlowReader readerOfClient;
					try {
						RandomAccessFile raf = new RandomAccessFile(pathToTrace, "r");
						raf.seek(offset);
						// use BufferedReader to read line instead of raf.readLine() for performance reasons:
						BufferedReader reader = new BufferedReader(Channels.newReader(raf.getChannel(), "ISO-8859-1"));
						readerOfClient = new FlowReader(reader, null, true, raf);
					} catch (Exception e) {
						System.err.println("could not read " +pathToTrace); 
						e.printStackTrace();
						throw new RuntimeException();
					}
					if (chooseClientsAtRandom && !chosenClients.contains(currentClientId)) {
						readerOfClient.close();
						continue;
					} else {
						cid++;
						TraceReplayClient client = new TraceReplayClient("Client" +cid, Simulator.getSimulator(), readerOfClient, cid, currentClientId);
						clients.add(client);
					}
					
				}
			}
		} catch (Exception e) {
			System.err.println("could not read trace file " +pathToTrace); 
			e.printStackTrace();
			throw new RuntimeException(); 
		}
		this.clients = clients.toArray(new TraceReplayClient[0]); 
		return this.clients;
	}

	
	private HashSet<Integer> getChosenClientIds(String pathToTrace, int limit) throws FileNotFoundException, IOException {
		// determine number of clients:
		FlowReader flowReader = new FlowReader(pathToTrace, null, false);
		int currentClientId = -1;
		while (flowReader.hasNextFlow()) {
			Flow flow = flowReader.readFlow();
			if (currentClientId != flow.senderId)
				currentClientId ++;
		}
		flowReader.close();
		int numberofClients = currentClientId +1;
		//chose clients (no duplicates):
		ArrayList<Integer> all = new ArrayList<Integer>(numberofClients);
		for (int i=0; i<numberofClients; i++)
			all.add(i);
		Collections.shuffle(all);
		List<Integer> chosenOnes = all.subList(0, limit);
		HashSet<Integer> result = new HashSet<Integer>(numberofClients);
		result.addAll(chosenOnes); 
		return result;
	}
	
	
	@Override
	public void startSending() {
		for (TraceReplayClient client: clients)
			client.startSending();
	}

}

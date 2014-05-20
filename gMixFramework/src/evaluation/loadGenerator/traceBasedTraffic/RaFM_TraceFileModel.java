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
package evaluation.loadGenerator.traceBasedTraffic;

import java.io.BufferedReader;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.util.Vector;

import evaluation.traceParser.engine.dataStructure.Flow;
import evaluation.traceParser.engine.fileReader.FlowReader;
import framework.core.config.Settings;


/**
 * This class is responsible for creating the clients with the ability to read
 * the trace file via flowReader (which is passed in constructor to every
 * client).
 * 
 * @author Johannes Wendel, Simon Lecheler, kpf
 * 
 */
public class RaFM_TraceFileModel {

	private Settings settings;


	public RaFM_TraceFileModel(Settings settings){
		this.settings = settings; 
	}
	

	/**
	 * This method creates the number of clients defined in the config-file.
	 * Each client gets an FlowReader-Object which is able to read the trace
	 * file.
	 * 
	 * @return returns an array containing the clients
	 *         (TBT_TraceReplayClient-Objects).
	 */
	public RaFM_TraceReplayClient[] createClientsArray() {
		String pathToTrace = settings.getProperty("AL-RaFM_TraceFile-NAME");
		boolean limitClients = settings.getPropertyAsBoolean("AL-RaFM-LIMIT_NUMBER_OF_CLIENTS");
		int limit = limitClients ? settings.getPropertyAsInt("AL-RaFM-CLIENT_LIMIT") : 0;
		boolean debugOn = settings.getPropertyAsBoolean("AL-RaFM_DIPLAY_DEBUG_INFO");
		boolean isDuplex = settings.getPropertyAsBoolean("GLOBAL_IS_DUPLEX");
		if (limitClients)
			System.err.println("warning: AL-RaFM_TraceFile-NAME client limit active; see AL-RaFM-LIMIT_NUMBER_OF_CLIENTS in loadGenerator config file");
		Vector<RaFM_TraceReplayClient> clients = new Vector<RaFM_TraceReplayClient>(1000);
		try {
			FlowReader flowReader = new FlowReader(pathToTrace, null, false);
			int currentClientId = -1;
			while (flowReader.hasNextFlow()) {
				Flow flow = flowReader.readFlow();
				if (currentClientId != flow.senderId) {
					currentClientId ++;
					if (limitClients && currentClientId == limit)
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
						readerOfClient = new FlowReader(reader, null, true);
					} catch (Exception e) {
						System.err.println("could not read " +pathToTrace); 
						e.printStackTrace();
						throw new RuntimeException();
					}
					RaFM_TraceReplayClient client = new RaFM_TraceReplayClient(readerOfClient, currentClientId, isDuplex, debugOn);
					clients.add(client);
				}
			}
		} catch (Exception e) {
			System.err.println("could not read trace file " +pathToTrace); 
			e.printStackTrace();
			throw new RuntimeException(); 
		}
		return clients.toArray(new RaFM_TraceReplayClient[0]);
	}

}

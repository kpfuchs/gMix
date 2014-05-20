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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import evaluation.loadGenerator.LoadGenerator;
import evaluation.loadGenerator.scheduler.ThreadPoolScheduler;
import evaluation.loadGenerator.traceBasedTraffic.event.Event;
import framework.core.AnonNode;
import framework.core.launcher.ToolName;
import framework.core.routing.RoutingMode;
import framework.core.socket.socketInterfaces.StreamAnonSocket;
import framework.core.socket.socketInterfaces.AnonSocketOptions.CommunicationDirection;
import framework.core.util.Util;


/**
 * This class establishes the connections of the clients to the first mix and 
 * instruments the clients. The inner-class ReplyReceiverThread is responsible 
 * for receiving the incoming replies from the server in a dedicated thread.
 * 
 * @author Johannes Wendel, Simon Lecheler, kpf
 * 
 */
public class RaFM_LoadGenerator {

	private AnonNode client; // we use only one "real" client to retrieve public keys of mixes etc and then create several anonymous connections (sockets) with this client (note: those connections are unlinkable for mixes, so this simplification should not affect simulation accuracy but improve the performance of the load generator)   
	private ThreadPoolScheduler<Event> scheduler; // scheduler used by clients to schedule send events
	
	private RaFM_TraceReplayClient[] clients; // the virtual clients as present in the trace (note: clients are event-based; all events are called by the threads of this class (and its scheduler))
	private StreamAnonSocket[] clientSockets;
	private ReplyReceiverThread[] replyThreads; // for duplex simulations (notifies virtual clients about received replies)
	
	private int voteStopCounter = 0;
	

	protected RaFM_LoadGenerator(LoadGenerator owner) {
		int numberOfThreads = owner.settings.getPropertyAsInt("AL-RaFM-NUMBER_OF_SCHEDULER_THREADS");
		this.scheduler = new ThreadPoolScheduler<Event>(owner.settings, numberOfThreads);
		RaFM_TraceFileModel traceFileModel = new RaFM_TraceFileModel(owner.settings);
		this.clients = traceFileModel.createClientsArray();
		this.clientSockets = new StreamAnonSocket[this.clients.length];
		this.replyThreads = new ReplyReceiverThread[this.clients.length];
		
		//create anon client we will use to create sockets:
		owner.commandLineParameters.gMixTool = ToolName.CLIENT;
		this.client = new AnonNode(owner.commandLineParameters);
		CommunicationDirection cd = client.IS_DUPLEX ? CommunicationDirection.DUPLEX : CommunicationDirection.SIMPLEX_SENDER;
		int port = owner.settings.getPropertyAsInt("SERVICE_PORT1");
		
		System.out.println("RaFM_LOAD_GENERATOR: connecting clients..."); 
		for (int i=0; i<this.clients.length; i++) { // connect all virtual clients
			clientSockets[i] = client.createStreamSocket(cd, client.ROUTING_MODE != RoutingMode.CASCADE);
			try {
				clientSockets[i].connect(port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (client.IS_DUPLEX) { // start reply threads
			for (int i=0; i<this.clients.length; i++) {
				try {
					replyThreads[i] = new ReplyReceiverThread(clients[i], clientSockets[i]);
				} catch (IOException e) {
					e.printStackTrace();
				}
				replyThreads[i].start();
			}
		}
		for (int i=0; i<this.clients.length; i++) { // tell virtual clients to start scheduling/sending
			clients[i].startSending(this, scheduler, clientSockets[i]);
		}
	
	}

	
	public void voteForStop(StreamAnonSocket socket) {
		voteStopCounter++;
		System.out.println("Stopped Clients: "+voteStopCounter);
		if (voteStopCounter == clients.length)
			stopSimulation("end of trace reached (variable SIMULATION_END in experiment config)");
	}

	
	public void stopSimulation(String reason) {
		System.out.println("### stopping simulation. reason: " + reason);
		System.exit(0);
	}

	
	private class ReplyReceiverThread extends Thread {
		
		private InputStream inputStream;
		private RaFM_TraceReplayClient client;
		
		
		public ReplyReceiverThread(RaFM_TraceReplayClient client, StreamAnonSocket socket) throws IOException {
			this.client = client;
			this.inputStream = socket.getInputStream();
		}
		

		@Override
		public void run() {
			while (true) {
				try {
					byte[] reply = Util.forceRead(inputStream, Util.forceReadInt(inputStream));
					int transactionId = Util.byteArrayToInt(Arrays.copyOf(reply, 4));
					client.incomingReply(transactionId);
				} catch (IOException e) {
					stopSimulation(e.getLocalizedMessage());
				}
			} 
		}
		
	}


	public static RaFM_LoadGenerator createInstance(LoadGenerator loadGenerator) {
		return new RaFM_LoadGenerator(loadGenerator);
	}
	
}

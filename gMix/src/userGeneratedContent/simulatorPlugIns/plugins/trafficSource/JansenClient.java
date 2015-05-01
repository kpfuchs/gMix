/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  Karl-Peter Fuchs
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
package userGeneratedContent.simulatorPlugIns.plugins.trafficSource;

import org.apache.commons.math.random.RandomDataImpl;

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.annotations.plugin.Plugin;
import staticContent.evaluation.simulator.annotations.property.IntSimulationProperty;
import staticContent.evaluation.simulator.core.event.Event;
import staticContent.evaluation.simulator.core.event.SimulationEvent;
import staticContent.evaluation.simulator.core.message.EndToEndMessage;
import staticContent.evaluation.simulator.core.networkComponent.AbstractClient;
import staticContent.evaluation.traceParser.engine.dataStructure.ExtendedTransaction;


/**
 * 
 * logic: paper "Never Been KIST: Tor’s Congestion Management Blossoms with 
 * Kernel-Informed Socket Transport", 2014, Rob Jansen, John Geddes, Chris 
 * Wacek, Micah Sherr, Paul Syverson
 * 
 * web-clients: 78.26%:
 * 		1: wait [1,60000] ms (uniform)
 *		2: download 320 KiB
 *		3: goto 1
 *	dl-clients: 8.69%
 *		1: download 5 MiB
 *		2: goto
 *	tor-perf1: 4.35%
 *		1: wait 60000 ms
 *		2. download 50 KiB
 *		3. goto 1
 *	tor-perf2: 4.35%
 *		1: wait 60000 ms
 *		2. download 1 MiB
 *		3. goto 1
 *	tor-perf3: 4.35%
 *		1: wait 60000 ms
 *		2. download 5 MiB
 *		3. goto 1
 * Total: one hour simulation time
 *
 */
@Plugin(pluginKey = "JANSEN", pluginName="Jansen Model")
public class JansenClient extends AbstractClient {

	public enum ClientType {WEB, DOWNLOAD, PERF1, PERF2, PERF3};
	private ClientType clientType;
	private RandomDataImpl rand = new RandomDataImpl();
	private int requestSize;
	@IntSimulationProperty( name = "Maximum bytes/sec for server to send replies (bw-limit)", 
			key = "JANSEN_SERVER_BW_LIMIT_BYTES_PER_SEC",
			min = 1)
	private int maxBytesPerSec;
	private int sendInterval;
	private int maxReplyPayloadSize;
	private int expectedReplies = 0;
	private int receivedReplies = 0;
	
	
	public JansenClient(ClientType clientType, String identifier, Simulator simulator, int clientId) {
		super(identifier, simulator);
		this.clientId = clientId;
		this.clientType = clientType;
		this.requestSize = Simulator.settings.getPropertyAsInt("MIX_REQUEST_PAYLOAD_SIZE");
		this.maxReplyPayloadSize = Simulator.settings.getPropertyAsInt("MIX_REPLY_PAYLOAD_SIZE");
		this.maxBytesPerSec = Simulator.settings.getPropertyAsInt("JANSEN_SERVER_BW_LIMIT_BYTES_PER_SEC");
		this.sendInterval = (int) Math.round(1000d/((double)maxBytesPerSec/(double)maxReplyPayloadSize));
	}

	
	public void startSending() {
		int delay = rand.nextSecureInt(1, 1000);
		Event startSendingEvent = new Event(this, Simulator.getNow() + delay, JansenClientEvent.START_SENDING);
		simulator.scheduleEvent(startSendingEvent, this);
	}

	
	@Override
	public void incomingMessage(EndToEndMessage message) {
		receivedReplies ++;
		if (receivedReplies == expectedReplies)
			scheduleNextMessage();
	}

	
	@Override
	public void messageReachedServer(EndToEndMessage message) {
		// JansenClient uses arrival of reply as feedback, so we do 
		// nothing here
	}
	
	
	/**
	 * 
	 * logic: paper "Never Been KIST: Tor’s Congestion Management Blossoms with 
	 * Kernel-Informed Socket Transport", 2014, Rob Jansen, John Geddes, Chris 
	 * Wacek, Micah Sherr, Paul Syverson
	 * 
	 * web-clients: 78.26%:
	 * 		1: wait [1,60000] ms (uniform)
	 *		2: download 320 KiB
	 *		3: goto 1
	 *	dl-clients: 8.69%
	 *		1: download 5 MiB
	 *		2: goto
	 *	tor-perf1: 4.35%
	 *		1: wait 60000 ms
	 *		2. download 50 KiB
	 *		3. goto 1
	 *	tor-perf2: 4.35%
	 *		1: wait 60000 ms
	 *		2. download 1 MiB
	 *		3. goto 1
	 *	tor-perf3: 4.35%
	 *		1: wait 60000 ms
	 *		2. download 5 MiB
	 *		3. goto 1
	 * Total: one hour simulation time
	 *
	 */
	private void scheduleNextMessage() {
		if (clientType == ClientType.WEB) {
			int delay = rand.nextSecureInt(1, 60000);
			int replySize = 320*1024;
			ExtendedTransaction et = createTransaction(replySize);
			EndToEndMessage eteMessage = new EndToEndMessage(0, et, true);
			Event sendNextMessageEvent = new Event(this, Simulator.getNow() + delay, JansenClientEvent.SEND_NEXT_MESSAGE, eteMessage);
			simulator.scheduleEvent(sendNextMessageEvent, this);
		} else if (clientType == ClientType.DOWNLOAD) {
			int replySize = 5*1024*1024; 
			ExtendedTransaction et = createTransaction(replySize);
			EndToEndMessage eteMessage = new EndToEndMessage(0, et, true);
			sendMessage(eteMessage);
		} else if (clientType == ClientType.PERF1) {
			int delay = 60000;
			int replySize = 50*1024;
			ExtendedTransaction et = createTransaction(replySize);
			EndToEndMessage eteMessage = new EndToEndMessage(0, et, true);
			Event sendNextMessageEvent = new Event(this, Simulator.getNow() + delay, JansenClientEvent.SEND_NEXT_MESSAGE, eteMessage);
			simulator.scheduleEvent(sendNextMessageEvent, this);
		} else if (clientType == ClientType.PERF2) {
			int delay = 60000;
			int replySize = 1024*1024;
			ExtendedTransaction et = createTransaction(replySize);
			EndToEndMessage eteMessage = new EndToEndMessage(0, et, true);
			Event sendNextMessageEvent = new Event(this, Simulator.getNow() + delay, JansenClientEvent.SEND_NEXT_MESSAGE, eteMessage);
			simulator.scheduleEvent(sendNextMessageEvent, this);
		} else if (clientType == ClientType.PERF3) {
			int delay = 60000;
			int replySize = 5*1024*1024;
			ExtendedTransaction et = createTransaction(replySize);
			EndToEndMessage eteMessage = new EndToEndMessage(0, et, true);
			Event sendNextMessageEvent = new Event(this, Simulator.getNow() + delay, JansenClientEvent.SEND_NEXT_MESSAGE, eteMessage);
			simulator.scheduleEvent(sendNextMessageEvent, this);
		} else {
			throw new RuntimeException("Unknown ClientType -> check source code (impementation fault)"); 
		}
	}
	
	
	private ExtendedTransaction createTransaction(int toSend) {
		int numberOfReplies = (int)Math.ceil((double)toSend/maxReplyPayloadSize);
		long[] replyOffsets = new long[numberOfReplies];
		int[] replySizes = new int[numberOfReplies];
		int remaining = toSend;
		int replyOffset = 0;
		for (int i=0; i<numberOfReplies; i++) {
			replyOffsets[i] = replyOffset;
			replyOffset += sendInterval;
			if (remaining >= maxReplyPayloadSize) {
				replySizes[i] = maxReplyPayloadSize;
				remaining -= maxReplyPayloadSize;
			} else {
				replySizes[i] = remaining;
			}
		} 
		this.expectedReplies = numberOfReplies;
		this.receivedReplies = 0;
		return new ExtendedTransaction(0, 0l, 0l, requestSize, 0, replyOffsets, replyOffsets, replySizes);
	}
	
	// TODO:remove:
	public static ExtendedTransaction createT(int toSend, int maxReplyPayloadSize, int sendInterval, int requestSize) {
		int numberOfReplies = (int)Math.ceil((double)toSend/maxReplyPayloadSize);
		long[] replyOffsets = new long[numberOfReplies];
		int[] replySizes = new int[numberOfReplies];
		int remaining = toSend;
		int replyOffset = 0;
		for (int i=0; i<numberOfReplies; i++) {
			replyOffsets[i] = replyOffset;
			replyOffset += sendInterval;
			if (remaining >= maxReplyPayloadSize) {
				replySizes[i] = maxReplyPayloadSize;
				remaining -= maxReplyPayloadSize;
			} else {
				replySizes[i] = remaining;
			}
		} 
		expectedR = numberOfReplies;
		return new ExtendedTransaction(0, 0l, 0l, requestSize, 0, replyOffsets, replyOffsets, replySizes);
	}
	static int expectedR = 0;
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		Integer[] test = new Integer[] {(50*1024), (320*1024), (1024*1024), (5*1024*1024)};
		int sendInterval = (int) Math.round(1000d/((double)15625/(double)512));
		for (int i=0; i<test.length; i++) {
			System.out.println();
			System.out.println("size: " +test[i]);
			createT(test[i], 512, sendInterval, 512);
			System.out.println("expected replies: " +expectedR);
			System.out.println("transfer duration: " +(expectedR*sendInterval) +"ms"); 
			
			
			/*System.out.println(); 	
					int numberOfClients = test[i];
			int webClients = (int) Math.round(0.7826d*(double)numberOfClients);
			int dlClients = (int) Math.round(0.0869d*(double)numberOfClients);
			int perf1Clients = (int) Math.round(0.0435d*(double)numberOfClients);
			int perf2Clients = (int) Math.round(0.0435d*(double)numberOfClients);
			int perf3Clients = (int) Math.round(0.0435d*(double)numberOfClients);
			int sum = webClients + dlClients + perf1Clients + perf2Clients + perf3Clients; 
			if (sum < numberOfClients)
				webClients += numberOfClients - sum;
			else if (sum > numberOfClients)
				webClients--;
			System.out.println();
			System.out.println(numberOfClients + " clients:");
			System.out.println("web: " +webClients); 
			System.out.println("dlClients: " +dlClients); 
			System.out.println("perf1Clients: " +perf1Clients); 
			System.out.println("perf2Clients: " +perf2Clients); 
			System.out.println("perf3Clients: " +perf3Clients); */
			System.out.println(); 
		} 
		
	} 
	
	@Override
	public void close() {
		// nothing to do here
	}
	
	
	@Override
	public void executeEvent(Event event) {
		if (event.getEventType() instanceof JansenClientEvent) {
			if (event.getEventType() == JansenClientEvent.SEND_NEXT_MESSAGE) {
				sendMessage((EndToEndMessage)event.getAttachment());
			} else if (event.getEventType() == JansenClientEvent.START_SENDING) {
				scheduleNextMessage();
			} else {
				throw new RuntimeException("ERROR: received unknown Event: " +event.toString()); 
			}
		} else {
			super.executeEvent(event);
		}
	}
	
	
	private enum JansenClientEvent implements SimulationEvent {
		SEND_NEXT_MESSAGE,
		START_SENDING;	
	}
	
}

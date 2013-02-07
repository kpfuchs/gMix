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
package evaluation.traceParser.engine.converter;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import evaluation.traceParser.engine.Protocol;
import evaluation.traceParser.engine.dataStructure.Flow;
import evaluation.traceParser.engine.dataStructure.Packet;
import evaluation.traceParser.engine.dataStructure.ExtendedTransaction;
import evaluation.traceParser.engine.dataStructure.Flow.FlowDirection;
import evaluation.traceParser.engine.dataStructure.Packet.TCPflags;
import evaluation.traceParser.engine.fileReader.PacketIterator;
import evaluation.traceParser.engine.fileReader.PacketSource;
import evaluation.traceParser.engine.protocolHandler.TCPhandler.TCPhandshakeStatus;
import evaluation.traceParser.engine.protocolHandler.TCPhandler.TCPteardownStatus;
import framework.core.util.Util;


public class TCPFlowExtractor {

	private enum TransactionStatus {NONE, REQUEST_PHASE, REPLY_PHASE};
	private final static long UNKNOWN = -1l;
	private final static int MULTIPART_REPLY_THRESHOLD = 5; // consecutive replies with a delay of less than MULTIPART_REPLY_THRESHOLD ms will be treated as a single reply
	private Writer resultTrace;
	private HashSet<String> flowIds;
	private HashMap<String, TempFlow> tempFlows;
	private long startOfTrace = Util.NOT_SET;
	//private Vector<TempFlow> removals;
	
	
	public void extractFlows(HashSet<String> flowIds, Writer resultTrace, PacketSource packetSource) throws IOException {
		this.flowIds = flowIds;
		this.resultTrace = resultTrace;
		//this.packetSource = packetSource;
		this.tempFlows = new HashMap<String, TempFlow>(10000); // TODO: change flowIds type to <FlowId.java> -> data structure with two fields: string id + TempFlow tempFlowReference -> avoid two hashtables
		//this.removals = new Vector<TempFlow>(1000);
		PacketIterator iterator = new PacketIterator(packetSource);
		while (iterator.hasNext()) {
			handlePacket(iterator.next());
		}
	}

	
	/**
	 * data structure that stores all packets of a distinct flow (one TempFlow 
	 * per "real" (tcp) flow). will be used to create an "Flow.java" 
	 * object after the last packet of this flow was added (tcp teardown ack).
	 */
	public class TempFlow {
		public String flowIdentifier;
		//long flowIdNumeric;
		public FlowDirection flowDirection; // who inited this flow?
		public Protocol layer4protocol;
		public String layer4protocolAsString;
		public String clientAddress;
		public String clientPort;
		public String serverAddress;
		public String serverPort;
		public TCPhandshakeStatus handshakeStatus;
		public TCPteardownStatus teardownStatus;
		//long startOfFlow;
		//Calendar startTime;
		//Calendar endTime;
		public Vector<Packet> packets; // stores all packets of this flow. will be used to create an "Flow.java" after the last packet of this flow was added (e.g. tcp teardown ack)
		
		
		public TempFlow(String flowIdentifier) {
			this.flowIdentifier = flowIdentifier;
			//this.flowIdNumeric = flowIdCounter++;
			packets = new Vector<Packet>(10);
		}
		
	}
	
	
	public Packet handlePacket(Packet packet) {
		/*if (packet.getSequenceNumber() % 50000 == 0) { // remove dead entries
			for (TempFlow flow:tempFlows.values())
				if ( (packet.getTimestamp().getTimeInMillis() - flow.startOfFlow) > 5000
					&& flow.handshakeStatus != TCPhandshakeStatus.COMPLETE
						)  // no complete handshake after 5 seconds
					removals.add(flow);
			System.out.println("read " +packet.getSequenceNumber() +" packets so far. hm-size: " +tempFlows.size() +" - " +removals.size());
			for (TempFlow flowToRemove:removals)
				tempFlows.remove(flowToRemove);
			removals.clear();
		}*/
		if (startOfTrace == Util.NOT_SET)
			startOfTrace = packet.getTimestamp().getTimeInMillis();
		// filter:
		
		if (packet.getLayer3protocol() != Protocol.TCP) // this is a tcp flow extractor -> ignore other packets
			return null;
		if (packet.getLayer4length() == UNKNOWN) // we cannot replay packets with unknown size
			return null;
		//if (packet.getLayer4protocol() != Protocol.HTTP && packet.getLayer4protocol() != Protocol.HTTPS)
		//	return null;
		String flowIdentifier = TCPflowFinder.getFlowIdentifier(packet);
		if (!flowIds.contains(flowIdentifier)) // will be handled in another run
			return null;
		
		TempFlow flow = tempFlows.get(flowIdentifier);
		if (flow == null) { // we dont know this flow yet
			if (packet.getTCPflags() != TCPflags.SYN) { // we haven't seen the start of this flow, so we ignore it (malformed packet, or start of flow not captured/not present in trace file)
				return null;
			} else { // add new flow
				flow = new TempFlow(flowIdentifier);
				tempFlows.put(flowIdentifier, flow);
				//flow.startOfFlow = packet.getTimestamp().getTimeInMillis();
			}
		} 
		if (flow.packets.size() > 1) {
			assert flow.packets.get(flow.packets.size()-1).getTimestamp().compareTo(packet.getTimestamp()) < 1;
		}
		flow.packets.add(packet);
		if (flow.handshakeStatus == null && packet.getTCPflags() == TCPflags.SYN) { // SYN flag set; first packet of the flow
			//System.out.println("found SYN");
			flow.handshakeStatus = TCPhandshakeStatus.SYN_TRANSMITTED;
			//flow.startTime = packet.getTimestamp();
			flow.flowDirection = packet.getFlowDirection();
			flow.layer4protocol = packet.getLayer4protocol();
			flow.layer4protocolAsString = packet.getLayer4protocol().toString();
			flow.clientAddress = packet.getLayer2srcAddress();
			flow.clientPort = packet.getLayer3srcAddress();
			flow.serverAddress = packet.getLayer2dstAddress();
			flow.serverPort = packet.getLayer3dstAddress();
		} else if (flow.handshakeStatus == null) { // no handshakeStatus and no syn flag -> we haven't seen the start of this flow, so we ignore it
			//System.out.println("found packet not belonging to a connection");
			tempFlows.remove(flow.flowIdentifier);
			return null;
		} else if (flow.handshakeStatus == TCPhandshakeStatus.SYN_TRANSMITTED) {
			if (packet.getTCPflags() != TCPflags.SYN_ACK) { // corrupt flow -> discart it
				//System.out.println("found corrupt flow: no SYN_ACK: " +flow.flowIdentifier +", " +packet.getTimestamp().getTime()+ " and " +packet.getTimestamp().get(Calendar.MILLISECOND) +"ms, size: " +packet.getLayer4length()); 
				tempFlows.remove(flow.flowIdentifier);
				return null;
			} else { // none-corrupt flow
				// System.out.println("found SYN_ACK"); 
				flow.handshakeStatus = TCPhandshakeStatus.SYN_ACK_TRANSMITTED;
			}
		} else if (flow.handshakeStatus == TCPhandshakeStatus.SYN_ACK_TRANSMITTED) {
			if (packet.getTCPflags() != TCPflags.ACK) { // corrupt flow -> discart it
				//System.out.println("found corrupt flow: no ACK after SYN_ACK");
				tempFlows.remove(flow.flowIdentifier);
				return null;
			} else { // none-corrupt flow
				//System.out.println("found ACK -> handshake complete"); 
				flow.handshakeStatus = TCPhandshakeStatus.COMPLETE;
			}
		}
		
		if (flow.teardownStatus == null && (packet.getTCPflags() == TCPflags.FIN || packet.getTCPflags() == TCPflags.FIN_ACK)) {
			// System.out.println("found FIN"); 
			flow.teardownStatus = TCPteardownStatus.FIN1_TRANSMITTED;
		} else if (flow.teardownStatus == TCPteardownStatus.FIN1_TRANSMITTED && (packet.getTCPflags() == TCPflags.FIN || packet.getTCPflags() == TCPflags.FIN_ACK)) {
			// TODO: should we deal with half-open tcp connections?
			//System.out.println("tcp teardown complete");
			flow.teardownStatus = TCPteardownStatus.COMPLETE;
			//flow.endTime = packet.getTimestamp();
			// serialize and remove flow:
			serializeFlow(tempFlows.remove(flowIdentifier));
		}
		//lastPacket = packet;
		return packet;
	}
	
	
	private void serializeFlow(TempFlow flow) {
		// TODO: extract and store latency of flow
		//System.out.println("serialize flow: start");
		assert flow.handshakeStatus == TCPhandshakeStatus.COMPLETE;
		assert flow.teardownStatus == TCPteardownStatus.COMPLETE;
		
		// create Flow object
		Flow result = new Flow();
		result.startOfFlow = flow.packets.get(2).getTimestamp().getTimeInMillis() - startOfTrace;// ignore handshake packets -> start with 3rd packet
		assert flow.packets.get(1).getLayer4length() == UNKNOWN || flow.packets.get(1).getLayer4length() == 0 : flow.packets.get(1);
		result.requestSize = 0;
		result.replySize = 0;
		result.senderAddress = flow.clientAddress;
		result.receiverAddress = flow.serverAddress;
		result.senderId = AddressMapper.getClientId(result.senderAddress);
		result.receiverID = AddressMapper.getServerId(result.receiverAddress);
		result.senderPort = Integer.parseInt(flow.clientPort);
		result.receiverPort = Integer.parseInt(flow.serverPort);
		result.layer4protocol = flow.layer4protocol;
		result.protocolAsString = flow.layer4protocolAsString;
		result.flowDirection = flow.flowDirection;
		
		// extract transactions:
		TransactionStatus transactionStatus = TransactionStatus.NONE;
		long startOfRequest = UNKNOWN;
		long endOfRequest = UNKNOWN;
		//long endOfRequestPhase = UNKNOWN;
		Vector<Long> startReplyOffsets = new Vector<Long>();
		Vector<Long> endReplyOffsets = new Vector<Long>();
		Vector<Integer> replySizes = new Vector<Integer>();
		long startOfReply = UNKNOWN;
		long endOfReply = UNKNOWN;
		//long startOfReplyPhase = UNKNOWN;
		//long endOfTransaction = UNKNOWN;
		long lastActivity = UNKNOWN;
		long endOfLastTransaction = result.startOfFlow;
		//long startOfLastTransaction = UNKNOWN;
		
		int transactionRequestSize = 0;
		int transactionReplySize = 0;

		//int packetCtr = 1;
		for (int i=0; i<flow.packets.size(); i++) {
			Packet packet = flow.packets.get(i);
			long now = packet.getTimestamp().getTimeInMillis() - startOfTrace; // offset from start of trace
			assert now >= lastActivity;
			if (packet.getLayer4length() == 0 || packet.getLayer4length() == UNKNOWN) { // ignore ack packages; we are interested in payload only
				//System.out.println("serialize flow: NEW PACKET("+packetCtr++ +"): no payload packed -> ignore");
				continue;
			}
			if (packet.getLayer2dstAddress().equals(result.receiverAddress)) { // REQUEST (from client to server)
				//System.out.println("serialize flow: NEW PACKET("+packetCtr++ +"): direction: from client to server");
				switch (transactionStatus) {
					case NONE: // new transaction
						//System.out.println("serialize flow: status: NONE");  
						//System.out.println("serialize flow: it's the start of a new transaction (request)");  
						transactionStatus = TransactionStatus.REQUEST_PHASE;
						startOfRequest = now;
						endOfRequest = now; // may be changed later
						lastActivity = now;
						transactionRequestSize += packet.getLayer4length();
						result.requestSize += packet.getLayer4length();
						break;
					case REQUEST_PHASE: // REQUEST+REQUEST: packet might belong to the last request or a new one (decide by delay)
						//System.out.println("serialize flow: status: REQUEST_PHASE"); 
						assert lastActivity != UNKNOWN;
						long timeSinceLastRequest = now - lastActivity;
						if (timeSinceLastRequest <= 1) { // packet belongs to last request: assume that a delay of more than one ms indicates a new transaction (no layer 4 application on the client should interrupt a send operation that long...)
							//System.out.println("serialize flow: packet belongs to last request"); 
							lastActivity = now;
							endOfRequest = now; // may be changed later
							transactionRequestSize += packet.getLayer4length();
							result.requestSize += packet.getLayer4length();
						} else { // packet belongs to a new transaction
							//System.out.println("serialize flow: packet belongs to a new transaction"); 
							// finish current transaction (as a transaction without reply):
							result.transactions.add(new ExtendedTransaction(
									(int) (startOfRequest - endOfLastTransaction),
									startOfRequest,
									endOfRequest,
									transactionRequestSize,
									result.receiverID,
									null, 
									null,
									null
								));
							endOfLastTransaction = endOfRequest;
							//endOfRequestPhase = UNKNOWN; // reset
							startOfReply = UNKNOWN; // reset
							endOfReply = UNKNOWN; // reset
							//startOfReplyPhase = UNKNOWN; // reset
							//endOfTransaction = UNKNOWN; // reset
							transactionRequestSize = 0; // reset
							transactionReplySize = 0; // reset
							// start new transaction:
							//startOfLastTransaction = startOfTransaction;
							transactionStatus = TransactionStatus.REQUEST_PHASE;
							startOfRequest = now;
							endOfRequest = now; // may be changed later
							lastActivity = now;
							transactionRequestSize += packet.getLayer4length();
							result.requestSize += packet.getLayer4length();
						}
						break;
					case REPLY_PHASE: // REPLY+REQUEST: packet ends the current transaction and starts a new one
						//System.out.println("serialize flow: status: REPLY_PHASE");
						//System.out.println("serialize flow: it's the end of the current transaction (request)");
						// store data about current reply:
						assert startOfReply != UNKNOWN;
						replySizes.add(transactionReplySize);
						startReplyOffsets.add(startOfReply);
						endReplyOffsets.add(endOfReply);
						result.transactions.add(new ExtendedTransaction(
								(int) (startOfRequest - endOfLastTransaction),
								startOfRequest,
								endOfRequest,
								transactionRequestSize,
								result.receiverID, 
								Util.toLongArray(startReplyOffsets),
								Util.toLongArray(endReplyOffsets),
								Util.toIntArray(replySizes)
							));
						endOfLastTransaction = endOfReply;
						//endOfRequestPhase = UNKNOWN; // reset
						startOfReply = UNKNOWN; // reset
						endOfReply = UNKNOWN; // reset
						//startOfReplyPhase = UNKNOWN; // reset
						//endOfTransaction = UNKNOWN; // reset
						transactionRequestSize = 0; // reset
						transactionReplySize = 0; // reset
						startReplyOffsets.clear(); // reset
						endReplyOffsets.clear(); // reset
						replySizes.clear(); // reset
						// start new transaction:
						//System.out.println("serialize flow: it's the start of a new transaction (request)"); 
						//startOfLastTransaction = startOfTransaction;
						transactionStatus = TransactionStatus.REQUEST_PHASE;
						startOfRequest = now;
						endOfRequest = now; // may be changed later
						lastActivity = now;
						transactionRequestSize += packet.getLayer4length();
						result.requestSize += packet.getLayer4length();
						break;
				}
			} else { // REPLY (from server to client)
				//System.out.println("serialize flow: NEW PACKET("+packetCtr++ +"): direction: from server to client");
				switch (transactionStatus) {
					case NONE: // server sends the first message
						transactionStatus = TransactionStatus.REPLY_PHASE;
						//endOfRequestPhase = now;
						startOfRequest = now;
						endOfRequest = now;
						lastActivity = now;
						transactionRequestSize = 0;
						result.requestSize = 0;
						transactionReplySize = packet.getLayer4length();
						result.replySize += packet.getLayer4length();
						startOfReply = now;
						endOfReply = now; // may be changed later
						//startOfReplyPhase = now;
						break;
					case REQUEST_PHASE: // REQUEST+REPLY: switch from REQUEST_PHASE to REPLY_PHASE
						//System.out.println("serialize flow: status: REQUEST_PHASE");  
						//System.out.println("serialize flow: it's the switch between request and reply phase");
						transactionStatus = TransactionStatus.REPLY_PHASE;
						//endOfRequestPhase = lastActivity;
						startOfReply = now;
						endOfReply = now; // may be changed later
						//startOfReplyPhase = now;
						lastActivity = now;
						transactionReplySize += packet.getLayer4length();
						result.replySize += packet.getLayer4length();
						break;
					case REPLY_PHASE: // REPLY+REPLY: packet should belong to the last reply or be a new reply (delay decides)
						assert lastActivity != UNKNOWN;
						long timeSinceLastReply = now - lastActivity;
						if (timeSinceLastReply < MULTIPART_REPLY_THRESHOLD) { // assume that packet belongs to current reply
							lastActivity = now;
							endOfReply = now;
							transactionReplySize += packet.getLayer4length();
							result.replySize += packet.getLayer4length();
							break;
						} else { // assume that packet belongs to a new reply
							// finish last reply:
							startReplyOffsets.add(startOfReply);
							endReplyOffsets.add(endOfReply);
							replySizes.add(transactionReplySize);
							startOfReply = now;
							endOfReply = now; // may be changed later
							lastActivity = now;
							// add new Reply:
							transactionReplySize = packet.getLayer4length();
							result.replySize += packet.getLayer4length();
						}
						//System.out.println("serialize flow: status: REPLY_PHASE"); 
						//long timeSinceLastReply = now - lastActivity;
				}
			}
		}
		
		// handle open transactions
		if (transactionStatus == TransactionStatus.REQUEST_PHASE && transactionRequestSize != 0) {
			//System.out.println("serialize flow: handle open transaction: REQUEST_PHASE");
			result.transactions.add(new ExtendedTransaction(
					(int) (startOfRequest - endOfLastTransaction),
					startOfRequest,
					endOfRequest,
					transactionRequestSize,
					result.receiverID, 
					null, 
					null,
					null
				));
			endOfLastTransaction = endOfRequest;
		} else if (transactionStatus == TransactionStatus.REPLY_PHASE) {
			//System.out.println("serialize flow: handle open transaction: REPLY_PHASE");
			assert startOfReply != UNKNOWN;
			replySizes.add(transactionReplySize);
			startReplyOffsets.add(startOfReply);
			endReplyOffsets.add(endOfReply);
			result.transactions.add(new ExtendedTransaction(
					(int) (startOfRequest - endOfLastTransaction),
					startOfRequest,
					endOfRequest,
					transactionRequestSize,
					result.receiverID,
					Util.toLongArray(startReplyOffsets),
					Util.toLongArray(endReplyOffsets),
					Util.toIntArray(replySizes)
				));
			endOfLastTransaction = endOfReply;
		}
			
		// serialize results
		if (result.transactions.size() != 0) {
			try {
				result.endOfFlow = lastActivity;
				assert result.endOfFlow >= result.startOfFlow;
				result.serialize(resultTrace);
				resultTrace.write("\n");
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("ERROR: could not wrtie flow to trace file "  +resultTrace);
			}
		}
		//System.out.println("resulting transactions: ");
		//for (ApplicationLevelMessage transaction: result.transactions)
		//	System.out.println(transaction); 
		//System.out.println("serialize flow: finsihed");
	}

}

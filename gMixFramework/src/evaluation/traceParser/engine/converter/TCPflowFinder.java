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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import evaluation.traceParser.engine.dataStructure.Packet;
import evaluation.traceParser.engine.dataStructure.Packet.TCPflags;
import evaluation.traceParser.engine.fileReader.PacketIterator;
import evaluation.traceParser.engine.fileReader.PacketSource;
import evaluation.traceParser.engine.filter.PacketFilter;


public class TCPflowFinder implements PacketFilter {

	private HashMap<String, FlowRecord> tempFlows = new HashMap<String, FlowRecord>(250000);
	private Vector<FlowRecord> removals = new Vector<FlowRecord>(10000);
	private PacketFilter filter;
	private HashSet<String> result = new HashSet<String>(250000);
	
	
	public TCPflowFinder() {
		this(null);
	}
	
	
	public TCPflowFinder(PacketFilter filter) {
		this.filter = filter;
	}
	
	
	@Override
	public Packet newRecord(Packet packet) {
		// clean up:
		if (packet.getSequenceNumber() % 1000000 == 0) { // remove dead entries
			for (FlowRecord flow:tempFlows.values())
				if ( (packet.getTimestamp().getTimeInMillis() - flow.startOfFlow) > 5000
					&& !flow.handshakeComplete
						)  // no complete handshake after 5 seconds
					removals.add(flow);
			System.out.println("TCPflowFinder: read " +packet.getSequenceNumber() +" packets so far. hm-size: " +tempFlows.size() +" - " +removals.size());
			for (FlowRecord flowToRemove:removals)
				tempFlows.remove(flowToRemove);
			removals.clear();
		}
		
		// filter:
		if (!isAllowed(packet)) // ignore packets filtered by white list
			return null;
		boolean isSyn = isSyn(packet);
		boolean isFin = isFin(packet);
		if (!isSyn && !isFin) // ignore none connection establish/teardown packets
			return null;
		
		// extract information:
		String flowIdentifier = getFlowIdentifier(packet);
		FlowRecord flow = tempFlows.get(flowIdentifier);
		if (isSyn && flow == null) { // first syn
			flow = new FlowRecord();
			flow.startOfFlow = packet.getTimestamp().getTimeInMillis();
			tempFlows.put(flowIdentifier, flow);
		} else if (isSyn) { // second syn
			flow.handshakeComplete = true;
		} else if (isFin && flow == null) { // fin for unknown connection (malformed packet or start of flow not covered in trace)
			return null;
		} else if (isFin && flow.finCtr == 0) { // first fin
			flow.finCtr++;
		} else if (isFin && flow.finCtr == 1) { // second fin -> store flow identifier
			tempFlows.remove(flowIdentifier);
			result.add(flowIdentifier);
		} else
			System.err.println("should not happen");
		
		return packet;
	}

	
	@Override
	public void finished() {
		tempFlows.clear();
		tempFlows = null;
		removals.clear();
		removals = null;
		if (filter != null)
			filter.finished(); 
	}
	
	
	private boolean isAllowed(Packet packet) {
		if (filter == null)
			return true;
		return filter.newRecord(packet) != null;
	}

	
	private boolean isSyn(Packet packet) {
		if (packet.getTCPflags() == TCPflags.SYN)
			return true;
		if (packet.getTCPflags() == TCPflags.SYN_ACK)
			return true;
		return false;
	}
	
	
	private boolean isFin(Packet packet) {
		if (packet.getTCPflags() == TCPflags.FIN)
			return true;
		if (packet.getTCPflags() == TCPflags.FIN_ACK)
			return true;
		return false;
	}

	
	private class FlowRecord {
		long startOfFlow;
		int finCtr = 0;
		boolean handshakeComplete;
		
	}
	
	
	public HashSet<String> getFlows() {
		return result;
	}
	
	
	// not able to distinguish between 2 different flows, that are established between the same two hosts, with opposite ports (e.g. both run a web server on port 80 + both operating systems choose port 60015 for the client side... should happen very rarely)
	public static String getFlowIdentifier(Packet packet) {
		if (packet.getLayer2srcAddress().compareTo(packet.getLayer2dstAddress()) < 0) // src address is smaller
			return packet.getLayer2srcAddress() +":" +packet.getLayer3srcAddress() +"-" +packet.getLayer2dstAddress() +":" +packet.getLayer3dstAddress();
		else
			return packet.getLayer2dstAddress() +":" +packet.getLayer3dstAddress() +"-" +packet.getLayer2srcAddress() +":" +packet.getLayer3srcAddress();
	}
	
	
	public static HashSet<String> findFlows(PacketSource packetSource) {
		return findFlows(packetSource, null);
	}
	
	
	public static HashSet<String> findFlows(PacketSource packetSource, PacketFilter filter) {
		TCPflowFinder flowFinder = new TCPflowFinder(filter);
		PacketIterator iterator = new PacketIterator(packetSource, flowFinder);
		while (iterator.hasNext()) {
			iterator.next();
		}
		packetSource.close();
		if (filter != null)
			filter.finished();
		return flowFinder.getFlows();
	}
	
}

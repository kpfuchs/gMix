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
package evaluation.traceParser.engine.fileReader;

import java.io.IOException;
import java.io.InputStream;

import evaluation.traceParser.engine.Protocol;
import evaluation.traceParser.engine.TraceInfo;
import evaluation.traceParser.engine.dataStructure.Packet;
import evaluation.traceParser.engine.dataStructure.Flow.FlowDirection;
import evaluation.traceParser.engine.dataStructure.Packet.TCPflags;
import evaluation.traceParser.engine.protocolHandler.ProtocolHandler;
import evaluation.traceParser.engine.protocolHandler.ProtocolHandler.LengthAccuracy;
import evaluation.traceParser.engine.protocolHeaderParser.TCPpacket;


public class DynamicPacketReader extends PacketReader {

	private TraceInfo traceInfo;
	private final String WAN_ADDRESS; // mac
	private final String LAN_ADDRESS; // mac
	private long seqNumber = 0;
	private long read = 0;
	
	
	/**
	 * Packet reader that create Packets from any packet format with a 
	 * handler available (see enum Protocol.java).
	 * 
	 * @param sourceTraceFile
	 * @param traceInfo
	 */
	public DynamicPacketReader(InputStream sourceTraceFile, TraceInfo traceInfo) {
		super(sourceTraceFile);
		this.traceInfo = traceInfo;
		this.WAN_ADDRESS = traceInfo.getWanAddress();
		this.LAN_ADDRESS = traceInfo.getLanAddress();
	}
	
	
	@Override
	public Packet readPacket() throws IOException {
		return readPack(new Packet());
	}


	@Override
	public Packet readPacket(Packet reusePacket) throws IOException {
		reusePacket.reuse();
		return readPack(reusePacket);
	}	

	
	// TODO: support direct layer 2 records (layer 0 contains layer 2 packets... e.g. ERFtype = TYPE_IPV4)
	private Packet readPack(Packet packet) throws IOException {
		
		// layer 0:
		byte[] layer0payload;
		byte[] layer0packet;
		ProtocolHandler layer0handler = traceInfo.getTraceFormat().getProtocolHandler();
		assert layer0handler != null;
		layer0packet = layer0handler.readPacket(sourceTraceFile);
		if (layer0packet == null)
			return null;
		read += layer0packet.length;
		//packet.setRaw(layer0packet);
		layer0payload = layer0handler.getPayload(layer0packet);
		//packet.setPayload(layer0payload);
		packet.setSequenceNumber(++seqNumber);
		packet.setTimestamp(layer0handler.getTimestamp(layer0packet));
		assert packet.getTimestamp() != null;
		packet.setLayer0protocol(traceInfo.getTraceFormat());
		packet.setLayer1length(layer0handler.getPayloadLength(layer0packet));
		
		Protocol layer1protocol = layer0handler.getPayloadProtocol(layer0packet);
		packet.setLayer1protocol(layer1protocol);
		ProtocolHandler layer1handler = layer1protocol.getProtocolHandler();
		if (layer1protocol == Protocol.UNKNOWN || layer1handler == null)
			return packet;
		
		// layer 1:
		byte[] layer1packet = layer0payload;
		byte[] layer1payload;
		packet.setLayer2length(layer1handler.getPayloadLength(layer1packet));
		packet.setLayer1srcAddress(layer1handler.getSourceAddress(layer1packet));
		packet.setLayer1dstAddress(layer1handler.getDestinationAddress(layer1packet));
		if (WAN_ADDRESS != null) { // determine and store flow direction if possible
			if (packet.getLayer1srcAddress().equalsIgnoreCase(WAN_ADDRESS))
				packet.setFlowDirection(FlowDirection.FROM_WAN);
			else if (packet.getLayer1dstAddress().equalsIgnoreCase(WAN_ADDRESS))
				packet.setFlowDirection(FlowDirection.TO_WAN);
			else
				packet.setFlowDirection(FlowDirection.UNKNOWN);
		}
		if ((packet.getFlowDirection() == null || packet.getFlowDirection() == FlowDirection.UNKNOWN) && LAN_ADDRESS != null) {
			if (packet.getLayer1srcAddress().equalsIgnoreCase(LAN_ADDRESS))
				packet.setFlowDirection(FlowDirection.TO_WAN);
			else if (packet.getLayer1dstAddress().equalsIgnoreCase(LAN_ADDRESS))
				packet.setFlowDirection(FlowDirection.FROM_WAN);
			else
				packet.setFlowDirection(FlowDirection.UNKNOWN);
		}
		
		Protocol layer2protocol = layer1handler.getPayloadProtocol(layer1packet);
		packet.setLayer2protocol(layer2protocol);
		layer1payload = layer1handler.getPayload(layer1packet);
		//packet.setPayload(layer1payload);
		ProtocolHandler layer2handler = layer2protocol.getProtocolHandler();
		if (layer2protocol == Protocol.UNKNOWN || layer2handler == null)
			return packet;
		
		// layer 2:
		byte[] layer2packet = layer1payload;
		byte[] layer2payload;
		if (	layer1handler.getPayloadLengthAccuracy(layer1packet) != LengthAccuracy.EXACT // the layer below is not sure about the size
				&& layer2handler.getLengthAccuracy(layer2packet) == LengthAccuracy.EXACT // this layer knows the exact value
				)
			packet.setLayer2length(layer2handler.getLength(layer2packet));
		if (layer1handler.getPayloadLengthAccuracy(layer1packet) == LengthAccuracy.EXACT
			&& layer2handler.getLengthAccuracy(layer2packet) == LengthAccuracy.EXACT
				) {
			if (layer1handler.getPayloadLength(layer1packet) != layer2handler.getLength(layer2packet)) { // malformed packet
				return readPacket(packet);
			}
		}
		packet.setLayer3length(layer2handler.getPayloadLength(layer2packet));
		packet.setLayer2srcAddress(layer2handler.getSourceAddress(layer2packet));
		packet.setLayer2dstAddress(layer2handler.getDestinationAddress(layer2packet));
		
		Protocol layer3protocol = layer2handler.getPayloadProtocol(layer2packet);
		packet.setLayer3protocol(layer3protocol);
		layer2payload = layer2handler.getPayload(layer2packet);
		//packet.setPayload(layer2payload);
		ProtocolHandler layer3handler = layer3protocol.getProtocolHandler();
		if (layer3protocol == Protocol.UNKNOWN || layer3handler == null || layer2payload == null) {
			// try to find out about the actual size
			if (layer1handler.getPayloadLengthAccuracy(layer1packet) == LengthAccuracy.EXACT // the layer below is sure about the payload size
				&& layer2handler.getHeaderLengthAccuracy(layer2packet) == LengthAccuracy.EXACT // this layer is sure about the header size
				)
				packet.setLayer3length(layer1handler.getPayloadLength(layer1packet) - layer2handler.getHeaderLength(layer2packet));
			return packet;
		}
		
		// layer 3:
		byte[] layer3packet = layer2payload;
		byte[] layer3payload;
		if (	layer2handler.getPayloadLengthAccuracy(layer2packet) != LengthAccuracy.EXACT // the layer below is not sure about the size
				&& layer3handler.getLengthAccuracy(layer3packet) == LengthAccuracy.EXACT // this layer knows the exact value
				)
			packet.setLayer3length(layer3handler.getLength(layer3packet));
		if (layer2handler.getPayloadLengthAccuracy(layer2packet) == LengthAccuracy.EXACT
			&& layer3handler.getLengthAccuracy(layer3packet) == LengthAccuracy.EXACT
				) {
			if (layer2handler.getPayloadLength(layer2packet) != layer3handler.getLength(layer3packet)) { // malformed packet
				return readPacket(packet);
			}
		}
		packet.setLayer4length(layer3handler.getPayloadLength(layer3packet));
		packet.setLayer3srcAddress(layer3handler.getSourceAddress(layer3packet));
		packet.setLayer3dstAddress(layer3handler.getDestinationAddress(layer3packet));
		if (layer3protocol == Protocol.TCP) { // store handshake status if possible
			boolean syn = TCPpacket.getFlag_SYN(layer3packet);
			boolean ack = TCPpacket.getFlag_ACK(layer3packet);
			boolean fin = TCPpacket.getFlag_FIN(layer3packet);
			if (syn && ack)
				packet.setTCPflags(TCPflags.SYN_ACK);
			else if (fin && ack)
				packet.setTCPflags(TCPflags.FIN_ACK);
			else if (syn)
				packet.setTCPflags(TCPflags.SYN);
			else if (ack)
				packet.setTCPflags(TCPflags.ACK);
			else
				packet.setTCPflags(TCPflags.NONE);
		}
			
		Protocol layer4protocol = layer3handler.getPayloadProtocol(layer3packet);
		packet.setLayer4protocol(layer4protocol);
		layer3payload = layer3handler.getPayload(layer3packet);
		//packet.setPayload(layer3payload);
		ProtocolHandler layer4handler = layer4protocol.getProtocolHandler();
		if (layer4protocol == Protocol.UNKNOWN || layer4handler == null) {
			// try to find out about the actual size
			if (layer2handler.getPayloadLengthAccuracy(layer2packet) == LengthAccuracy.EXACT // the layer below is sure about the payload size
				&& layer3handler.getHeaderLengthAccuracy(layer3packet) == LengthAccuracy.EXACT // this layer is sure about the header size
				)
				packet.setLayer4length(layer2handler.getPayloadLength(layer2packet) - layer3handler.getHeaderLength(layer3packet));
			return packet;
		}
		
		// layer 4:
		byte[] layer4packet = layer3payload;
		//byte[] layer4payload;
		if (	layer3handler.getPayloadLengthAccuracy(layer3packet) != LengthAccuracy.EXACT // the layer below is not sure about the size
				&& layer4handler.getLengthAccuracy(layer4packet) == LengthAccuracy.EXACT // this layer knows the exact value
				) 
			packet.setLayer4length(layer4handler.getLength(layer4packet));
		if (layer3handler.getPayloadLengthAccuracy(layer3packet) == LengthAccuracy.EXACT
			&& layer4handler.getLengthAccuracy(layer4packet) == LengthAccuracy.EXACT
				) {
			if (layer3handler.getPayloadLength(layer3packet) != layer4handler.getLength(layer4packet)) { // malformed packet
				return readPacket(packet);
			}
		}
		//layer4payload = layer4handler.getPayload(layer4packet);
		//packet.setPayload(layer4payload);
		return packet;
	}


	@Override
	public long getTotalBytesRead() {
		return read;
	}

}

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
package evaluation.traceParser.engine.dataStructure;

import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;

import evaluation.traceParser.engine.Protocol;
import evaluation.traceParser.engine.dataStructure.Flow.FlowDirection;


public class Packet {
	
	public enum TCPflags {NONE, SYN, SYN_ACK, ACK, FIN, FIN_ACK};
	
	private long sequenceNumber;
	private Calendar timestamp;
	
	private String layer3srcAddress; // port
	private String layer3dstAddress;
	private String layer2srcAddress; // ipv4/ipv6
	private String layer2dstAddress;
	private String layer1srcAddress; // mac
	private String layer1dstAddress;
	
	private Protocol layer4protocol = null;
	private Protocol layer3protocol = null;
	private Protocol layer2protocol = null;
	private Protocol layer1protocol = null;
	private Protocol layer0protocol = null;
	
	private int layer4length;
	private int layer3length;
	private int layer2length;
	private int layer1length;
	
	private FlowDirection flowDirection = null;
	private TCPflags tcpFlags = null;
	
	//private byte[] payload;
	//private byte[] rawPacket;
	
	
	public Packet() {
		
	}
	
	public Packet(String serializedFlow) {
		init(serializedFlow);
	}
	
	
	public void init(String serializedFlow) {
		String[] fields = serializedFlow.split("#");
		if (fields.length != 19)
			System.err.println("unrecognized trace file format"); 
		this.sequenceNumber = Long.parseLong(fields[0]);
		this.timestamp = Calendar.getInstance();
		this.timestamp.setTimeInMillis(Long.parseLong(fields[1]));
		
		this.layer3srcAddress = fields[2];
		this.layer3dstAddress = fields[3];
		this.layer2srcAddress = fields[4];
		this.layer2dstAddress = fields[5];
		this.layer1srcAddress = fields[6];
		this.layer1dstAddress = fields[7];
		
		this.layer4protocol = Protocol.getProtocol(Integer.parseInt(fields[8]));
		this.layer3protocol = Protocol.getProtocol(Integer.parseInt(fields[9]));
		this.layer2protocol = Protocol.getProtocol(Integer.parseInt(fields[10]));
		this.layer1protocol = Protocol.getProtocol(Integer.parseInt(fields[11]));
		this.layer0protocol = Protocol.getProtocol(Integer.parseInt(fields[12]));
		
		this.layer4length = Integer.parseInt(fields[13]);
		this.layer3length = Integer.parseInt(fields[14]);
		this.layer2length = Integer.parseInt(fields[15]);
		this.layer1length = Integer.parseInt(fields[16]);
		
		this.flowDirection = FlowDirection.values()[Integer.parseInt(fields[17])];
		this.tcpFlags = TCPflags.values()[Integer.parseInt(fields[18])];
	}
	
	
	public void serialize(Writer destination) throws IOException {
		destination.write(sequenceNumber +"#");
		destination.write(timestamp.getTimeInMillis() +"#");
		
		destination.write(layer3srcAddress +"#");
		destination.write(layer3dstAddress +"#");
		destination.write(layer2srcAddress +"#");
		destination.write(layer2dstAddress +"#");
		destination.write(layer1srcAddress +"#");
		destination.write(layer1dstAddress +"#");
		
		if (layer4protocol == null)
			destination.write(Protocol.UNKNOWN.ordinal() +"#");
		else
			destination.write(layer4protocol.ordinal() +"#");
		if (layer3protocol == null)
			destination.write(Protocol.UNKNOWN.ordinal() +"#");
		else
			destination.write(layer3protocol.ordinal() +"#");
		if (layer2protocol == null)
			destination.write(Protocol.UNKNOWN.ordinal() +"#");
		else
			destination.write(layer2protocol.ordinal() +"#");
		if (layer1protocol == null)
			destination.write(Protocol.UNKNOWN.ordinal() +"#");
		else
			destination.write(layer1protocol.ordinal() +"#");
		if (layer0protocol == null)
			destination.write(Protocol.UNKNOWN.ordinal() +"#");
		else
			destination.write(layer0protocol.ordinal() +"#");
		
		destination.write(layer4length +"#");
		destination.write(layer3length +"#");
		destination.write(layer2length +"#");
		destination.write(layer1length +"#");
		
		if (flowDirection == null)
			destination.write(FlowDirection.UNKNOWN.ordinal() +"#");
		else
			destination.write(flowDirection.ordinal() +"#");
		if (tcpFlags == null)
			destination.write(TCPflags.NONE.ordinal() +"#");
		else
			destination.write(tcpFlags.ordinal() +"#");
		destination.write("\n");
	}

	
	public static Protocol getProtocol(int code) {
		Protocol[] pArray = Protocol.values();
		if (code >= pArray.length)
			return null;
		else
			return pArray[code];
	}

	
	public void reuse() {
		this.sequenceNumber = 0;
		this.timestamp = null;
		
		this.layer3srcAddress = null;
		this.layer3dstAddress = null;
		this.layer2srcAddress = null;
		this.layer2dstAddress = null;
		this.layer1srcAddress = null;
		this.layer1dstAddress = null;
		
		this.layer4protocol = null;
		this.layer3protocol = null;
		this.layer2protocol = null;
		this.layer1protocol = null;
		this.layer0protocol = null;
		
		this.layer4length = 0;
		this.layer3length = 0;
		this.layer2length = 0;
		this.layer1length = 0;
		
		this.flowDirection = null;
		this.tcpFlags = null;
		
		//this.payload = null;
		//this.rawPacket = null;
	}
	
	
	public long getSequenceNumber() {
		return sequenceNumber;
	}


	public void setSequenceNumber(long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}


	public Calendar getTimestamp() {
		return timestamp;
	}


	public void setTimestamp(Calendar timestamp) {
		this.timestamp = timestamp;
	}


	public String getLayer3srcAddress() {
		return layer3srcAddress;
	}


	public void setLayer3srcAddress(String layer3srcAddress) {
		this.layer3srcAddress = layer3srcAddress;
	}


	public String getLayer3dstAddress() {
		return layer3dstAddress;
	}


	public void setLayer3dstAddress(String layer3dstAddress) {
		this.layer3dstAddress = layer3dstAddress;
	}


	public String getLayer2srcAddress() {
		return layer2srcAddress;
	}


	public void setLayer2srcAddress(String layer2srcAddress) {
		this.layer2srcAddress = layer2srcAddress;
	}


	public String getLayer2dstAddress() {
		return layer2dstAddress;
	}


	public void setLayer2dstAddress(String layer2dstAddress) {
		this.layer2dstAddress = layer2dstAddress;
	}


	public String getLayer1srcAddress() {
		return layer1srcAddress;
	}


	public void setLayer1srcAddress(String layer1srcAddress) {
		this.layer1srcAddress = layer1srcAddress;
	}


	public String getLayer1dstAddress() {
		return layer1dstAddress;
	}


	public void setLayer1dstAddress(String layer1dstAddress) {
		this.layer1dstAddress = layer1dstAddress;
	}


	public Protocol getLayer4protocol() {
		return layer4protocol;
	}


	public void setLayer4protocol(Protocol layer4protocol) {
		this.layer4protocol = layer4protocol;
	}


	public Protocol getLayer3protocol() {
		return layer3protocol;
	}


	public void setLayer3protocol(Protocol layer3protocol) {
		this.layer3protocol = layer3protocol;
	}


	public Protocol getLayer2protocol() {
		return layer2protocol;
	}


	public void setLayer2protocol(Protocol layer2protocol) {
		this.layer2protocol = layer2protocol;
	}


	public Protocol getLayer1protocol() {
		return layer1protocol;
	}


	public void setLayer1protocol(Protocol layer1protocol) {
		this.layer1protocol = layer1protocol;
	}


	public Protocol getLayer0protocol() {
		return layer0protocol;
	}


	public void setLayer0protocol(Protocol layer0protocol) {
		this.layer0protocol = layer0protocol;
	}


	/**
	 * length of the layer 4 data including headers (i.e. the payload length of 
	 * the layer 3 packet)
	 * @return
	 */
	public int getLayer4length() {
		return layer4length;
	}


	public void setLayer4length(int layer4length) {
		this.layer4length = layer4length;
	}


	public int getLayer3length() {
		return layer3length;
	}


	public void setLayer3length(int layer3length) {
		this.layer3length = layer3length;
	}


	public int getLayer2length() {
		return layer2length;
	}


	public void setLayer2length(int layer2length) {
		this.layer2length = layer2length;
	}


	public int getLayer1length() {
		return layer1length;
	}


	public void setLayer1length(int layer1length) {
		this.layer1length = layer1length;
	}

	
	
	/*public byte[] getPayload() {
		return this.payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}
	
	
	public byte[] getRaw() {
		return this.rawPacket;
	}

	public void setRaw(byte[] rawPacket) {
		this.rawPacket = rawPacket;
	}*/
	
	
	public Protocol getHighestLevelProtocol() {
		if (layer4protocol != null && layer4protocol != Protocol.UNKNOWN)
			return layer4protocol;
		else if (layer3protocol != null && layer3protocol != Protocol.UNKNOWN)
			return layer3protocol;
		else if (layer2protocol != null && layer2protocol != Protocol.UNKNOWN)
			return layer2protocol;
		else if (layer1protocol != null && layer1protocol != Protocol.UNKNOWN)
			return layer1protocol;
		else // (layer0protocol != null && layer0protocol != Protocol.UNKNOWN)
			return layer0protocol;
	}
	
	
	public FlowDirection getFlowDirection() {
		if (flowDirection == null)
			return FlowDirection.UNKNOWN;
		return flowDirection;
	}


	public void setFlowDirection(FlowDirection flowDirection) {
		this.flowDirection = flowDirection;
	}
	
	
	public TCPflags getTCPflags() {
		return tcpFlags;
	}


	public void setTCPflags(TCPflags tcpFlags) {
		this.tcpFlags = tcpFlags;
	}
	
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Packet #" +sequenceNumber);
		if (timestamp != null)
			sb.append(", Time: " +timestamp.getTimeInMillis() 
					+"(" +timestamp.getTime() 
					+" and " +timestamp.get(Calendar.MILLISECOND)  +"ms)"
				);
		sb.append(", Source: ");
		if (layer1srcAddress == null)
			sb.append("unknown");
		else
			sb.append(layer1srcAddress);
		if (layer2srcAddress != null)
			sb.append("->" +layer2srcAddress);
		if (layer3srcAddress != null)
			sb.append("->" +layer3srcAddress);
		sb.append(", Destination: ");
		if (layer1dstAddress == null)
			sb.append("unknown");
		else
			sb.append(layer1dstAddress);
		if (layer2dstAddress != null)
			sb.append("->" +layer2dstAddress);
		if (layer3dstAddress != null)
			sb.append("->" +layer3dstAddress);
		sb.append(", Protocol: ");
		sb.append("(" +layer0protocol 
				+")->" +layer1protocol 
				+"->" +layer2protocol 
				+"->" +layer3protocol 
				+"->" +layer4protocol
				);
		sb.append(", Length:"
				+" layer1:"  +layer1length 
				+"->layer2:" +layer2length 
				+"->layer3:" +layer3length 
				+"->layer4:" +layer4length
				);
		if (flowDirection != null)
			sb.append(", flow direction: " +flowDirection);
		if (tcpFlags != null)
			sb.append(", tcp flags: " +tcpFlags);
		sb.append(", Payload: ");
		/*if (payload == null)
			sb.append("none");
		else
			sb.append(payload.length +"bytes");
		sb.append(", Raw packet: ");
		if (rawPacket == null)
			sb.append("none");
		else
			sb.append(rawPacket.length +"bytes");*/
		return sb.toString();
	}

}

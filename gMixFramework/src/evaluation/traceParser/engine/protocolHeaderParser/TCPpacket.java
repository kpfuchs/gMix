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
package evaluation.traceParser.engine.protocolHeaderParser;

import java.util.Arrays;

import evaluation.traceParser.engine.protocolHandler.ProtocolHandler;
import framework.core.util.Util;


public class TCPpacket {

	
	public static int getSourcePort(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 0, 2));
	}
	
	
	public static int getDestinationPort(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 2, 4));
	}
	
	
	public static long getSequenceNumber(byte[] packet) {
		return Util.unsignedIntToLong(Arrays.copyOfRange(packet, 4, 8));
	}
	
	
	public static long getAcknowledgmentNumber(byte[] packet) {
		return Util.unsignedIntToLong(Arrays.copyOfRange(packet, 8, 12));
	}
	
	
	public static int getHeaderLengthStat(byte[] packet) {
		return 4 * ((packet[12] & (0xf0)) >> 4);
	}
	
	
	public static boolean getFlag_NS(byte[] packet) {
		return Util.getBitAt(0, packet[12]);
	}
	
	
	public static boolean getFlag_CWR(byte[] packet) {
		return Util.getBitAt(7, packet[13]);
	}
	
	
	public static boolean getFlag_ECE(byte[] packet) {
		return Util.getBitAt(6, packet[13]);
	}
	
	
	public static boolean getFlag_URG(byte[] packet) {
		return Util.getBitAt(5, packet[13]);
	}
	
	
	public static boolean getFlag_ACK(byte[] packet) {
		return Util.getBitAt(4, packet[13]);
	}
	
	public static boolean getFlag_PSH(byte[] packet) {
		return Util.getBitAt(3, packet[13]);
	}
	
	
	public static boolean getFlag_RST(byte[] packet) {
		return Util.getBitAt(2, packet[13]);
	}
	
	
	public static boolean getFlag_SYN(byte[] packet) {
		return Util.getBitAt(1, packet[13]);
	}
	
	
	public static boolean getFlag_FIN(byte[] packet) {
		return Util.getBitAt(0, packet[13]);
	}
	
	
	public static int getWindowSize(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 14, 16));
	}
	
	
	public static int getCheckSum(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 16, 18));
	}
	
	
	public static int getUrgentPointer(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 18, 20));
	}
	
	
	public static byte[] getOptions(byte[] packet) {
		int headerLength = getHeaderLengthStat(packet);
		if (headerLength <= 20)
			return null;
		return Arrays.copyOfRange(packet, 20, headerLength);
	}
	
	
	public static int getPayloadLengthStat(byte[] packet) {
		int headerLength = getHeaderLengthStat(packet);
		int availableData = packet.length - headerLength;
		if (availableData <= 0) // truncated packet
			return ProtocolHandler.UNKNOWN;
		else
			return packet.length - headerLength;
	}
	
	
	public static byte[] getPayloadStat(byte[] packet) {
		int headerLength = getHeaderLengthStat(packet);
		int availableData = packet.length - headerLength;
		if (availableData <= 0) // truncated packet
			return null;
		return Arrays.copyOfRange(packet, headerLength, packet.length);
	}
	
	
	public static String toString(byte[] packet) {
		StringBuffer sb = new StringBuffer();
		sb.append("TCP header: \n");
		sb.append(" source port: " +TCPpacket.getSourcePort(packet) +"\n");
		sb.append(" destination port: " +TCPpacket.getDestinationPort(packet) +"\n");
		sb.append(" sequence number: " +TCPpacket.getSequenceNumber(packet) +"\n");
		sb.append(" ack number: " +TCPpacket.getAcknowledgmentNumber(packet) +"\n");
		sb.append(" header length: " +TCPpacket.getHeaderLengthStat(packet) +"\n");
		sb.append(" flag NS: " +TCPpacket.getFlag_NS(packet) +"\n");
		sb.append(" flag CWR: " +TCPpacket.getFlag_CWR(packet) +"\n");
		sb.append(" flag ECE: " +TCPpacket.getFlag_ECE(packet) +"\n");
		sb.append(" flag URG: " +TCPpacket.getFlag_URG(packet) +"\n");
		sb.append(" flag ACK: " +TCPpacket.getFlag_ACK(packet) +"\n");
		sb.append(" flag PSH: " +TCPpacket.getFlag_PSH(packet) +"\n");
		sb.append(" flag RST: " +TCPpacket.getFlag_RST(packet) +"\n");
		sb.append(" flag SYN: " +TCPpacket.getFlag_SYN(packet) +"\n");
		sb.append(" flag FIN: " +TCPpacket.getFlag_FIN(packet) +"\n");
		sb.append(" window size: " +TCPpacket.getWindowSize(packet) +"\n");
		sb.append(" checksum: " +TCPpacket.getCheckSum(packet) +"\n");
		sb.append(" urgent pointer: " +TCPpacket.getUrgentPointer(packet) +"\n");
		byte[] options = TCPpacket.getOptions(packet);
		if (options != null)
			sb.append(" options: " +Util.toHex(options) +"\n");
		byte[] payload = TCPpacket.getPayloadStat(packet);
		if (payload == null)
			sb.append(" payload: none");
		else
			sb.append(" payload (" +payload.length +" bytes): " +Util.toHex(payload));
		return sb.toString();
	}

}

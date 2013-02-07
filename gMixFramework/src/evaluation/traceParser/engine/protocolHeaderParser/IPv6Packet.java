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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Vector;

import evaluation.traceParser.engine.protocolHeaderParser.IPpacket.IPprotocol;
import framework.core.util.Util;


public class IPv6Packet {

	
	public static int getVersion(byte[] packet) {
		return (packet[0] & (0xf0)) >> 4;
	}
	
	
	public static int getTrafficClass(byte[] packet) {
		int result = Util.unsignedShortToInt(Arrays.copyOfRange(packet, 0, 2));
		return (result >> 4) & 0xffff;
	}
	
	
	public static int getPayloadLengthIncludingExtensionHeaders(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 4, 6));
	}
	
	
	public static IPprotocol getProtocol(byte[] packet) { // TODO: skip extension headers if present
		return IPpacket.getIPprotocolNumberByByte(packet[6]);
	}
	
	
	public static int getProtocolAsInt(byte[] packet) {
		return Util.unsignedByteToShort(packet[6]);
	}
	
	
	public static String getProtocolAsHex(byte[] packet) {
		return "0x" +String.format("%02X", packet[6]);
	}
	
	
	public static int getNextHeader(byte[] packet) {
		return getProtocolAsInt(packet);
	}
	
	
	public static int getHopLimit(byte[] packet) {
		return Util.unsignedByteToShort(packet[7]);
	}
	
	
	public static InetAddress getSrcIP(byte[] packet) {
		try {
			return InetAddress.getByAddress(Arrays.copyOfRange(packet, 8, 24));
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static String getSrcIPasString(byte[] packet) {
		try {
			return InetAddress.getByAddress(Arrays.copyOfRange(packet, 8, 24)).getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
	}

	
	public static InetAddress getDstIP(byte[] packet) {
		try {
			return InetAddress.getByAddress(Arrays.copyOfRange(packet, 24, 40));
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static String getDstIPasString(byte[] packet) {
		try {
			return InetAddress.getByAddress(Arrays.copyOfRange(packet, 24, 40)).getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static int getHeaderLengthStat(byte[] packet) {
		if (!IPpacket.isIPv6ExtensionHeader(getProtocolAsInt(packet))) // no extension headers -> return basic length (= 40 bytes)
			return 40;
		int headerOffset = 40;
		while (true) {
			int type = Util.unsignedByteToShort(packet[headerOffset]);
			if (IPpacket.isIPv6ExtensionHeader(type))
				headerOffset += (8 + Util.unsignedByteToShort(packet[headerOffset+1]));
			else
				break;
		}
		return headerOffset;
	}
	
	
	public static boolean containsExtensionHeader(byte[] packet) {
		return IPpacket.isIPv6ExtensionHeader(getProtocolAsInt(packet));
	}
	
	
	public static byte[][] getExtensionHeaders(byte[] packet) {
		if (!containsExtensionHeader(packet))
			return null;
		Vector<byte[]> extensionHeaders = new Vector<byte[]>();
		int headerOffset = 40;
		while (true) {
			int type = Util.unsignedByteToShort(packet[headerOffset]);
			if (IPpacket.isIPv6ExtensionHeader(type)) {
				int len = 8 + Util.unsignedByteToShort(packet[headerOffset+1]);
				extensionHeaders.add(Arrays.copyOfRange(packet, headerOffset, headerOffset + len));
				headerOffset += len;
			} else
				break;
		}
		byte[][] result = new byte[extensionHeaders.size()][];
		for (int i=0; i<result.length; i++) {
			result[i] = extensionHeaders.get(i);
		} 
		return result;
	}
	
	
	/**
	 * does not count extension headers as payload
	 * @param packet
	 * @return
	 */
	public static boolean containsPayload(byte[] packet) {
		return packet.length > getHeaderLengthStat(packet);
	}
	
	
	public static byte[] getPayloadStat(byte[] packet) {
		int totalHeaderLength = getHeaderLengthStat(packet); // including extension headers
		if (packet.length <= totalHeaderLength) { // truncated packet; no payload
			return null;
		} else {
			int expectedActualPayloadLength = getPayloadLengthWithoutExtensionHeaders(packet);
			int availableData = packet.length - totalHeaderLength; // might include padding (!)
			if (availableData == expectedActualPayloadLength) // no padding included
				return Arrays.copyOfRange(packet, totalHeaderLength, packet.length);
			else if (availableData < expectedActualPayloadLength) // truncated packet -> return available data only (yes we could exchange "==" with "<=" above, but than the code is less intuitive...)
				return Arrays.copyOfRange(packet, totalHeaderLength, packet.length);
			else // availableData < expectedActualPayloadLength -> padding included
				return Arrays.copyOfRange(packet, totalHeaderLength, totalHeaderLength + expectedActualPayloadLength);
		}
	}
	
	
	public static int getPayloadLengthWithoutExtensionHeaders(byte[] packet) {
		return IPv6Packet.getPayloadLengthIncludingExtensionHeaders(packet) - (getHeaderLengthStat(packet) - 40); // payload length WITHOUT extension headers (40 is the basic header length) as indicated by the payload length field
	}
	
	
	
	public static String toString(byte[] packet) {
		StringBuffer sb = new StringBuffer();
		sb.append("IPv6 header: \n");
		sb.append(" version: " +IPv6Packet.getVersion(packet) +"\n");
		sb.append(" traffic class: " +IPv6Packet.getTrafficClass(packet) +"\n");
		sb.append(" payload length (incl. ext. headers): " +IPv6Packet.getPayloadLengthIncludingExtensionHeaders(packet) +"\n");
		sb.append(" next header: " +IPv6Packet.getProtocol(packet) +"\n");
		sb.append(" hop limit: " +IPv6Packet.getHopLimit(packet) +"\n");
		sb.append(" source ip: " +IPv6Packet.getSrcIPasString(packet) +"\n");
		sb.append(" destinatin ip: " +IPv6Packet.getDstIPasString(packet) +"\n");
		boolean containsExtensionHeaders = IPv6Packet.containsExtensionHeader(packet);
		sb.append(" contains extension headers: " +containsExtensionHeaders +"\n");
		if (containsExtensionHeaders) {
			byte[][] extensionHeaders =  IPv6Packet.getExtensionHeaders(packet);
			for (int i=0; i<extensionHeaders.length; i++) {
				sb.append("   header " +i +": " +Util.toHex(extensionHeaders[i]) +"\n");
			} 
		}
		byte[] payload = IPv6Packet.getPayloadStat(packet);
		if (payload == null)
			sb.append(" payload: none");
		else
			sb.append(" payload (" +payload.length +" bytes): " +Util.toHex(payload));
		return sb.toString();
	}

}

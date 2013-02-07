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

import evaluation.traceParser.engine.protocolHeaderParser.IPpacket.IPprotocol;
import framework.core.util.Util;


public class IPv4Packet {

	
	public static int getVersion(byte[] packet) {
		return (packet[0] & (0xf0)) >> 4;
	}
	
	
	public static int getHeaderLengthStat(byte[] packet) {
		return (packet[0] & (0x0f)) * 4;
	}
	
	
	public static int getDifferentiatedServicesCodePoint(byte[] packet) {
		return packet[1] & (0x0f);
	}
	
	
	public static boolean getECN_CT(byte[] packet) {
		return Util.getBitAt(1, packet[1]);
	}
	
	
	public static boolean getECN_CE(byte[] packet) {
		return Util.getBitAt(0, packet[1]);
	}
	
	
	/**
	 * header length + payload length in byte
	 */
	public static int getTotalLength(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 2, 4));
	}
	
	
	public static int getIdentification(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 4, 6));
	}
	
	
	public static boolean getFlagsReservedBit(byte[] packet) {
		return Util.getBitAt(5, packet[6]);
	}
	
	
	public static boolean getFlagsDontFragment(byte[] packet) {
		return Util.getBitAt(6, packet[6]);
	}
	
	
	public static boolean getFlagsMoreFragments(byte[] packet) {
		return Util.getBitAt(7, packet[6]);
	}
	
	
	/**
	 * returns thy byte index (not bit index)
	 */
	public static int getFragmentOffset(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 6, 8)) & 0x1fff;
	}
	
	
	public static int getTTL(byte[] packet) {
		return Util.unsignedByteToShort(packet[8]);
	}
	
	
	public static IPprotocol getProtocol(byte[] packet) {
		return IPpacket.getIPprotocolNumberByByte(packet[9]);
	}
	
	
	public static int getProtocolAsInt(byte[] packet) {
		return Util.unsignedByteToShort(packet[9]);
	}
	
	
	public static String getProtocolAsHex(byte[] packet) {
		return "0x" +String.format("%02X", packet[9]);
	}
	
	
	public static byte[] getHeaderCheckSum(byte[] packet) {
		return Arrays.copyOfRange(packet, 10, 12);
	}
	
	
	public static byte[] calculateHeaderCheckSum(byte[] packet) {
		int headerLength = getHeaderLengthStat(packet);
		byte[] header = Arrays.copyOf(packet, headerLength);
		int result = 0;
		for (int i=0; i<header.length; i+=2)
			if (i != 10 && i != 11 ) { // ignore checksum field
				int pt1 = header[i] & 0xff;
				int pt2 = (i+1 < header.length) ? (header[i+1] & 0xff) : 0;
				result += (pt1 << 8) + pt2;
			}
		result = (result >> 16) + (result & 0xffff);
		result = result + (result >> 16);
		result = ~result & 0xffff;
		return Arrays.copyOfRange(Util.intToByteArray(result), 2, 4);
	}
	
	
	public static boolean isHeaderChecksumCorrect(byte[] packet) {
		return Arrays.equals(getHeaderCheckSum(packet), calculateHeaderCheckSum(packet));
	} 
	
	
	public static String getSrcIPasString(byte[] packet) {
		try {
			return InetAddress.getByAddress(Arrays.copyOfRange(packet, 12, 16)).getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static InetAddress getSrcIP(byte[] packet) {
		try {
			return InetAddress.getByAddress(Arrays.copyOfRange(packet, 12, 16));
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static String getDstIPasString(byte[] packet) {
		try {
			return InetAddress.getByAddress(Arrays.copyOfRange(packet, 16, 20)).getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static InetAddress getDstIP(byte[] packet) {
		try {
			return InetAddress.getByAddress(Arrays.copyOfRange(packet, 16, 20));
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static boolean hasOptionsSet(byte[] packet) {
		return getHeaderLengthStat(packet) != 20;
	}
	
	
	public static byte[] getOptions(byte[] packet) {
		int optionsLength = getHeaderLengthStat(packet) - 20;
		if (optionsLength == 0)
			return null;
		else
			return Arrays.copyOfRange(packet, 20, 20+optionsLength);
	}
	
	
	public static boolean containsPayload(byte[] packet) {
		return packet.length > getHeaderLengthStat(packet);
	}
	
	
	public static byte[] getPayloadStat(byte[] packet) {
		int headerLength = getHeaderLengthStat(packet);
		if (packet.length - headerLength <= 0) { // truncated packet; no payload
			return null;
		} else { // payload or fraction of payload available
			int expectedPayloadLength = getTotalLength(packet) - headerLength; // length according to header
			int availableData = packet.length - headerLength; // might include padding (!)
			if (availableData == expectedPayloadLength) // no padding included
				return Arrays.copyOfRange(packet, headerLength, packet.length);
			else if (availableData < expectedPayloadLength) // truncated packet -> return available data only (yes we could exchange "==" with "<=" above, but than the code is less intuitive...)
				return Arrays.copyOfRange(packet, headerLength, packet.length);
			else // availableData < expectedPayloadLength -> padding included
				return Arrays.copyOfRange(packet, headerLength, headerLength + expectedPayloadLength);
		}
	}
	
	
	public static int getPayloadLengthStat(byte[] packet) {
		int headerLength = getHeaderLengthStat(packet);
		return getTotalLength(packet) - headerLength; // length according to header
	}
	
	
	public static String toString(byte[] packet) {
		StringBuffer sb = new StringBuffer();
		sb.append("IPv4 header: \n");
		sb.append(" version: " +IPv4Packet.getVersion(packet) +"\n");
		sb.append(" header length: " +IPv4Packet.getHeaderLengthStat(packet) +" bytes\n");
		sb.append(" DSF: " +IPv4Packet.getDifferentiatedServicesCodePoint(packet) +"\n");
		sb.append(" total length: " +IPv4Packet.getTotalLength(packet) +"\n");
		sb.append(" identification: " +IPv4Packet.getIdentification(packet) +"\n");
		sb.append(" don't fragment: " +IPv4Packet.getFlagsDontFragment(packet) +"\n");
		sb.append(" more fragments: " +IPv4Packet.getFlagsMoreFragments(packet) +"\n");
		sb.append(" fragment offst: " +IPv4Packet.getFragmentOffset(packet) +"\n");
		sb.append(" TTL: " +IPv4Packet.getTTL(packet) +"\n");
		sb.append(" protocol: " +IPv4Packet.getProtocol(packet) +"\n");
		sb.append(" header checksum correct: " +IPv4Packet.isHeaderChecksumCorrect(packet) +"\n");
		sb.append(" source ip: " +IPv4Packet.getSrcIPasString(packet) +"\n");
		sb.append(" destinatin ip: " +IPv4Packet.getDstIPasString(packet) +"\n");
		boolean hasOptions = IPv4Packet.hasOptionsSet(packet);
		sb.append(" has options set: " +hasOptions +"\n");
		if (hasOptions)
			sb.append("\n options: " +Util.toHex(IPv4Packet.getOptions(packet)) +"\n");
		byte[] payload = IPv4Packet.getPayloadStat(packet);
		if (payload == null)
			sb.append(" payload: none");
		else
			sb.append(" payload (" +payload.length +" bytes): " +Util.toHex(payload));
		return sb.toString();
	}
	
}

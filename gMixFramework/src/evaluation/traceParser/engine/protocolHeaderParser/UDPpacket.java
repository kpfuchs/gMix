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
import framework.core.util.Util;


public class UDPpacket {

	
	public static int getSourcePort(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 0, 2));
	}
	
	
	public static int getDestinationPort(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 2, 4));
	}
	
	
	public static int getTotalLength(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 4, 6));
	}
	
	
	public static byte[] getCheckSum(byte[] packet) {
		return Arrays.copyOfRange(packet, 6, 8);
	}
	
	
	public static int getHeaderLengthStat(byte[] packet) {
		return getHeaderLengthStat();
	}
	
	
	public static int getHeaderLengthStat() {
		return 8;
	}
	
	
	public static int getPayloadLengthStat(byte[] packet) {
		return getTotalLength(packet) - getHeaderLengthStat();
	}
	
	public static byte[] getPayloadStat(byte[] packet) {
		int expectedLength = getTotalLength(packet) - getHeaderLengthStat();
		int actualLength = packet.length - getHeaderLengthStat();
		if (actualLength <= 0) // truncated packet; no payload
			return null;
		else if (actualLength == expectedLength)
			return Arrays.copyOfRange(packet, getHeaderLengthStat(), getHeaderLengthStat() + actualLength);
		else if (actualLength < expectedLength) // truncated payload
			return Arrays.copyOfRange(packet, getHeaderLengthStat(), packet.length);
		else // if (actualLength > expectedLength) // padding included (same action as with "==" case above; readability)
			return Arrays.copyOfRange(packet, getHeaderLengthStat(), getHeaderLengthStat() + expectedLength);
	}
	
	
	public static String toString(byte[] packet) {
		StringBuffer sb = new StringBuffer();
		sb.append("UDP header: \n");
		sb.append(" source port: " +UDPpacket.getSourcePort(packet) +"\n");
		sb.append(" destination port: " +UDPpacket.getDestinationPort(packet) +"\n");
		sb.append(" total length: " +UDPpacket.getTotalLength(packet) +"\n");
		sb.append(" checksum: " +Util.toHex(UDPpacket.getCheckSum(packet)) +"\n");
		byte[] payload = UDPpacket.getPayloadStat(packet);
		if (payload == null)
			sb.append(" payload: none");
		else
			sb.append(" payload (" +payload.length +" bytes): " +Util.toHex(payload));
		return sb.toString();
	}
	
}

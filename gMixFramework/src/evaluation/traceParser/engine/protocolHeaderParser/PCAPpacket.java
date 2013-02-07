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

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import evaluation.traceParser.engine.Protocol;
import framework.core.util.Util;


public class PCAPpacket {

	public static PCAPfileHeader pcapFileHeader = null;
	private final static int NOT_SET = -1;
	private static long timestamp = NOT_SET;
	private static long timestampMS = NOT_SET;
	private static int actualLength = NOT_SET;
	private static int origLength = NOT_SET;
	
	
	public static void readFileHeader(InputStream inputStream) throws IOException {
		pcapFileHeader = PCAPfileHeader.readHeader(inputStream);
	}
	
	
	public static byte[] readPCAPpacket(InputStream inputStream) throws IOException {
		byte[] tsRaw = pcapFileHeader.forceRead(inputStream, 4);
		if (tsRaw == null) // EOF
			return null;
		timestamp = Util.unsignedIntToLong(tsRaw);
		timestampMS = Util.unsignedIntToLong(pcapFileHeader.forceRead(inputStream, 4));
		actualLength = (int)Util.unsignedIntToLong(pcapFileHeader.forceRead(inputStream, 4));
		origLength = (int)Util.unsignedIntToLong(pcapFileHeader.forceRead(inputStream, 4));
		return Util.forceRead(inputStream, actualLength);
	}
	
	
	// unix time (ms passed since midnight, January 1, 1970 UTC; same format as returned by "System.currentTimeMillis()")
	public static long getTimestamp(byte[] packet) {
		if (timestamp == NOT_SET || timestampMS == NOT_SET)
			throw new RuntimeException("readPCAPpacket() must be called first"); 
		return (timestamp * 1000l) + Math.round((double)timestampMS/1000.0d);
	}
	
	
	public static long getTimestampMicroSeconds(byte[] packet) {
		if (timestampMS == NOT_SET)
			throw new RuntimeException("readPCAPpacket() must be called first"); 
		return timestampMS;
	}
	
	
	public static Calendar getTimestampAsCalendar(byte[] packet) {
		Calendar result = Calendar.getInstance();
		result.setTimeInMillis(getTimestamp(packet));
		return result;
	}


	public static int getPayloadLengthStat(byte[] rawPacket) {
		if (origLength == NOT_SET)
			throw new RuntimeException("readPCAPpacket() must be called first"); 
		return origLength;
	}


	public static byte[] getPayloadStat(byte[] rawPacket) {
		if (pcapFileHeader == null)
			throw new RuntimeException("readFileHeader() must be called first");
		return rawPacket;
	}


	public static Protocol getPayloadProtocol(byte[] rawPacket) {
		if (pcapFileHeader == null)
			throw new RuntimeException("readFileHeader() must be called first");
		return pcapFileHeader.getLinkLayerHeaderTypeAsProtocol();
	}
	
	
	public static String toString(byte[] rawPacket) {
		StringBuffer sb = new StringBuffer();
		sb.append("pcap packet header: \n");
		sb.append(" timestamp: " +PCAPpacket.getTimestampAsCalendar(rawPacket).getTime() +" (+" +timestampMS/1000d +"ms)"+"\n");
		sb.append(" actualLength: " +actualLength +" bytes\n");
		sb.append(" origLength: " +origLength +" bytes\n");
		byte[] payload = PCAPpacket.getPayloadStat(rawPacket);
		if (payload == null)
			sb.append(" payload: none");
		else
			sb.append(" payload (" +payload.length +" bytes): " +Util.toHex(payload));
		return sb.toString();
	}
}

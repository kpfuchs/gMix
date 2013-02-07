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
package evaluation.traceParser.engine.protocolHandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import evaluation.traceParser.engine.Protocol;
import evaluation.traceParser.engine.protocolHeaderParser.TCPpacket;


public class TCPhandler implements ProtocolHandler {

	public enum TCPhandshakeStatus {SYN_TRANSMITTED, SYN_ACK_TRANSMITTED, COMPLETE};
	public enum TCPteardownStatus {FIN1_TRANSMITTED, FIN2_TRANSMITTED, COMPLETE};
	
	
	@Override
	public Calendar getTimestamp(byte[] rawPacket) {
		return null;
	}


	@Override
	public String getSourceAddress(byte[] rawPacket) {
		return ""+TCPpacket.getSourcePort(rawPacket);
	}


	@Override
	public String getDestinationAddress(byte[] rawPacket) {
		return ""+TCPpacket.getDestinationPort(rawPacket);
	}


	@Override
	public LengthAccuracy getLengthAccuracy(byte[] rawPacket) {
		return LengthAccuracy.UNCLEAR;
	}
	
	
	@Override
	public int getLength(byte[] rawPacket) {
		int headerLength = TCPpacket.getHeaderLengthStat(rawPacket);
		int availableData = rawPacket.length - headerLength;
		if (availableData <= 0) // truncated packet
			return headerLength;
		else
			return rawPacket.length;
	}


	@Override
	public LengthAccuracy getHeaderLengthAccuracy(byte[] rawPacket) {
		return LengthAccuracy.EXACT;
	}


	@Override
	public int getHeaderLength(byte[] rawPacket) {
		return TCPpacket.getHeaderLengthStat(rawPacket);
	}
	
	
	@Override
	public LengthAccuracy getPayloadLengthAccuracy(byte[] rawPacket) {
		return LengthAccuracy.UNCLEAR;
	}
	
	
	@Override
	public int getPayloadLength(byte[] rawPacket) {
		return TCPpacket.getPayloadLengthStat(rawPacket);
	}
	
	
	@Override
	public byte[] getPayload(byte[] rawPacket) {
		return TCPpacket.getPayloadStat(rawPacket);
	}


	@Override
	public Protocol getPayloadProtocol(byte[] rawPacket) {
		int srcPort = TCPpacket.getSourcePort(rawPacket);
		Protocol protocol = Protocol.portToProtocol(srcPort);
		if (protocol != Protocol.UNKNOWN)
			return protocol;
		int dstPort = TCPpacket.getDestinationPort(rawPacket);
		return Protocol.portToProtocol(dstPort);
	}


	@Override
	public boolean canHandle(FileInputStream fis) {
		// TODO implement
		return false;
	}
	
	
	@Override
	public byte[] readPacket(InputStream is) throws IOException {
		throw new RuntimeException("cannot read raw packets"); 
	}
	
}

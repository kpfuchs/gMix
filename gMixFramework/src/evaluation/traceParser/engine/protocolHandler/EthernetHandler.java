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
import evaluation.traceParser.engine.protocolHeaderParser.EthernetFrame;


public class EthernetHandler implements ProtocolHandler {

	@Override
	public Calendar getTimestamp(byte[] rawPacket) {
		return null;
	}


	@Override
	public String getSourceAddress(byte[] rawPacket) {
		return EthernetFrame.getSrcMacAsString(rawPacket);
	}


	@Override
	public String getDestinationAddress(byte[] rawPacket) {
		return EthernetFrame.getDstMacAsString(rawPacket);
	}


	@Override
	public LengthAccuracy getLengthAccuracy(byte[] rawPacket) {
		if (EthernetFrame.isIEEE_802_3_Frame(rawPacket))
			return LengthAccuracy.EXACT;
		else
			return LengthAccuracy.UNCLEAR; // may be truncated and or padded
	}
	
	
	@Override
	public int getLength(byte[] rawPacket) {
		return EthernetFrame.getLengthStat(rawPacket);
	}

	
	@Override
	public LengthAccuracy getHeaderLengthAccuracy(byte[] rawPacket) {
		return LengthAccuracy.EXACT;
	}


	@Override
	public int getHeaderLength(byte[] rawPacket) {
		return EthernetFrame.getHeaderLengthStat(rawPacket);
	}
	
	
	@Override
	public LengthAccuracy getPayloadLengthAccuracy(byte[] rawPacket) {
		if (EthernetFrame.isIEEE_802_3_Frame(rawPacket))
			return LengthAccuracy.EXACT;
		else
			return LengthAccuracy.UNCLEAR; // may be truncated and or padded
	}
	
	
	@Override
	public int getPayloadLength(byte[] rawPacket) {
		return EthernetFrame.getPayloadLengthStat(rawPacket);
	}
	

	@Override
	public byte[] getPayload(byte[] rawPacket) {
		return EthernetFrame.getPayloadStat(rawPacket);
	}


	@Override
	public Protocol getPayloadProtocol(byte[] rawPacket) {
		int type = EthernetFrame.getEtherType(rawPacket);
		if (type == EthernetFrame.ETHER_TYPE_IP_V4)
			return Protocol.IPv4;
		else if (type == EthernetFrame.ETHER_TYPE_IP_V6)
			return Protocol.IPv6;
		else if (type == EthernetFrame.ETHER_TYPE_ARP)
			return Protocol.ARP;
		else 
			return Protocol.UNKNOWN;
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

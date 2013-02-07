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
import evaluation.traceParser.engine.protocolHeaderParser.IPpacket;
import evaluation.traceParser.engine.protocolHeaderParser.IPv4Packet;
import evaluation.traceParser.engine.protocolHeaderParser.IPv6Packet;
import evaluation.traceParser.engine.protocolHeaderParser.IPpacket.IPprotocol;


public class IPhandler implements ProtocolHandler {

	@Override
	public Calendar getTimestamp(byte[] rawPacket) {
		return null;
	}


	@Override
	public String getSourceAddress(byte[] rawPacket) {
		return IPpacket.getSrcIPasString(rawPacket);
	}


	@Override
	public String getDestinationAddress(byte[] rawPacket) {
		return IPpacket.getDstIPasString(rawPacket);
	}


	@Override
	public LengthAccuracy getLengthAccuracy(byte[] rawPacket) {
		return LengthAccuracy.EXACT;
	}

	
	@Override
	public int getLength(byte[] rawPacket) {
		return IPpacket.getTotalLength(rawPacket);
	}
	
	
	@Override
	public LengthAccuracy getHeaderLengthAccuracy(byte[] rawPacket) {
		return LengthAccuracy.EXACT;
	}


	@Override
	public int getHeaderLength(byte[] rawPacket) {
		return IPpacket.getHeaderLengthStat(rawPacket);
	}
	
	
	@Override
	public LengthAccuracy getPayloadLengthAccuracy(byte[] rawPacket) {
		return LengthAccuracy.EXACT;
	}
	
	
	@Override
	public int getPayloadLength(byte[] rawPacket) {
		if (IPpacket.isIPv4(rawPacket))
			return IPv4Packet.getPayloadLengthStat(rawPacket);
		else
			return IPv6Packet.getPayloadLengthWithoutExtensionHeaders(rawPacket);
	}


	@Override
	public byte[] getPayload(byte[] rawPacket) {
		return IPpacket.getPayloadStat(rawPacket);
	}


	@Override
	public Protocol getPayloadProtocol(byte[] rawPacket) {
		IPprotocol protocol = IPpacket.getProtocol(rawPacket);
		return ipProtocolToProtocol(protocol);
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
	
	
	public static Protocol ipProtocolToProtocol(IPprotocol ipProtocol) {
		if (ipProtocol == IPprotocol.TCP)
			return Protocol.TCP;
		else if (ipProtocol == IPprotocol.UDP)
			return Protocol.UDP;
		else if (ipProtocol == IPprotocol.ICMP)
			return Protocol.ICMP;
		else if (ipProtocol == IPprotocol.RDP)
			return Protocol.RDP;
		else if (ipProtocol == IPprotocol.ESP)
			return Protocol.ESP;
		else if (ipProtocol == IPprotocol.DCCP)
			return Protocol.DCCP;
		else if (ipProtocol == IPprotocol.XTP)
			return Protocol.XTP;
		else if (ipProtocol == IPprotocol.DDP)
			return Protocol.DDP;
		else if (ipProtocol == IPprotocol.RSVP)
			return Protocol.RSVP;
		else if (ipProtocol == IPprotocol.IPv6_ICMP)
			return Protocol.IPv6_ICMP;
		else if (ipProtocol == IPprotocol.TTP)
			return Protocol.TTP;
		else if (ipProtocol == IPprotocol.MTP)
			return Protocol.MTP;
		else if (ipProtocol == IPprotocol.IPIP)
			return Protocol.IPIP;
		else if (ipProtocol == IPprotocol.GMTP)
			return Protocol.GMTP;
		else if (ipProtocol == IPprotocol.SCTP)
			return Protocol.SCTP;
		else if (ipProtocol == IPprotocol.UDPlite)
			return Protocol.UDPlite;
		else
			return Protocol.UNKNOWN;
	}
}

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
import evaluation.traceParser.engine.protocolHeaderParser.PCAPpacket;


public class PCAPhandler implements ProtocolHandler {

	
	@Override
	public Calendar getTimestamp(byte[] rawPacket) {
		return PCAPpacket.getTimestampAsCalendar(rawPacket);
	}
	

	@Override
	public String getSourceAddress(byte[] rawPacket) {
		return null;
	}
	

	@Override
	public String getDestinationAddress(byte[] rawPacket) {
		return null;
	}
	

	@Override
	public LengthAccuracy getLengthAccuracy(byte[] rawPacket) {
		return LengthAccuracy.EXACT;
	}
	

	@Override
	public int getLength(byte[] rawPacket) {
		return rawPacket.length;
	}
	

	@Override
	public LengthAccuracy getHeaderLengthAccuracy(byte[] rawPacket) {
		return LengthAccuracy.EXACT;
	}
	

	@Override
	public int getHeaderLength(byte[] rawPacket) {
		return 16;
	}
	

	@Override
	public LengthAccuracy getPayloadLengthAccuracy(byte[] rawPacket) {
		return LengthAccuracy.EXACT;
	}
	

	@Override
	public int getPayloadLength(byte[] rawPacket) {
		return PCAPpacket.getPayloadLengthStat(rawPacket);
	}
	

	@Override
	public byte[] getPayload(byte[] rawPacket) {
		return PCAPpacket.getPayloadStat(rawPacket);
	}
	

	@Override
	public Protocol getPayloadProtocol(byte[] rawPacket) {
		return PCAPpacket.getPayloadProtocol(rawPacket);
	}

	
	@Override
	public byte[] readPacket(InputStream is) throws IOException {
		return PCAPpacket.readPCAPpacket(is);
	}

	
	@Override
	public boolean canHandle(FileInputStream fis) {
		// TODO implement check
		return false;
	}

}

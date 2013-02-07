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


public interface ProtocolHandler {

	
	public final static int UNKNOWN = -1;
	public enum LengthAccuracy {EXACT, MAY_UNDERESTIMATE, MAY_OVERESTIMATE, UNCLEAR};
	
	/**
	 * optional to implement (may return null) as many protocols do not contain 
	 * timestamps. must be implemented if the protocol provides a timestamp.
	 * @return
	 */
	public Calendar getTimestamp(byte[] rawPacket);
	
	public String getSourceAddress(byte[] rawPacket);
	
	public String getDestinationAddress(byte[] rawPacket);
	
	
	/**
	 * some protocols do not contain a header that specifies the actual length 
	 * of the packet. if those packets are truncated during capture, their 
	 * actual size cannot be determined on this layer and we should try to get 
	 * or calculate the size from (an)other layer(s).
	 * EXACT means that this handler's getLength() method can return exact 
	 * values even if packets are truncated.
	 * MAY_UNDERESTIMATE means that the returned value may be lower but not 
	 * larger than the actual value.
	 * MAY_OVERESTIMATE means that the returned value may be larger but not 
	 * shorter than the actual value.
	 * UNCLEAR means that the actual value may be larger or shorter.
	 * @param rawPacket
	 * @return
	 */
	public LengthAccuracy getLengthAccuracy(byte[] rawPacket);
	
	
	/**
	 * returns the total length of the packet (header + payload + padding (if 
	 * included)).
	 * if the packet is truncated, this method should return the theoretic 
	 * length, not the actual length of the data contained
	 * @param rawPacket
	 * @return
	 */
	public int getLength(byte[] rawPacket);
	
	
	/**
	 * some protocols do not contain a header that specifies the actual length 
	 * of the header. if those packets are truncated during capture, their 
	 * actual size cannot be determined on this layer and we should try to get 
	 * or calculate the size from (an)other layer(s).
	 * EXACT means that this handler's getLength() method can return exact 
	 * values even if packets are truncated.
	 * MAY_UNDERESTIMATE means that the returned value may be lower but not 
	 * larger than the actual value.
	 * MAY_OVERESTIMATE means that the returned value may be larger but not 
	 * shorter than the actual value.
	 * UNCLEAR means that the actual value may be larger or shorter.
	 * @param rawPacket
	 * @return
	 */
	public LengthAccuracy getHeaderLengthAccuracy(byte[] rawPacket);
	
	
	/**
	 * returns the total length of the header of this packet.
	 * if the packet is truncated, this method should return the theoretic 
	 * length, not the actual length of the data contained
	 * @param rawPacket
	 * @return
	 */
	public int getHeaderLength(byte[] rawPacket);
	
	
	/**
	 * some protocols do not contain a header that specifies the actual length 
	 * of the payload. if those packets are truncated during capture, the 
	 * actual payload size cannot be determined on this layer and we should try 
	 * to get or calculate the size from (an)other layer(s).
	 * EXACT means that this handler's getPayloadLength() method can return 
	 * exact values even if packets are truncated.
	 * MAY_UNDERESTIMATE means that the returned value may be lower but not 
	 * larger than the actual value.
	 * MAY_OVERESTIMATE means that the returned value may be larger but not 
	 * shorter than the actual value.
	 * UNCLEAR means that the actual value may be larger or shorter.
	 * @param rawPacket
	 * @return
	 */
	public LengthAccuracy getPayloadLengthAccuracy(byte[] rawPacket);
	
	
	/**
	 * returns the length of the payload of this packet (+ padding (if 
	 * included)).
	 * if the packet is truncated, this method should return the theoretic 
	 * length, not the actual length of the payload contained
	 * @param rawPacket
	 * @return
	 */
	public int getPayloadLength(byte[] rawPacket);
	
	/**
	 * may return null if no payload included
	 * @return
	 */
	public byte[] getPayload(byte[] rawPacket);
	
	
	public Protocol getPayloadProtocol(byte[] rawPacket);
	
	/**
	 * Tries to read a packet from the referenced file. Returns null if EOF 
	 * reached or no (more) packet could be extracted.
	 * @param is
	 * @return
	 */
	public byte[] readPacket(InputStream is) throws IOException;
	
	
	/**
	 * Tries to detect if the referenced file can be parsed by this handler. 
	 * Will NOT reset the FileInputStream.
	 * 
	 * @param fis
	 * @return
	 */
	public boolean canHandle(FileInputStream fis);
		
}

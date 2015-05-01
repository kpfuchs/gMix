/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
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
 *******************************************************************************/
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.dnsProxy_v0_001;

import java.util.Arrays;
import java.util.List;

import staticContent.framework.util.Util;


public class Payload {
	
	/** Byte representation of this <code>Payload</code>. */
	private byte[] bytePayload;
	
	private int maxSize;
	
	
	/**
	 * Constructs a new <code>Payload</code> with the submitted content (<code>
	 * bytePayload</code>).
	 * 
	 * @param bytePayload	Byte representation of this Payload.
	 */
	public Payload(byte[] bytePayload, int maxSize) {
		this.bytePayload = bytePayload;
		this.maxSize = maxSize;
		if (bytePayload.length > maxSize)
			throw new RuntimeException("bad arguments"); 
		
	}
	
	public Payload(int maxSize){
		this.maxSize = maxSize;
	}
	
	
	/**
	 * Returns the byte representation of this <code>Payload</code>.
	 * 
	 * @return Byte representation of this <code>Payload</code>.
	 */
	public byte[] getBytePayload() {
		
		return bytePayload;
		
	}
	
	public byte[] getBytePayloadWithLengthHeader() {
		
		return Util.concatArrays(Util.intToByteArray(bytePayload.length), bytePayload);
		
	}
	

	/**
	 * Returns the length of the message embedded in this <code>Payload</code> 
	 * (without padding).
	 * 
	 * @return Length of the embedded message (without padding).
	 */
	private int getMessageLength() {
		
		return (null == bytePayload) ? 0 : DNSUtils.byteArrayToInt(Arrays.copyOfRange(bytePayload, 0, 4));
		
	}
	
	
	/**
	 * Returns the message embedded in this <code>Payload</code> (without 
	 * padding).
	 * 
	 * @return 	The message embedded in this <code>Payload</code> (without 
	 * 			padding).
	 */
	public byte[] getMessage() {
		//long start = System.nanoTime();
		
		byte[] result = new byte[getMessageLength()];
		
		if (result.length == 0)
			return result;
		
		System.arraycopy(bytePayload, 4, result, 0, result.length);
		
		
		/*
		for (int i=4; i<(result.length + 4); i++) {

			result[i-4] = bytePayload[i];
				
		}*/
		
		//LOGGER.info("getMessage: "+ (System.nanoTime()-start));
		
		return result;
		
	}
	
	
	/**
	 * Sets the <code>bytePayload</code> of this <code>Payload</code> to the 
	 * bypassed value. 
	 */
	public void setMessage(byte[] newMessage) {
		if (newMessage.length > this.maxSize)
			throw new RuntimeException("too large"); 
		this.bytePayload = newMessage;
	}
	
	
	/**
	 * Returns the length of the header used by this class for any 
	 * <code>Payload</code> (Used to calculate message sizes).
	 * 
	 * @return	The length of the header used by this class.
	 */
	public static int getHeaderLength() {
		return 4;
	}
	
	
	public List<byte[]> getMessages(int start){
		//return Util.splitArrayOnPattern(bytePayload, dnsTerminator, start);
		return DNSUtils.splitArrayOnPattern(Arrays.copyOfRange(bytePayload, 4, bytePayload.length), DNSUtils.dnsTerminator, start);
	}
	

	public void addMessage(byte[] rawDNS) {
		int currentLength = getMessageLength();
		byte[] newLengthAsArray = DNSUtils.intToByteArray(currentLength+rawDNS.length+5); //magic pattern length
		byte[] currentPayload = getMessage();
		rawDNS = DNSUtils.mergeArrays(rawDNS, DNSUtils.dnsTerminator);
		byte[] payload = DNSUtils.mergeArrays(currentPayload, rawDNS);
		//payload = Padder.addPadding(payload, maxPayloadLength);
		this.bytePayload = DNSUtils.mergeArrays(newLengthAsArray, payload);		
	}
	
	
	public int remaining() {
		return maxSize - (bytePayload.length+5); //magic pattern length
	}
	
	
	public void setMessageTest(byte[] msg){
		this.bytePayload = msg;
	}

}
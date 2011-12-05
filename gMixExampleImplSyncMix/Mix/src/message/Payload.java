/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2011  Karl-Peter Fuchs
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

package message;


import java.util.Arrays;

import util.Util;


/**
 * Data structure for the "payload" of a message. A <code>Payload</code> 
 * consists of two parts: Information about its length (4 byte length header) 
 * and the data it contains itself (e. g. data the user wants to send via the 
 * cascade or an encrypted message for the cascade's next mix).
 * <p>
 * Contains a padding mechanism to pad bypassed data to a desired length.
 * 
 * @author Karl-Peter Fuchs
 */
public class Payload {
	
	/** Byte representation of this <code>Payload</code>. */
	private byte[] bytePayload = null;
	
	
	/**
	 * Constructs a new <code>Payload</code> with the submitted content (<code>
	 * bytePayload</code>).
	 * 
	 * @param bytePayload	Byte representation of this Payload.
	 */
	public Payload(byte[] bytePayload) {
		
		this.bytePayload = bytePayload;
		
	}
	
	
	/**
	 * Returns the byte representation of this <code>Payload</code>.
	 * 
	 * @return Byte representation of this <code>Payload</code>.
	 */
	public byte[] getBytePayload() {
		
		return bytePayload;
		
	}
	

	/**
	 * Returns the length of the message embedded in this <code>Payload</code> 
	 * (without padding).
	 * 
	 * @return Length of the embedded message (without padding).
	 */
	private int getMessageLength() {
		
		return Util.byteArrayToInt(Arrays.copyOfRange(bytePayload, 0, 4));
		
	}
	
	
	/**
	 * Returns the message embedded in this <code>Payload</code> (without 
	 * padding).
	 * 
	 * @return 	The message embedded in this <code>Payload</code> (without 
	 * 			padding).
	 */
	public byte[] getMessage() {
		
		byte[] result = new byte[getMessageLength()];
		
		for (int i=4; i<(result.length + 4); i++) {
				
			result[i-4] = bytePayload[i];
				
		}
		
		return result;
		
	}
	
	
	/**
	 * Sets the <code>bytePayload</code> of this <code>Payload</code> to the 
	 * bypassed value. Adds padding if necessary.
	 * 
	 * @param newMessage 	The message to be embedded in this 
	 * 						<code>Payload</code>.
	 * @param desiredLength The desired length of this <code>Payload</code>.
	 */
	public void setMessage(byte[] newMessage, int desiredLength) {

		byte[] lengthAsArray = Util.intToByteArray(newMessage.length);
		newMessage = Padder.addPadding(newMessage, desiredLength);
		this.bytePayload = Util.mergeArrays(lengthAsArray, newMessage);
		
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

}

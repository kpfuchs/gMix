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


import java.security.SecureRandom;
import java.util.Arrays;


/**
 * Adds or removes padding to / from a message (Padding is used to guarantee a 
 * constant message length and therefore prevent linkability). Uses a 
 * pseudo-random number Generator (<code>SecureRandom</code>) for generating 
 * padding.
 * <p>
 * This class is thread-safe.
 */
final class Padder {
	
	/** Random number generator for padding. */
	private static SecureRandom secureRandom = new SecureRandom();
	
	
	/**
	 * Empty constructor. Never used since all methods are static.
	 */
	private Padder() {
		
	}
	
	
	/**
	 * Increases the size of the bypassed array to <code>desiredLength</code>, 
	 * filling it's unused space with padding. If <code>desiredLength</code> is 
	 * larger than the bypassed array, no actions are performed (since padding 
	 * is not necessary).
	 * 
	 * @param data			Array to be padded to <code>desiredLength</code>.
	 * @param desiredLength	Desired length of the bypassed array after padding 
	 * 						in byte.
	 */
	protected static byte[] addPadding(byte[] data, int desiredLength ) {

		if (desiredLength > data.length) { // padding is necessary

			int lengthOfUnpaddedData = data.length;
			data = Arrays.copyOf(data, desiredLength);
			byte[] padding = new byte[desiredLength - lengthOfUnpaddedData];
			secureRandom.nextBytes(padding);
			
			System.arraycopy(	padding,
								0, 
								data, 
								lengthOfUnpaddedData, 
								padding.length
								);
			
		}
		
		return data;
	}
	
	
	/**
	 * Removes padding from the bypassed array. If <code>lengthOfUnpaddedData
	 * </code> is smaller than the bypassed array, no actions are performed 
	 * (since the bypassed array contains no padding).
	 * 
	 * @param data					Array with padding.
	 * @param lengthOfUnpaddedData	Desired length of the bypassed array after 
	 * 								removal of padding.
	 */
	protected static byte[] removePadding(	byte[] data, 
											int lengthOfUnpaddedData
											) {

		if (lengthOfUnpaddedData < data.length) {

			data = Arrays.copyOfRange(data, 0, lengthOfUnpaddedData);

		}
		
		return data;

	}
	
}

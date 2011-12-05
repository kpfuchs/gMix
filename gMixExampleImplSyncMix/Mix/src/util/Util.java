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

package util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * Offers static methods for converting data types (which are of general use, 
 * but not offered by the java API).
 * 
 * @author Karl-Peter Fuchs
 */
public final class Util {
	
	
	/**
	 * Empty constructor. Never used since all methods are static.
	 */
	private Util() {
		
	}
	
	
	/**
	 * Converts the bypassed long value to a byte array.
	 * 
	 * @param source	The long value to be translated.
	 * @return 			Byte array representation of the bypassed long value.
	 */
	public static byte[] longToByteArray(long source) {
		
		byte[] result = new byte[8];
		
		for (int i=0; i<8; i++) {
			
			result[i] = new Long((source >> (i << 3)) & 255L).byteValue();
			
		}
		
		return result;
	}
	
	
	/**
	 * Converts the bypassed byte array to a long value.
	 * 
	 * @param byteArray	The byte array to be translated.
	 * @return 			"long" representation of the bypassed byte array.
	 */
	public static long byteArrayToLong(byte[] byteArray) {

		long result = 0;
		
		for (int i=0; (i<byteArray.length) && (i<8); i++) {
			
			result |= ((((long) byteArray[i]) & 255L) << (i << 3));
			
		}
		
		return result;
	}
	
	
	/**
	 * Converts the bypassed int value to a byte array.
	 * 
	 * @param source	The int value to be translated.
	 * @return 			Byte array representation of the bypassed int value.
	 */
	public static byte[] intToByteArray(int source) {
		
		byte[] result = new byte[4];
		
		for (int i = 0; i < 4; ++i) {
			
			result[3-i] = (byte)((source & (0xff << (i << 3))) >>> (i << 3));
			
		}
		
		return result;
	}
	
	
	/**
	 * Converts the bypassed byte array to an int value.
	 * 
	 * @param byteArray	The byte array to be translated.
	 * @return 			"int" representation of the bypassed byte array.
	 */
	public static int byteArrayToInt(byte[] byteArray) {
		
		int result = 0;
	
		for (int i = 0; (i<byteArray.length) && (i<8); i++) {
			
			result |= (byteArray[3-i] & 0xff) << (i << 3);
			
		}
		
		return result;
					
	}	
	
	
	/**
	 * Converts the bypassed short value to a byte array.
	 * 
	 * @param source	The short value to be translated.
	 * @return 			Byte array representation of the bypassed short value.
	 */
	public static byte[] shortToByteArray(int source) {
		
		byte[] result = new byte[2];
		
		result[0] = (byte)((source & 0xFF00) >> 8);
		result[1] = (byte)(source & 0x00FF);
		
		return result;

	}
	
	
	/**
	 * Converts the bypassed byte array to a short value.
	 * 
	 * @param byteArray	The byte array to be translated.
	 * @return 			"short" representation of the bypassed byte array.
	 */
	public static int byteArrayToShort(byte[] byteArray) {
		
		int result = 0;
		
		result |= (byteArray[0] & 0xFF);
		result <<= 8;
		result |= (byteArray[1] & 0xFF);
		
		return result;
		
	}
	
	
	/**
	 * Merges the bypassed byte arrays (<code>secondArray</code> is appended to 
	 * <code>firstArray</code>).
	 * 
	 * @param firstArray 	Array to be extended.
	 * @param secondArray 	Array to be appended.
	 * 
	 * @return 				Merged array. 
	 */
	public static byte[] mergeArrays(byte[] firstArray, byte[] secondArray) {
		
		byte[] result = new byte[firstArray.length + secondArray.length];
		
		System.arraycopy(firstArray, 0, result, 0, firstArray.length);
		
		System.arraycopy(	secondArray,
						 	0, result, 
						 	firstArray.length, 
						 	secondArray.length
						 	);
		
		return result;
		
	}
	
	
	/**
	 * Removes the specified part of the array.
	 * 
	 * @param data array to remove data from
	 * @param offset index of first byte (inclusive) to be removed (fist index is 0).
	 * @param length number of bytes to be removed.
	 */
	public static byte[] removePartOfArray(byte[] data, int offset, int length) {
		
		byte[] result = new byte[data.length - length];
		
		if (offset != 0) // add first part
			System.arraycopy(data, 0, result, 0, offset);
		

		// add second part
		System.arraycopy(data, offset+length, result, offset, data.length - (offset+length));
		
		return result;

	}
	
	
	
	/**
	 * Reads <code>result.length</code> bytes from the bypassed 
	 * <code>inputStream</code>. 
	 * Does not return before <code>result.length</code> bytes were read.
	 * 
	 * @param inputStream 	<code>InputStream</code> to read from.
	 * @param result 		array to read data to
	 * 
	 * @return 				array with the read data
	 */
	public static byte[] forceRead(	InputStream inputStream, 
									byte[] result
									) throws IOException {
		
		int bytesRead = 0;
		int total = result.length;
		int remaining = total;
		
		while (remaining > 0)
			remaining -= inputStream.read(result, bytesRead, remaining);
		
		return result;
		
	}
	
	
	public static byte[] generateMD5Hash(byte[] data) {
		
		try {
			
			MessageDigest md = MessageDigest.getInstance("MD5");
			return md.digest(data);
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return null;
		
	}
	
}
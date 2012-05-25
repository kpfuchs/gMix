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

package framework;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


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
	public static short byteArrayToShort(byte[] byteArray) {
		
		short result = 0;
		
		result |= (byteArray[0] & 0xFF);
		result <<= 8;
		result |= (byteArray[1] & 0xFF);
		
		return result;
		
	}
	
	
	public static byte[] concatArrays(byte[][] arrays) {
		
		byte[] result = concatArrays(arrays[0], arrays[1]);
		
		for (int i=2; i<arrays.length; i++)
			result = concatArrays(result, arrays[i]);

		return result;
		
	}
	
	
	public static byte[] concatArrays(byte[] firstArray, byte[] secondArray) {
		
		byte[] result = new byte[firstArray.length + secondArray.length];
		
		System.arraycopy(firstArray, 0, result, 0, firstArray.length);
		
		System.arraycopy(	secondArray,
						 	0, result, 
						 	firstArray.length, 
						 	secondArray.length
						 	);
		
		return result;
		
	}
	
	
	public static byte[] removePartOfArray(byte[] data, int offset, int length) {
		
		byte[] result = new byte[data.length - length];
		
		if (offset != 0) // add first part
			System.arraycopy(data, 0, result, 0, offset);
		

		// add second part
		System.arraycopy(data, offset+length, result, offset, data.length - (offset+length));
		
		return result;

	}
	
	
	public static byte[] xor(byte[] x, byte[] y) {
		 
		 assert (x.length == y.length);
		 
		 byte[] xored = new byte[x.length];
		 
		 for(int i=0; i<xored.length;i++) {
			 xored[i]= (byte) (x[i] ^ y[i]);
		 }
		 
		 return xored;
	 }
	
	
	public static void writeInt(OutputStream outputStream, int integerToWrite) throws IOException {
		outputStream.write(intToByteArray(integerToWrite));
	}
	
	
	public static void writeLong(OutputStream outputStream, long longToWrite) throws IOException {
		outputStream.write(longToByteArray(longToWrite));
	}
	
	
	public static void writeShort(OutputStream outputStream, short shortToWrite) throws IOException {
		outputStream.write(shortToByteArray(shortToWrite));
	}
	
	
	public static byte[] forceRead(InputStream inputStream, byte[] result) throws IOException {
		int bytesRead = 0;
		int total = result.length;
		int remaining = total;
		while (remaining > 0)
			remaining -= inputStream.read(result, bytesRead, remaining);
		return result;
	}
	
	
	public static byte[] forceRead(InputStream inputStream, int length) throws IOException {
		int bytesRead = 0;
		byte[] result = new byte[length];
		int total = result.length;
		int remaining = total;
		while (remaining > 0)
			remaining -= inputStream.read(result, bytesRead, remaining);
		return result;
	}
	
	
	public static int forceReadInt(InputStream inputStream) throws IOException {
		int bytesRead = 0;
		byte[] result = new byte[4];
		int total = result.length;
		int remaining = total;
		while (remaining > 0)
			remaining -= inputStream.read(result, bytesRead, remaining);
		return byteArrayToInt(result);
	}
	
	
	public static long forceReadLong(InputStream inputStream) throws IOException {
		int bytesRead = 0;
		byte[] result = new byte[8];
		int total = result.length;
		int remaining = total;
		while (remaining > 0)
			remaining -= inputStream.read(result, bytesRead, remaining);
		return byteArrayToLong(result);
	}
	
	
	public static short forceReadShort(InputStream inputStream) throws IOException {
		int bytesRead = 0;
		byte[] result = new byte[2];
		int total = result.length;
		int remaining = total;
		while (remaining > 0)
			remaining -= inputStream.read(result, bytesRead, remaining);
		return byteArrayToShort(result);
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
	
	
	public static String display(byte[] data) {
		return "[" +getStringWithoutNewLines(data) +"]";
	}
	
	
	public static String md5(byte[] data) {
		return "[" +asAscii(new String(generateMD5Hash(data))) +"]";
	}
	
	
	public static String getStringWithoutNewLines(byte[] data) {
		return ((new String(data)).replaceAll("\n", " ")).replaceAll("\r", " ");
	}
	
	
	public static String asAscii(String s) {
		String result = "";
		for (int i=0;i<s.length();i++) {
			result += (int)s.charAt(i);// +" ";
		}
		return result;
	}
	
	
	public static void checkIfBCIsInstalled() {
		
		// check if bouncy castle is installed
		try {
			
			Security.addProvider(new BouncyCastleProvider());
			KeyGenerator keyGen = KeyGenerator.getInstance("AES", "BC");
		    keyGen.init(new SecureRandom());
		    Key key = keyGen.generateKey();
		    Cipher encrypt = Cipher.getInstance("AES/OFB/NOPADDING", "BC");
		    encrypt.init(Cipher.ENCRYPT_MODE, key);
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
			CipherOutputStream cos = new CipherOutputStream(baos, encrypt);
			cos.write("plaintext".getBytes());
			cos.close();
			
		} catch (Exception e) {
			
			System.err.println(
					"The Bouncy Castle crypto provider (http://www.bou"+
					"ncycastle.org/) seems to be not installed or " +
					"working! \n" +
					"Please add the Bouncy Castle jar-file to your " +
					"classpath.\n"
					+e.getMessage()
					);
			
		    e.printStackTrace();
		    System.exit(1);
		    
		}
		
		// check if the Java Cryptography Extension (JCE) Unlimited Strength 
		// Jurisdiction Policy Files are installed
		try {
	    	KeyPairGenerator kpg =  KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
		    kpg.generateKeyPair();
	    } catch (NoSuchAlgorithmException e) {
	    	
	    	System.err.println(	
	    			"The Java Cryptography Extension (JCE) Unlimited" +
	    			" Strength Jurisdiction Policy Files (http://www." +
	    			"oracle.com/technetwork/java/javase/downloads/" +
	    			"index.html) seem to be not installed or " +
					"working! \n"
	    			+e.getMessage()
					);
	
	    	e.printStackTrace();
	    	System.exit(1);
	    	
	    }
	}


	public static boolean assertionsEnabled() {
		
		boolean enabled = false;
		assert enabled = true;
		
		if (!enabled)
			return false;
		else
			return true;
	}
	
}
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
package framework.core.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


public final class Util {
	
	
	public static final int NOT_SET =-222222222;
	private static SecureRandom random = new SecureRandom();
	
	
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
	
	
	
	public static byte[] floatToByteArray(float source) {
		return intToByteArray(Float.floatToRawIntBits(source));
	}
	
	
	public static float byteArrayToFloat(byte[] byteArray) {
		return Float.intBitsToFloat(byteArrayToInt(byteArray));		
	}
	
	
	public static byte[] doubleToByteArray(double source) {
		return longToByteArray(Double.doubleToRawLongBits(source));
	}
	
	
	public static double byteArrayToDouble(byte[] byteArray) {
		return Double.longBitsToDouble(byteArrayToLong(byteArray));	
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
		int read;
		while (remaining > 0) {
			read = inputStream.read(result, bytesRead, remaining);
			remaining -= read;
			bytesRead += read;
		}
		return result;
	}
	
	
	public static byte[] forceRead(InputStream inputStream, int length) throws IOException {
		int bytesRead = 0;
		byte[] result = new byte[length];
		int total = result.length;
		int remaining = total;
		int read;
		while (remaining > 0) {
			read = inputStream.read(result, bytesRead, remaining);
			remaining -= read;
			bytesRead += read;
		}
		return result;
	}
	
	
	public static int forceReadInt(InputStream inputStream) throws IOException {
		int bytesRead = 0;
		byte[] result = new byte[4];
		int total = result.length;
		int remaining = total;
		int read;
		while (remaining > 0) {
			read = inputStream.read(result, bytesRead, remaining);
			remaining -= read;
			bytesRead += read;
		}
		return byteArrayToInt(result);
	}
	
	
	public static long forceReadLong(InputStream inputStream) throws IOException {
		int bytesRead = 0;
		byte[] result = new byte[8];
		int total = result.length;
		int remaining = total;
		int read;
		while (remaining > 0) {
			read = inputStream.read(result, bytesRead, remaining);
			remaining -= read;
			bytesRead += read;
		}
		return byteArrayToLong(result);
	}
	
	
	public static short forceReadShort(InputStream inputStream) throws IOException {
		int bytesRead = 0;
		byte[] result = new byte[2];
		int total = result.length;
		int remaining = total;
		int read;
		while (remaining > 0) {
			read = inputStream.read(result, bytesRead, remaining);
			remaining -= read;
			bytesRead += read;
		}
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
			
		} catch (InvalidKeyException e) {
			System.err.println(
					"The \"Java Cryptography Extension (JCE) Unlimited " +
					"Strength Jurisdiction Policy Files\" seem to be not " +
					"installed but are required. Download available at " +
					"http://www.oracle.com/technetwork/java/javase/" +
					"downloads/index.html\n\n"
					+e.getMessage()
					);
			
		    e.printStackTrace();
		    System.exit(1);
			
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
	
	
	public static String getFileContent(String fileNameOrPath) {
		String result = "";
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
				new DataInputStream(new FileInputStream(fileNameOrPath))));
			String line;
			while ((line = br.readLine()) != null)
				result += line + "\n";
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(	"ERROR: could not read from file " 
										+fileNameOrPath +"!");
		}
		return result;
	}
	
	
	public static void writeToFile(String content, String fileNameOrPath) {
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				new DataOutputStream(new FileOutputStream(fileNameOrPath))));
			bw.write(content);
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(	"ERROR: could not write to file " 
										+fileNameOrPath +"!");
		}
	}
	
	
	public static String[] getTextBetweenAllAAndB(String source, String a, String b) {
		ArrayList<String> result = new ArrayList<String>();
		String[] lines = source.split("\n");
		for (String line:lines) {
			int positionA = source.indexOf(a);
			int positionB = source.indexOf(b, positionA);
			if (positionA != -1 && positionB != -1) {
				result.add(line.substring(positionA + a.length(), positionB));
			}
		}
		return result.toArray(new String[0]);
	}
	
	
	public static String getTextBetweenAAndB(String source, String a, String b) {
		int positionA = source.indexOf(a);
		int positionB = source.indexOf(b, positionA);
		return source.substring(positionA + a.length(), positionB);
	}
	
	
	@SuppressWarnings("unchecked") // type safety is assured through generic method header; the need for "@SuppressWarnings" is a flaw/feature of java generics...
	public static <T> T[][] splitInChunks(int chunkSize, T[] source) {
		if (source.length <= chunkSize)
			throw new RuntimeException("cannot split the bypassed array (array is smaller than chunkSize)"); 
		int chunks = (int) Math.ceil((double)source.length / (double)chunkSize);
		Object[][] result = new Object[chunks][];
		int pointer = 0;
		for (int i=0; i<result.length; i++)
			if (i < result.length-1)
				result[i] = Arrays.copyOfRange(source, pointer, pointer+=chunkSize);
			else
				result[i] = Arrays.copyOfRange(source, pointer, source.length);
		return (T[][]) result;
	}

	
	public static byte[][] splitInChunks(int chunkSize, byte[] source) {
		if (source.length <= chunkSize)
			throw new RuntimeException("cannot split the bypassed array (array is smaller than chunkSize)"); 
		int chunks = (int) Math.ceil((double)source.length / (double)chunkSize);
		byte[][] result = new byte[chunks][];
		int pointer = 0;
		for (int i=0; i<result.length; i++)
			if (i < result.length-1)
				result[i] = Arrays.copyOfRange(source, pointer, pointer+=chunkSize);
			else
				result[i] = Arrays.copyOfRange(source, pointer, source.length);
		return result;
	}
	
	
	
	@SuppressWarnings("unchecked") // type safety is assured through generic method header; the need for "@SuppressWarnings" is a flaw/feature of java generics...
	public static <T> T[][] splitAfter(int splitBefore, T[] source) {
		if (source.length <= splitBefore)
			throw new RuntimeException("cannot split the bypassed array (array too small)"); 
		Object[][] result = new Object[2][];
		result[0] = Arrays.copyOfRange(source, 0, splitBefore);
		result[1] = Arrays.copyOfRange(source, splitBefore, source.length);
		return (T[][]) result;
	}
	
	
	public static byte[][] split(int splitBefore, byte[] source) {
		if (source.length <= splitBefore)
			throw new RuntimeException("cannot split the bypassed array (array too small: "+source.length +"<=" +splitBefore +")"); 
		byte[][] result = new byte[2][];
		result[0] = Arrays.copyOfRange(source, 0, splitBefore);
		result[1] = Arrays.copyOfRange(source, splitBefore, source.length);
		return result;
	}
	
	
	private static final long SLEEP_PRECISION = TimeUnit.MILLISECONDS.toNanos(2); // TODO: determine for current machine
	
	public static void sleepNanos(long nanoDuration) throws InterruptedException {
		// see http://andy-malakov.blogspot.de/2010/06/alternative-to-threadsleep.html
		final long end = System.nanoTime() + nanoDuration;
		long timeLeft = nanoDuration;
		do {
			if (timeLeft > SLEEP_PRECISION)
				Thread.sleep(1);
			else
				Thread.yield();
			timeLeft = end - System.nanoTime();
			if (Thread.interrupted())
				throw new InterruptedException();
		} while (timeLeft > 0);
	}
	
	
	public static String humanReadableByteCount(long bytes, boolean si) {
		// see http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		for (File f:getFilePaths(".", "StaticFinctionSettings.txt"))
			System.out.println(f); 
		
	} 
	
	
	// example:
	// for (File f:getFilePaths(".", "StaticFinctionSettings.txt"))
	//      System.out.println(f); 
	public static File[] getFilePaths(String rootFolder, String filename) {
		Vector<File> found = new Vector<File>();
		File root = new File(rootFolder);
		getFilePaths(root, filename, found);
		return found.toArray(new File[0]);
	}
	
	
	private static void getFilePaths(File rootFolder, String filename, Vector<File> found) {
		if (rootFolder.isDirectory()) {
			File[] files = rootFolder.listFiles();
			for (File f : files)
				getFilePaths(f, filename, found);
		} else if (rootFolder.isFile() && rootFolder.getName().equalsIgnoreCase(filename)) {
			found.add(rootFolder);
		}
	}
	
	
	public static boolean contains(int searchFor, int[] in) {
		for (int i=0; i<in.length; i++)
			if (in[i] == searchFor)
				return true;
		return false;
	}
	
	
	public static int getRandomInt(int min, int max) {
		return min + (int)(random.nextDouble()*(max-min)+1);
	}
	
	
	public static int getRandomInt(int min, int max, Random random) {
		return min + (int)(random.nextDouble()*(max-min)+1);
	}
	
}
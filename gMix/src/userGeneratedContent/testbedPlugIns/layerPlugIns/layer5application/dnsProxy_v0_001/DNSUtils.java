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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.PTRRecord;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Section;
import org.xbill.DNS.SetResponse;
import org.xbill.DNS.Type;


public class DNSUtils {

	private static long longToByteArrayCounter = 0l;
	private static long byteArrayToLongCounter = 0l;
	private static long intToByteArrayCounter = 0l;
	private static long byteArrayToIntCounter = 0l;
	private static long shortToByteArrayCounter = 0l;
	private static long byteArrayToShortCounter = 0l;
	private static long mergeArraysCounter = 0l;
	private static long byteListToPrimitiveArrayListCounter = 0l;
	private static long getHexCounter = 0l;
	private static long splitArrayOnPatternCounter = 0l;
	private static long indexOfCounter = 0l;
	private static long processSetResponseCounter = 0l;
	private static long resolveDNSToLocalhostCounter = 0l;
	private static long formerrMessageForDNSQueryCounter = 0l;
	
	private static final InetAddress LOCALHOST;
	private static final InetAddress LOCALHOST_V6;


	public static final byte[] dnsTerminator = new byte[]{(byte)0xfa,(byte)0xfb,(byte)0xfc,(byte)0xfd,(byte)0xfe};


	/**
	 * Empty constructor. Never used since all methods are static.
	 */
	private DNSUtils() {

	}
	
	
	static {
		InetAddress temp = null;
		InetAddress temp2 = null;
		try {
			temp =  InetAddress.getLocalHost();
			temp2 = InetAddress.getByName("::1");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		LOCALHOST = temp;
		LOCALHOST_V6 = temp2;
	}

	private static ByteBuffer tempBuffer = ByteBuffer.allocate(2048);

	private static final DateFormat dateFormat= new SimpleDateFormat("yyyy-dd-MM_HH-mm-ss");


	/**
	 * Converts the bypassed long value to a byte array.
	 * 
	 * @param source	The long value to be translated.
	 * @return 			Byte array representation of the bypassed long value.
	 */
	public static byte[] longToByteArray(long source) {
		long methodStart = System.nanoTime();
		byte[] result = new byte[8];

		for (int i=0; i<8; i++) {

			result[i] = new Long((source >> (i << 3)) & 255L).byteValue();

		}
		long methodEnd = System.nanoTime();

		longToByteArrayCounter+= methodEnd-methodStart;

		return result;
	}


	/**
	 * Converts the bypassed byte array to a long value.
	 * 
	 * @param byteArray	The byte array to be translated.
	 * @return 			"long" representation of the bypassed byte array.
	 */
	public static long byteArrayToLong(byte[] byteArray) {

		long methodStart = System.nanoTime();

		long result = 0;

		for (int i=0; (i<byteArray.length) && (i<8); i++) {

			result |= ((((long) byteArray[i]) & 255L) << (i << 3));

		}

		long methodEnd = System.nanoTime();

		byteArrayToLongCounter+= methodEnd-methodStart;


		return result;
	}


	/**
	 * Converts the bypassed int value to a byte array.
	 * 
	 * @param source	The int value to be translated.
	 * @return 			Byte array representation of the bypassed int value.
	 */
	public static byte[] intToByteArray(int source) {
		
		long methodStart = System.nanoTime();
		
		byte[] result = new byte[4];

		for (int i = 0; i < 4; ++i) {

			result[3-i] = (byte)((source & (0xff << (i << 3))) >>> (i << 3));

		}
		
		long methodEnd = System.nanoTime();

		intToByteArrayCounter+= methodEnd-methodStart;

		return result;
	}


	/**
	 * Converts the bypassed byte array to an int value.
	 * 
	 * @param byteArray	The byte array to be translated.
	 * @return 			"int" representation of the bypassed byte array.
	 */
	public static int byteArrayToInt(byte[] byteArray) {
		
		long methodStart = System.nanoTime();
		
		int result = 0;

		for (int i = 0; (i<byteArray.length) && (i<8); i++) {

			result |= (byteArray[3-i] & 0xff) << (i << 3);

		}
		
		long methodEnd = System.nanoTime();

		byteArrayToIntCounter+= methodEnd-methodStart;

		return result;

	}	


	/**
	 * Converts the bypassed short value to a byte array.
	 * 
	 * @param source	The short value to be translated.
	 * @return 			Byte array representation of the bypassed short value.
	 */
	public static byte[] shortToByteArray(int source) {
		
		long methodStart = System.nanoTime();

		byte[] result = new byte[2];

		result[0] = (byte)((source & 0xFF00) >> 8);
		result[1] = (byte)(source & 0x00FF);
		
		long methodEnd = System.nanoTime();

		shortToByteArrayCounter+= methodEnd-methodStart;

		return result;

	}


	/**
	 * Converts the bypassed byte array to a short value.
	 * 
	 * @param byteArray	The byte array to be translated.
	 * @return 			"short" representation of the bypassed byte array.
	 */
	public static int byteArrayToShort(byte[] byteArray) {
		
		long methodStart = System.nanoTime();

		int result = 0;

		result |= (byteArray[0] & 0xFF);
		result <<= 8;
		result |= (byteArray[1] & 0xFF);
		
		long methodEnd = System.nanoTime();

		byteArrayToShortCounter+= methodEnd-methodStart;

		return result;

	}


	/**
	 * Merges the bypassed byte arrays (<code>secondArray</code> is appended to 
	 * <code>firstArray</code>).
	 * 
	 * @param firstArray 	Array to be extended.
	 * @param bytes 	Array to be appended.
	 * 
	 * @return 				Merged array. 
	 */
	public static byte[] mergeArrays(byte[] firstArray, byte[] bytes) {
		
		long methodStart = System.nanoTime();

		byte[] result = new byte[firstArray.length + bytes.length];

		System.arraycopy(firstArray, 0, result, 0, firstArray.length);

		System.arraycopy(	bytes,
				0, result, 
				firstArray.length, 
				bytes.length
		);
		
		long methodEnd = System.nanoTime();

		mergeArraysCounter+= methodEnd-methodStart;

		return result;

	}

	public static byte[] byteListToPrimitiveArray(List<byte[]> list)  {
		
		long methodStart = System.nanoTime();
		
		Iterator<byte[]> it = list.iterator();
		byte[] ret;
		
		synchronized(tempBuffer){
			while (it.hasNext()){
				tempBuffer.put(it.next());
			}
			ret = new byte[tempBuffer.position()];
			tempBuffer.flip();
			tempBuffer.get(ret).clear();
		}
		
		
		long methodEnd = System.nanoTime();

		byteListToPrimitiveArrayListCounter+= methodEnd-methodStart;
		
		return ret;
	}


	/** http://rgagnon.com/javadetails/java-0596.html*/
	static final String HEXES = "0123456789ABCDEF";
	public static String getHex( byte [] raw ) {// TODO change code, very slow!

		if ( raw == null ) {
			return null;
		}
		final StringBuilder hex = new StringBuilder( 2 * raw.length );
		for ( final byte b : raw ) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4))
			.append(HEXES.charAt((b & 0x0F)));
		}
		
		return hex.toString();
	}


	/** http://helpdesk.objects.com.au/java/search-a-byte-array-for-a-byte-sequence bzw
	 * http://stackoverflow.com/questions/1507780/searching-for-a-sequence-of-bytes-in-a-binary-file-with-java
	 */
	/**
	 * Finds the first occurrence of the pattern in the text.
	 */
	/*public static int indexOf(byte[] data, byte[] pattern) {
		int[] failure = computeFailure(pattern);
		System.out.println("failure is " + Arrays.toString(failure));

		int j = failure[failure.length-1];
		if (data.length == 0) return -1;

		for (int i = 0; i < data.length; i++) {
			while (j > 0 && pattern[j] != data[i]) {
				System.out.println("fail");
				j = failure[j - 1];
			}
			if (pattern[j] == data[i]) { j++; }
			if (j == pattern.length) {
				System.out.println("found");
				return i - pattern.length + 1;
			}
		}
		return -1;
	}*/


	/*
	 * Computes the failure function using a boot-strapping process,
	 * where the pattern is matched against itself.

	private static int[] computeFailure(byte[] pattern) {
		int[] failure = new int[pattern.length];

		int j = 0;
		for (int i = 1; i < pattern.length; i++) {
			while (j > 0 && pattern[j] != pattern[i]) {
				j = failure[j - 1];
			}
			if (pattern[j] == pattern[i]) {
				j++;
			}
			failure[i] = j;
		}

		return failure;
	} */




	public static List<byte[]> splitArrayOnPattern(byte[] data, byte[] pattern, int start){//dnsclient needs start 4 (skip msg length), proxy connection handler needs 0
		
		long methodStart = System.nanoTime();
		
		List<byte[]> arrayList = new ArrayList<byte[]>();
		// check if inside
		int index = IndexOf(data, pattern);
		if (index == -1){
			byte[] msg = Arrays.copyOfRange(data, start, data.length);
			arrayList.add(msg);
			return arrayList;
		}
		while ( -1 != (index = IndexOf(data, pattern)) ){
			byte[] msg = Arrays.copyOfRange(data, start, index);

			if (msg.length > 0){
				arrayList.add(msg);
			}
			//System.out.println("msg added: " + getHex(msg));
			data = Arrays.copyOfRange(data, index+5, data.length);
			//System.out.println("data for next round: " + getHex(data));
			start = 0;
		}
		
		long methodEnd = System.nanoTime();

		splitArrayOnPatternCounter+= methodEnd-methodStart;
		
		return arrayList;
	}
	

	// TODO rework this, copy of http://social.msdn.microsoft.com/Forums/en-US/csharpgeneral/thread/15514c1a-b6a1-44f5-a06c-9b029c4164d7
	// Problem: if pattern matches no subsequent match is checked
	public static int IndexOf(byte[] arrayToSearchThrough, byte[] patternToFind)
	{
		
		long methodStart = System.nanoTime();
		
		if (patternToFind.length > arrayToSearchThrough.length)
			return -1;
		for (int i = 0; i < arrayToSearchThrough.length; i++)
		{
			boolean found = true;
			int index = 0;
			for (int j = 0; j < patternToFind.length; j++)
			{
				index = i+j > arrayToSearchThrough.length-1 ? i : i+j;
				if (arrayToSearchThrough[index] != patternToFind[j])
				{
					found = false;
					break;
				}
			}
			if (found)
			{
				long methodEnd = System.nanoTime();

				indexOfCounter+= methodEnd-methodStart;
				return i;
			}
		}
		
		long methodEnd = System.nanoTime();

		indexOfCounter+= methodEnd-methodStart;
		
		return -1;
	}

	/** Taken from package org.apache.james.dnsserver; */
	public static Record[] processSetResponse(SetResponse sr) {
		
		long methodStart = System.nanoTime();
		
		Record [] answers;
		int answerCount = 0, n = 0;

		RRset [] rrsets = sr.answers();
		answerCount = 0;
		for (int i = 0; i < rrsets.length; i++) {
			answerCount += rrsets[i].size();
		}

		answers = new Record[answerCount];

		for (int i = 0; i < rrsets.length; i++) {
			@SuppressWarnings("rawtypes")
			Iterator iter = rrsets[i].rrs();
			while (iter.hasNext()) {
				Record r = (Record)iter.next();
				answers[n++] = r;
			}
		}
		
		long methodEnd = System.nanoTime();

		processSetResponseCounter+= methodEnd-methodStart;
		
		return answers;
	}

	/**
	 * TODO
	 * @param dnsQueryInWire
	 * @return
	 */
	public static byte[] resolveDNSQueryToLocalhost(byte[] dnsQueryInWire){
		// TODO make it faster?
		long methodStart = System.nanoTime();
		
		Message msg = null;
		try {
			msg = new Message(dnsQueryInWire);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			return new byte[0];
		}

		int queryType = msg.getQuestion().getType();

		switch (queryType){
		case Type.PTR:
			msg.addRecord(new PTRRecord(msg.getQuestion().getName(), DClass.IN, 1, msg.getQuestion().getName()), Section.ANSWER);
		case Type.AAAA:
			msg.addRecord(new AAAARecord(msg.getQuestion().getName(), DClass.IN, 1, LOCALHOST_V6), Section.ANSWER);
		case Type.SRV:
			msg.addRecord(new SRVRecord(msg.getQuestion().getName(), DClass.IN, 1, 0, 5, 12345, msg.getQuestion().getName()), Section.ANSWER);
		case Type.A:
		default:
			msg.addRecord(new ARecord(msg.getQuestion().getName(), DClass.IN, 1, LOCALHOST), Section.ANSWER);
		}

		msg.getHeader().setFlag(Flags.RA);
		msg.getHeader().setFlag(Flags.QR);
		
		long methodEnd = System.nanoTime();

		resolveDNSToLocalhostCounter+= methodEnd-methodStart;

		return msg.toWire();
	}

	//copyed from jnamed bwelling@xbill.org
	public static byte[] formerrMessageForDNSQuery(byte[] in) {
		
		long methodStart = System.nanoTime();
		
		Header header;

		try {
			header = new Header(in);
		} catch (IOException e) {
			return new byte[0];
		}

		Message response = new Message();
		response.setHeader(header);

		for (int i = 0; i < 4; i++) {
			response.removeAllRecords(i);
		}
		header.setRcode(Rcode.FORMERR);
		
		long methodEnd = System.nanoTime();

		formerrMessageForDNSQueryCounter+= methodEnd-methodStart;

		return response.toWire();
	}


	public static String getCurrentDateTimeForFilename(){
		return dateFormat.format(Calendar.getInstance().getTime()); 
	}
	
	public static void printValues(){
		StringBuffer sb = new StringBuffer();
		sb.append("longToByteArrayCounter: " + TimeUnit.NANOSECONDS.toMillis(longToByteArrayCounter)).append(System.getProperty("line.separator"));
		sb.append("byteArrayToLongCounter: " + TimeUnit.NANOSECONDS.toMillis(byteArrayToLongCounter)).append(System.getProperty("line.separator"));
		sb.append("intToByteArrayCounter: " + TimeUnit.NANOSECONDS.toMillis(intToByteArrayCounter)).append(System.getProperty("line.separator"));
		sb.append("byteArrayToIntCounter: " + TimeUnit.NANOSECONDS.toMillis(byteArrayToIntCounter)).append(System.getProperty("line.separator"));
		sb.append("shortToByteArrayCounter: " + TimeUnit.NANOSECONDS.toMillis(shortToByteArrayCounter)).append(System.getProperty("line.separator"));
		sb.append("byteArrayToShortCounter: " + TimeUnit.NANOSECONDS.toMillis(byteArrayToShortCounter)).append(System.getProperty("line.separator"));
		sb.append("mergeArraysCounter: " + TimeUnit.NANOSECONDS.toMillis(mergeArraysCounter)).append(System.getProperty("line.separator"));
		sb.append("byteListToPrimitiveArrayListCounter: " + TimeUnit.NANOSECONDS.toMillis(byteListToPrimitiveArrayListCounter)).append(System.getProperty("line.separator"));
		sb.append("getHexCounter: " + TimeUnit.NANOSECONDS.toMillis(getHexCounter)).append(System.getProperty("line.separator"));
		sb.append("splitArrayOnPatternCounter: " + TimeUnit.NANOSECONDS.toMillis(splitArrayOnPatternCounter)).append(System.getProperty("line.separator"));
		sb.append("indexOfCounter: " + TimeUnit.NANOSECONDS.toMillis(indexOfCounter)).append(System.getProperty("line.separator"));
		sb.append("processSetResponseCounter: " + TimeUnit.NANOSECONDS.toMillis(processSetResponseCounter)).append(System.getProperty("line.separator"));
		sb.append("resolveDNSToLocalhostCounter: " + TimeUnit.NANOSECONDS.toMillis(resolveDNSToLocalhostCounter)).append(System.getProperty("line.separator"));
		sb.append("formerrMessageForDNSQueryCounter: " + TimeUnit.NANOSECONDS.toMillis(formerrMessageForDNSQueryCounter)).append(System.getProperty("line.separator"));
		
		System.out.println(sb.toString());
		
	}



}

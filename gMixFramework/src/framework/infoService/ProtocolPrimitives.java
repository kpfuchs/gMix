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
package framework.infoService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import framework.core.util.Util;


public class ProtocolPrimitives {

	public static boolean DEBUG;
	
	
	public static void sendString(OutputStream outputStream, String string) throws UnsupportedEncodingException, IOException {
		if (DEBUG)
			System.out.println("void sendString(" +string +")");
		outputStream.write(Util.intToByteArray((string.getBytes("UTF-8")).length));
		outputStream.write(string.getBytes("UTF-8"));
	}
	
	
	public static String receiveString(InputStream inputStream) throws UnsupportedEncodingException, IOException {
		if (DEBUG)
			System.out.println("String readString(): waiting");
		int len = Util.forceReadInt(inputStream);
		if (len == 0)
			return null;
		String result = new String(Util.forceRead(inputStream, len), "UTF-8");
		if (DEBUG)
			System.out.println("String readString(): result: " +result);
		return result;
	}
	
	
	public static void sendInt(OutputStream outputStream, int integer) throws IOException {
		if (DEBUG)
			System.out.println("void sendInt(" +integer +")");
		outputStream.write(Util.intToByteArray(integer));
	}
	
	
	public static int receiveInt(InputStream inputStream) throws IOException {
		if (DEBUG)
			System.out.println("int receiveInt(): waiting");
		int result = Util.forceReadInt(inputStream);
		if (DEBUG)
			System.out.println("int receiveInt(): result: " +result);
		return result;
	}
	

	public static void sendByteArray(OutputStream outputStream, byte[] byteArray) throws IOException {
		if (DEBUG)
			System.out.println("void sendByteArray(" +Util.md5(byteArray) +")");
		outputStream.write(Util.intToByteArray(byteArray.length));
		outputStream.write(byteArray);
	}
	
	
	public static byte[] receiveByteArray(InputStream inputStream) throws IOException {
		if (DEBUG)
			System.out.println("byte[] receiveByteArray(): waiting");
		int len = Util.forceReadInt(inputStream);
		if (len == 0)
			return null;
		byte[] result = Util.forceRead(inputStream, len);
		if (DEBUG)
			System.out.println("byte[] receiveByteArray(): result: " +Util.md5(result));
		return result;
	}
	
	
	public static void sendBoolean(OutputStream outputStream, boolean bool) throws IOException {
		if (DEBUG)
			System.out.println("void sendBoolean(" +bool +")");
		if (bool)
			outputStream.write((byte)1);
		else 
			outputStream.write((byte)0);
	}
	
	
	public static boolean receiveBoolean(InputStream inputStream) throws IOException {
		if (DEBUG)
			System.out.println("boolean receiveBoolean(): waiting");
		boolean result = inputStream.read() == 1;
		if (DEBUG)
			System.out.println("boolean receiveBoolean(): result: " +result);
		return result;
	}
	
}

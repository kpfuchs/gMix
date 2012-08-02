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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;


public class IOTester {
	
	private static final int INITIAL_BUFFER_SIZE = 5 * 1024 * 1024;
	private static final String ERRORO_MSG = "IOTester: ERROR DETECTED: mismatch between sent and received data";
	private static HashMap<String, IOTester> instances = new HashMap<String, IOTester>();
	
	private String identifier;
	private ByteBuffer dataCache = ByteBuffer.allocate(INITIAL_BUFFER_SIZE);
	
	
	private IOTester(String identifier) {
		this.identifier = identifier;
	}
	
	
	public static IOTester createInstance(String identifier) {
		if (instances.get(identifier) != null) {
			return null;
		} else {
			IOTester tester = new IOTester(identifier);
			instances.put(identifier, tester);
			return tester;
		}
	}
	
	public static IOTester findInstance(String identifier) {
		return instances.get(identifier);
	}
	
	
	public synchronized void addSendRecord(byte[] data) {
		//System.out.println("send (" +data.length +" bytes): " +Arrays.toString(data)); 
		if (data.length > dataCache.remaining()) { // resize buffer
			System.out.println("resizing buffer (" +dataCache.capacity() +"->" +(dataCache.capacity() +INITIAL_BUFFER_SIZE) +")"); 
			ByteBuffer old = dataCache;
			dataCache = ByteBuffer.allocate(dataCache.capacity() +INITIAL_BUFFER_SIZE);
			dataCache.put(old);
		}
		dataCache.put(data);
	}
	
	
	public synchronized void addReceiveRecord(byte[] data) {
		//System.out.println("receive (" +data.length +" bytes): " +Arrays.toString(data)); 
		if (data.length > dataCache.position()) {
			System.err.println(ERRORO_MSG +" (id: " +identifier +", CODE-1)");
			System.exit(1);
		} else {
			byte[] compare = new byte[data.length];
			dataCache.flip();
			dataCache.get(compare);
			dataCache.compact();
			for (int i=0; i<data.length; i++) {
				if (data[i] != compare[i]) {
					String msg = ERRORO_MSG +" (id: " +identifier +") CODE-2:"
							+ "\n\"" +data[i] +"\"!=\"" +compare[i] +"\""
							+ "\ndump (sent): \n"
							+ Arrays.toString(compare)
							+ "\ndump (received): \n"
							+ Arrays.toString(data);
					System.err.println(msg); 
					System.exit(1);
				}
			}
		}
	}
	
}

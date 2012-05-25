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
package staticFunctions.layer2recodingScheme.basicReplayDetection_v0_001;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;

import framework.core.AnonNode;
import framework.core.util.Util;


public class ReplayDetectionBasic {


	private final int DB_CAPACITY;
	private final float DB_LOAD_FACTOR;
	private Hashtable<Integer, byte[]> replayDatabase;
	private static HashMap<AnonNode, ReplayDetectionBasic> replayDetections = new HashMap<AnonNode, ReplayDetectionBasic>();

	
	public static ReplayDetectionBasic getInstance(AnonNode owner) {
		ReplayDetectionBasic rd = replayDetections.get(owner);
		if (rd == null) {
			rd = new ReplayDetectionBasic(owner);
			replayDetections.put(owner, rd);
		}
		return rd;
	}
	
	
	private ReplayDetectionBasic(AnonNode owner) {
		this.DB_CAPACITY = owner.getSettings().getPropertyAsInt("REPLAY_DB_CAPACITY");
		this.DB_LOAD_FACTOR = owner.getSettings().getPropertyAsFloat("DB_LOAD_FACTOR");
		this.replayDatabase = 
			new Hashtable<Integer, byte[]>(DB_CAPACITY, DB_LOAD_FACTOR);	
	}
	

	public synchronized boolean isReplay(byte[] identifier) {
		assert identifier.length >= 8;
		
		// switch key, if database is almost full
		if ((double)replayDatabase.size() > ((double)DB_LOAD_FACTOR*0.9d) * DB_CAPACITY) {
			System.out.println("key switch");
			// TODO
			this.replayDatabase = 
				new Hashtable<Integer, byte[]>(DB_CAPACITY, DB_LOAD_FACTOR);
		}
		
		/* 
		 * Use first 4 bytes as key for Hashtabel (appropreate, since 
		 * java.util.Hashtable uses integers as keys (see class implementation 
		 * comment for further information)).
		 */
		Integer key = Util.byteArrayToInt(Arrays.copyOf(identifier, 4));
		
		/*
		 * 8 bytes -> with 2^64 = 18,446,744,073,709,551,616 
		 * possibilities, collisions are already almost impossible and the 
		 * space for the remaining 24 bytes can be saved for each entry.
		 */
		byte[] value = Arrays.copyOf(identifier, 8);
		
		// perform replay detection
		boolean isReplay = (replayDatabase.put(key, value) == null) ? false : true;
		
		if (isReplay)
			System.out.println("replay");
		
		return isReplay;
	}
	
}
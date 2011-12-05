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

package messageProcessor;


import internalInformationPort.InternalInformationPortController;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.logging.Logger;

import networkClock.NetworkClockController;

import message.BasicMessage;
import message.ChannelEstablishMessage;

import util.Util;


/**
 * Detects whether a message has already been processed, or not. Detection is 
 * done using timestamps and hash tables.
 * <p>
 * This class is thread-safe (but parallel execution won't increase 
 * performance).
 * 
 * @author Karl-Peter Fuchs
 */
class ReplayDetection {
	/*
	 * Uses timestamps to keep number of messages in replay database low. Uses 
	 * hash table since searching elements in this data structure is very 
	 * efficient (constant time with well chosen values).
	 * Since searching for outdated timestamps in a hash table is inefficient, 
	 * this class uses two hash tables, each holding all timestamps from a 
	 * certain amount of time (twice the maximum message delay). After this 
	 * amount of time, all timestamps in one table are obsolete and therefore 
	 * the whole table can simply be replaced by a new one without dealing with
	 * single entries.
	 * 
	 * Note: Java uses integers as hash codes (see java.lang.Object). Therefore,
	 * the length of a key in a java.util.Hashtable is always 32 bit and can't 
	 * be adjusted.
	 */

	/** 
	 * Reference on <code>InternalInformationPort</code>. Used to display 
	 * and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	/** 
	 * Reference on component <code>NetworkClock</code> used to validate 
	 * timestamps.
	 */
	private static NetworkClockController clock = new NetworkClockController();
	
	/**
	 * Number of items the database for replay detection should approximately 
	 * hold.
	 */
	private final int INITIAL_DB_CAPACITY;
	
	/**
	 * The load factor for the hash tables <code>replayDatabase1</code> and 
	 * <code>replayDatabase2</code>. 
	 */
	private final float DB_LOAD_FACTOR;
	
	/** Maximum divergence between mix' and client's clock in ms. */
	private final long TOLERANCE;
	
	/** Time after which a message is considered as out of date in ms. */
	private final long MAX_DELAY;
	
	/** First replay database. */
	private Hashtable<Integer, byte[]> replayDatabase1;
	
	/** Second replay database. */
	private Hashtable<Integer, byte[]> replayDatabase2;
	
	/**
	 * Point of time, the databases were switched (and one got replaced) lastly.
	 */
	private long lastDBSwitch;
	
	/**
	 * Indicates weather to write in the first or second replay database.
	 */
	private boolean writeToDB1;

	
	/**
	 * Loads values from property file and initializes hash tables.
	 */
	protected ReplayDetection() {
		
		this.INITIAL_DB_CAPACITY = 
			new Integer(internalInformationPort.getProperty(
					"INITIAL_REPLAY_DB_CAPACITY")
				);

		this.TOLERANCE = 
			new Long(
					internalInformationPort.getProperty("TIMESTAMP_TOLERANCE")
					);
		
		this.MAX_DELAY = 
			new Long(internalInformationPort.getProperty("MAX_MESSAGE_DELAY"));
		
		this.DB_LOAD_FACTOR = 
			new Float(internalInformationPort.getProperty("DB_LOAD_FACTOR"));
		
		this.replayDatabase1 = 
			new Hashtable<Integer, byte[]>(	INITIAL_DB_CAPACITY / 2,
											DB_LOAD_FACTOR
											);

			this.replayDatabase2 = 
			new Hashtable<Integer, byte[]>(	INITIAL_DB_CAPACITY / 2, 
											DB_LOAD_FACTOR
											);

			this.lastDBSwitch = clock.getTime();
			this.writeToDB1 = true;
			
	}
	
	
	/**
	 * Detects whether a message has already been processed, or not. Detection 
	 * is done using timestamps and hash tables.
	 * 
	 * @param basicMessage	The message to be checked.
	 * 
	 * @return				Indicates whether the bypasses message is a replay  
	 * 						or not.
	 */
	protected boolean isReplay(BasicMessage basicMessage) {
		
		ChannelEstablishMessage message = 
			(ChannelEstablishMessage)basicMessage;
		
		/*
		 * Mac key is used as identifier (appropriate, since the mac key is long
		 * enough (32 byte) and randomly chosen).
		 */
		byte[] identifier = message.getMACKey().getEncoded();
		
		/* 
		 * Use first 4 bytes as key for Hashtabel (appropreate, since 
		 * java.util.Hashtable uses integers as keys (see class implementation 
		 * comment for further information)).
		 */
		Integer key = Util.byteArrayToInt(Arrays.copyOf(identifier, 4));
		
		/*
		 * Use first 8 bytes as value (with 2^64 = 18,446,744,073,709,551,616 
		 * possibilities, collisions are already almost impossible and the 
		 * space for the remaining 24 bytes can be saved for each entry).
		 */
		byte[] value = Arrays.copyOf(identifier, 8);
		
		long timestamp = message.getTimestamp();
		
		// perform replay detection
		boolean result = isReplay(key, value, timestamp);
		
		if (result == true) {
			
			LOGGER.fine(	"(ReplayDetection) Message: " 
							+message.getChannelID() +":" 
							+message.getMessageID()
							);
			
			LOGGER.finer(message.toString());
			
		}
		
		return result;
		
	}
	
	
	/**
	 * Detects whether a message has already been processed, or not. Detection 
	 * is done using timestamps and hash tables.
	 * 
	 * @param key		The message's key in the hash table.
	 * @param value		The message's value in the hash table.
	 * @param timestamp	The message's timestamp.
	 * @return			Indicates whether the bypasses message is a replay or 
	 * 					not.
	 */
	private synchronized boolean isReplay(	Integer key, 
											byte[] value, 
											long timestamp) {
		/* Replay detection can't be parallelized. */

		// value to be returned at the end of the method
		boolean result;
		
		// local timestamp
		long now = clock.getTime();
		
		if (timestamp > now + TOLERANCE) { // timestamp too far in the future
			
			LOGGER.fine("(ReplayDetection) Timestamp too far in the future!");

			result = true;
			
		} else { // timestamp not too far in the future
			
			if (MAX_DELAY > (now - TOLERANCE - timestamp)) {
				// timestamp not yet expired

				if (!isAlreadyInDB(key, value)) {
					
					// insert
					if (writeToDB1) { // replayDatabase1 is active
						
						replayDatabase1.put(key, value);
						
					} else { // replayDatabase2 is active
						
						replayDatabase2.put(key, value);
						
					}
					
					result = false;
					
				} else {
					
					LOGGER.fine(	"(ReplayDetection) Message is a replay: " 
									+"Message already in DB"
									);
					
					result = true;
						
				}
		
			} else { // timestamp expired
				
				LOGGER.fine(	"(ReplayDetection) Message is a replay: " 
								+"Timestamp expired"
								);

				result = true;
				
			}
			
		}
		
		// if one database is obsolete: switch databases
		if (MAX_DELAY <= (now - lastDBSwitch - TOLERANCE)) {
			
			if (writeToDB1) { // if replayDatabase2 is obsolete
				
				replayDatabase2 = replayDatabase1; // switch dbs
				replayDatabase1 = 
					new Hashtable<Integer, byte[]>(INITIAL_DB_CAPACITY / 2);
											
			} else { // replayDatabase1 is obsolete
				
				replayDatabase1 = replayDatabase2; // switch dbs
				replayDatabase2 = 
					new Hashtable<Integer, byte[]>(INITIAL_DB_CAPACITY / 2);
											
			}
			
			writeToDB1 = !writeToDB1;
			lastDBSwitch = clock.getTime();
		}
		
		return result;		
	}
	
	
	/**
	 * Detects whether a key is in one of the databases or not.
	 * 
	 * @param key	The key to be checked.
	 * 
	 * @return		Indicates whether the bypasses key is in one of the 
	 * 				databases or not
	 */
	private boolean isAlreadyInDB(Integer key, byte[] value) {
		
		// check replayDatabase1
		if (replayDatabase1.containsKey(key)) { // key is present
			
			// retrieve value for comparison
			byte[] savedValue = replayDatabase1.get(key);
			
			if (Arrays.equals(value, savedValue)) { // value is present as well
				
				return true;
				
			}
			
		}
		
		// check replayDatabase2
		if (replayDatabase2.containsKey(key)) { // key is present
			
			// retrieve value for comparison
			byte[] savedValue = replayDatabase2.get(key);
			
			if (Arrays.equals(value, savedValue)) { // value is present as well
				
				return true;
				
			}
			
		}
		
		// no entry was found in both databases
		return false;
		
	}
	
}
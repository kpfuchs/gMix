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

package userDatabase;


import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import exception.UserAlreadyExistingException;
import exception.UnknownUserException;

import architectureInterface.UserDatabaseInterface;

import internalInformationPort.InternalInformationPortController;


/**
 * Controller class of component <code>UserDatabase</code>. Implements the 
 * architecture interface <code>UserDatabaseInterface</code>. 
 * <p>
 * Used to store user-specific data (e. g. identifiers, session keys and 
 * buffers).
 * 
 * @author Karl-Peter Fuchs
 */
public class UserDatabaseController implements UserDatabaseInterface {

	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	/** 
	 * Period of time without actions, after which a channel is considered as 
	 * inactive.
	 */
	private final int INACTIVITY_TIMEOUT;
	
	/**
	 * <code>Hashtable</code> containing references on all available 
	 * <code>User</code>s, retrievable via a local user identifier 
	 * (<code>Integer</code>).
	 */
	private Hashtable<Integer, User> localUserIDs;
	
	/**
	 * <code>Hashtable</code> containing references on all available 
	 * <code>User</code>s, retrievable via a the next mix' user identifier 
	 * (<code>Integer</code>).
	 * <p>
	 * Note: The identifier differs from mix to mix to prevent linkability.
	 */
	private Hashtable<Integer, User> nextMixUserIDs;
	
	
	/**
	 * Generates a new <code>UserDatabase</code> component.
	 * <p>
	 * Used to store user-specific data (e. g. identifiers, session keys and 
	 * buffers).
	 */
	public UserDatabaseController() {
		
		int channelDBCapacity = 
			new Integer(internalInformationPort.getProperty("MAX_CONNECTIONS"));

		this.INACTIVITY_TIMEOUT = 
			new Integer(internalInformationPort.getProperty(
					"INACTIVITY_TIMEOUT")
					);
		
		long cleanDBInterval = 
			new Integer(internalInformationPort.getProperty(
					"CLEAN_DB_INTERVAL")
					);
		
		this.localUserIDs = 
			new Hashtable<Integer, User>(channelDBCapacity);
		
		this.nextMixUserIDs = 
			new Hashtable<Integer, User>(channelDBCapacity);
		
		// clean database every "updateRate" ms
		Timer timer = new Timer(); 
		
		timer.schedule(	new cleanDatabaseTask(), 
						cleanDBInterval, 
						cleanDBInterval
						); 
		
	}

	
	/**
	 * Initializes the this component.
	 */
	public void initialize() {
		
	}

	
	/**
	 * Adds the bypassed <code>User</code> to the internal database.
	 * 
	 * @param user	The <code>User</code> to be added. 
	 * 
	 * @throws UserAlreadyExistingException	Thrown when the bypassed <code>
	 * 										User</code> has already been added 
	 * 										(user's identifier already in use).
	 */
	public void addUser(User user) throws 
			UserAlreadyExistingException {
		
		if (	localUserIDs.get(user.getIdentifier()) != null
				||
				nextMixUserIDs.get(user.getIdentifierForNextMix()) 
					!= null
				) {
			
			throw new UserAlreadyExistingException();
			
		} else {
			
			localUserIDs.put(user.getIdentifier(), user);
			nextMixUserIDs.put(user.getIdentifierForNextMix(), user);
			
		}
		
	}


	/**
	 * Returns the number of <code>User</code>s currently stored in the 
	 * internal database.
	 * 
	 * @return	Number of <code>User</code>s currently stored in the internal 
	 * 			database.
	 */
	public int getSize() {
		
		return localUserIDs.size();
		
	}


	/**
	 * Returns the <code>User</code> with the bypassed identifier.
	 * 
	 * @param identifier	Identifier of the <code>User</code> to be returned.
	 *  
	 * @return				<code>User</code> with the bypassed identifier.
	 * 
	 * @throws UnknownUserException		Thrown when no <code>User</code> with 
	 * 									the bypassed identifier is existent.
	 */
	public User getUser(int identifier) throws UnknownUserException {
		
		User channel = localUserIDs.get(identifier);
		
		if (channel == null ) {
			
			throw new UnknownUserException();
			
		} else {
			
			return channel;
		
		}
		
	}


	/**
	 * Returns whether a <code>User</code> with the bypassed identifier is 
	 * present in the internal database or not.
	 * 
	 * @param identifier	Identifier to search for.
	 * 
	 * @return	<code>User</code> present or not.
	 */
	public boolean isExistingUser(int identifier) {
		
		return (localUserIDs.get(identifier) != null) ? true : false;
		
	}


	/**
	 * Removes the <code>User</code> with the bypassed identifier.
	 * 
	 * @param identifier	Identifier of the <code>User</code> to be removed 
	 * 						from the internal database.
	 *  
	 * @throws UnknownUserException	Thrown when no <code>User</code> with 
	 * 									the bypassed identifier is existent.
	 */
	public void removeUser(int identifier) throws UnknownUserException {
		
		User channel = localUserIDs.get(identifier);
		
		if (channel == null) {
			
			throw new UnknownUserException();
			
		} else {
			
			nextMixUserIDs.remove(channel.getIdentifierForNextMix());
			localUserIDs.remove(identifier);
			
		}
		
	}


	/**
	 * Returns the <code>User</code> with the bypassed identifier.
	 * 
	 * @param nextMixIdentifier	Identifier of the <code>User</code> to be 
	 * 							returned.
	 *  
	 * @return					<code>User</code> with the bypassed identifier.
	 * 
	 * @throws UnknownUserException		Thrown when no <code>User</code> with 
	 * 									the bypassed identifier is existent.
	 */
	public User getUserByNextMixIdentifier(int nextMixIdentifier)
			throws UnknownUserException {
		
		User channel = nextMixUserIDs.get(nextMixIdentifier);
		
		if (channel == null ) {
			
			throw new UnknownUserException();
			
		} else {
			
			return channel;
		
		}
		
	}


	/**
	 * Returns all <code>User</code>s that are currently active.
	 * 
	 * @return	Collection of all <code>User</code>s currently active.
	 */
	public Collection<User> getActiveUsers() {
		
		return localUserIDs.values();
		
	}
	
	
	/**
	 * Removes <code>User</code>s from the internal database that were inactive 
	 * for at least <code>INACTIVITY_TIMEOUT</code> ms.
	 * 
	 * @see #INACTIVITY_TIMEOUT
	 */
	private void cleanDatabase() {
		/* 
		 * Note: For performance reasons, this method is NOT synchronized. 
		 * Therefore, the iterator "channelsInDatabase" might contain references 
		 * on null. See try-catch-block.
		 */
		
		LOGGER.fine("Searching for inactive channels.");
		
		Iterator<User> channelsInDatabase = 
			localUserIDs.values().iterator();
		
		User channel;
		
		while (channelsInDatabase.hasNext()) {
			
			try {
				
				channel = channelsInDatabase.next();
				
				if (!channel.isStillValid(INACTIVITY_TIMEOUT)) {
					
					LOGGER.fine(	"Channel " +channel.getIdentifier() +" was "
									+"closed due to inactivity!"
									);
					
					removeUser(channel.getIdentifier());
					
				}
				
			} catch (NullPointerException e) {
				// channel got closed in the meantime 
				
				continue;
				
			} catch (UnknownUserException e) {

				continue;
				
			}
			
		}
		
		LOGGER.fine("Search for inactive channels complete.");
		
	}
	
	
	/**
	 * Simple <code>TimerTask</code>, which calls <code>cleanDatabase()</code>.
	 * 
	 * @see UserDatabaseController#cleanDatabase()
	 * 
	 * @author Karl-Peter Fuchs
	 */
	private final class cleanDatabaseTask extends TimerTask {

		/** 
		 * Calls the method <code>cleanDatabase()</code>.
		 */
		@Override 
		public void run() {
			
			cleanDatabase();
			
		}
		
	}

}

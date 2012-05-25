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

import java.util.Vector;


import framework.Controller;
import framework.LocalClassLoader;
import framework.Mix;
import framework.observer.ObservationSubject;


/**
 * Controller class of component <code>UserDatabase</code>. Implements the 
 * architecture interface <code>UserDatabase</code>. 
 * <p>
 * Used to store user-specific data (e. g. identifiers, session keys and 
 * buffers).
 * 
 * @author Karl-Peter Fuchs
 */
public class UserDatabaseController extends Controller implements UserDatabase, ObservationSubject<DatabaseEventListener> {

	private UserDatabase userDatabaseImplementation;
	private int userIdCounter = 0;
	private Vector<DatabaseEventListener> eventListeners;
	
	
	public UserDatabaseController(Mix mix) {
		super(mix);
		this.eventListeners = new Vector<DatabaseEventListener>();
	}
	
	
	public User generateUser() { // TODO: nach versch. user-typen unterscheiden
		User newUser = new User(userIdCounter++, this);
		//addUser(newUser);
		return newUser;
	}

	public User generateUser(int id) { // TODO: nach versch. user-typen unterscheiden
		User newUser = new User(id, this);
		//addUser(newUser);
		return newUser;
	}
	
	
	@Override
	public void registerEventListener(DatabaseEventListener dbel) {
		synchronized (eventListeners) {
			eventListeners.add(dbel);
		}
	}
	
	
	@Override
	public boolean unregisterEventListener(DatabaseEventListener dbel) {
		synchronized (eventListeners) {
			return eventListeners.remove(dbel);
		}
	}
	
	
	@Override
	public void addUser(User user) {
		if (user != null) {
			userDatabaseImplementation.addUser(user);
			synchronized (eventListeners) {
				for (DatabaseEventListener dbel:eventListeners)
					dbel.userAdded(user);
			}
		}
	}
	
	
	// avoid notification for calling instance
	public void addUser(User user, DatabaseEventListener callingInstance) {
		if (user != null) {
			userDatabaseImplementation.addUser(user);
			synchronized (eventListeners) {
				for (DatabaseEventListener dbel:eventListeners)
					if (dbel != callingInstance)
						dbel.userAdded(user);
			}
		}
	}

	
	@Override
	public User removeUser(User user) {
		
		User removedUser = userDatabaseImplementation.removeUser(user);
		
		if (removedUser != null) {
			synchronized (eventListeners) {
				for (DatabaseEventListener dbel:eventListeners)
					dbel.userRemoved(user);
			}
		}

		return removedUser;
		
	}
	
	
	// avoid notification for calling instance
	public User removeUser(User user, DatabaseEventListener callingInstance) {
		
		User removedUser = userDatabaseImplementation.removeUser(user);
		
		if (removedUser != null) {
			synchronized (eventListeners) {
				for (DatabaseEventListener dbel:eventListeners)
					if (dbel != callingInstance)
						dbel.userRemoved(user);
			}
		}

		return removedUser;
		
	}
	
	
	@Override
	public User removeUser(int identifier) {
		return removeUser(getUser(identifier));
	}
		
	
	// avoid notification for calling instance
	public User removeUser(int identifier, DatabaseEventListener callingInstance) {
		return removeUser(getUser(identifier), callingInstance);
	}
	

	@Override
	public User getUser(int identifier) {
		return userDatabaseImplementation.getUser(identifier);
	}

	
	@Override
	public boolean isExistingUser(int identifier) {
		return userDatabaseImplementation.isExistingUser(identifier);
	}

	
	@Override
	public void instantiateSubclass() {
		userDatabaseImplementation = LocalClassLoader.instantiateImplementation(this, UserDatabase.class, settings);	
	}


	@Override
	public int getNumberOfUsers() {
		return userDatabaseImplementation.getNumberOfUsers();
	}


	@Override
	public User[] getAllUsers() {
		return userDatabaseImplementation.getAllUsers();
	}


	@Override
	public String getPropertyKey() {
		return "USER_DATABASE";
	}

}

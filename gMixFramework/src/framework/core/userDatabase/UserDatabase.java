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
package framework.core.userDatabase;

import java.util.HashMap;
import java.util.Vector;

import framework.core.observer.ObservationSubject;


/**
 * Controller class of component <code>UserDatabase</code>. Implements the 
 * architecture interface <code>UserDatabase</code>. 
 * <p>
 * Used to store user-specific data (e. g. identifiers, session keys and 
 * buffers).
 */
public class UserDatabase implements ObservationSubject<DatabaseEventListener> {

	private int userIdCounter = 0;
	private Vector<DatabaseEventListener> eventListeners;
	private HashMap<String, User> users = new HashMap<String, User>(); 
	
	public UserDatabase() {
		this.eventListeners = new Vector<DatabaseEventListener>();
		
	}
	
	
	public User generateUser() { // TODO: distinguish between different types of users
		User newUser = new User(userIdCounter++, this);
		//addUser(newUser);
		return newUser;
	}

	
	public User generateUser(int id) { // TODO: distinguish between different types of users
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
	
	
	public void addUser(User user) {
		if (user != null) {
			users.put(""+user.getIdentifier(), user);
			synchronized (eventListeners) {
				for (DatabaseEventListener dbel:eventListeners)
					dbel.userAdded(user);
			}
		}
	}
	
	
	// avoid notification for calling instance
	public void addUser(User user, DatabaseEventListener callingInstance) {
		if (user != null) {
			users.put(""+user.getIdentifier(), user);
			synchronized (eventListeners) {
				for (DatabaseEventListener dbel:eventListeners)
					if (dbel != callingInstance)
						dbel.userAdded(user);
			}
		}
	}

	
	public User removeUser(User user) {
		User removedUser = users.remove(""+user.getIdentifier());
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
		User removedUser = users.remove(""+user.getIdentifier());
		if (removedUser != null) {
			synchronized (eventListeners) {
				for (DatabaseEventListener dbel:eventListeners)
					if (dbel != callingInstance)
						dbel.userRemoved(user);
			}
		}
		return removedUser;
	}
	
	
	public User removeUser(int identifier) {
		return users.remove(""+getUser(identifier));
	}
		
	
	// avoid notification for calling instance
	public User removeUser(int identifier, DatabaseEventListener callingInstance) {
		return removeUser(getUser(identifier), callingInstance);
	}
	

	public User getUser(int identifier) {
		return users.get(""+identifier);
	}

	
	public boolean isExistingUser(int identifier) {
		return users.containsKey(""+identifier);
	}


	public int getNumberOfUsers() {
		return users.size();
	}


	public User[] getAllUsers() {
		return users.values().toArray(new User[0]);
	}

}

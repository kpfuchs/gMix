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

package architectureInterface;


import java.util.Collection;

import userDatabase.User;

import exception.UserAlreadyExistingException;
import exception.UnknownUserException;



/**
 * Architecture interface for component <code>UserDatabase</code>. 
 * <p>
 * Used to store user-specific data (e. g. identifiers, session keys and 
 * buffers).
 * <p>
 * Must be thread-safe.
 * 
 * @author Karl-Peter Fuchs
 */
public interface UserDatabaseInterface {

	
	/**
	 * Must add the bypassed <code>User</code> to the internal database.
	 * 
	 * @param user	The <code>User</code> to be added. 
	 * 
	 * @throws UserAlreadyExistingException	Thrown when the bypassed <code>
	 * 										User</code> has already been added 
	 * 										(user's identifier already in use).
	 */
	public void addUser(User user) 
		throws UserAlreadyExistingException;
	
	
	/**
	 * Must remove the <code>User</code> with the bypassed identifier.
	 * 
	 * @param identifier	Identifier of the <code>User</code> to be removed 
	 * 						from the internal database.
	 *  
	 * @throws UnknownUserException		Thrown when no <code>User</code> with 
	 * 									the bypassed identifier is existent.
	 */
	public void removeUser(int identifier) throws UnknownUserException;
	
	
	/**
	 * Must return the <code>User</code> with the bypassed identifier.
	 * 
	 * @param identifier	Identifier of the <code>User</code> to be returned.
	 *  
	 * @return				<code>User</code> with the bypassed identifier.
	 * 
	 * @throws UnknownUserException		Thrown when no <code>User</code> with 
	 * 									the bypassed identifier is existent.
	 */
	public User getUser(int identifier) throws UnknownUserException;
	
	
	
	/**
	 * Must return the <code>User</code> with the bypassed identifier.
	 * 
	 * @param nextMixIdentifier	Identifier of the <code>User</code> to be 
	 * 							returned.
	 *  
	 * @return					<code>User</code> with the bypassed identifier.
	 * 
	 * @throws UnknownUserException		Thrown when no <code>User</code> with 
	 * 									the bypassed identifier is existent.
	 */
	public User getUserByNextMixIdentifier(int nextMixIdentifier) throws 
			UnknownUserException;
	
	
	/**
	 * Must return whether a <code>User</code> with the bypassed identifier is 
	 * present in the internal database or not.
	 * 
	 * @param identifier	Identifier to search for.
	 * 
	 * @return	<code>User</code> present or not.
	 */
	public boolean isExistingUser(int identifier);
	
	
	/**
	 * Must return the number of <code>User</code>s currently stored in the 
	 * internal database.
	 * 
	 * @return	Number of <code>User</code>s currently stored in the internal 
	 * 			database.
	 */
	public int getSize();
	
	
	/**
	 * Must return all <code>User</code>s currently active.
	 * 
	 * @return	Collection of all <code>User</code>s currently active.
	 */
	public Collection<User> getActiveUsers();
	
}

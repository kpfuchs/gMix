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
import exception.UserAlreadyExistingException;
import exception.UnknownUserException;
import framework.Controller;
import framework.LocalClassLoader;
import framework.Mix;
import architectureInterface.UserDatabaseInterface;


/**
 * Controller class of component <code>UserDatabase</code>. Implements the 
 * architecture interface <code>UserDatabaseInterface</code>. 
 * <p>
 * Used to store user-specific data (e. g. identifiers, session keys and 
 * buffers).
 * 
 * @author Karl-Peter Fuchs
 */
public class UserDatabaseController extends Controller implements UserDatabaseInterface {

	private UserDatabaseInterface userDatabaseImplementation;
	
	
	public UserDatabaseController(Mix mix) {
		super(mix);
	}
	

	@Override
	public void addUser(User user) throws UserAlreadyExistingException {
		userDatabaseImplementation.addUser(user);
	}

	@Override
	public void removeUser(int identifier) throws UnknownUserException {
		userDatabaseImplementation.removeUser(identifier);
	}

	@Override
	public User getUser(int identifier) throws UnknownUserException {
		return userDatabaseImplementation.getUser(identifier);
	}

	@Override
	public User getUserByNextMixIdentifier(int nextMixIdentifier)
			throws UnknownUserException {
		return userDatabaseImplementation.getUserByNextMixIdentifier(nextMixIdentifier);
	}

	@Override
	public boolean isExistingUser(int identifier) {
		return userDatabaseImplementation.isExistingUser(identifier);
	}

	@Override
	public int getSize() {
		return userDatabaseImplementation.getSize();
	}

	@Override
	public Collection<User> getActiveUsers() {
		return userDatabaseImplementation.getActiveUsers();
	}

	@Override
	public void instantiateSubclass() {
		userDatabaseImplementation = LocalClassLoader.instantiateUserDatabaseImplementation(this);	
	}

}

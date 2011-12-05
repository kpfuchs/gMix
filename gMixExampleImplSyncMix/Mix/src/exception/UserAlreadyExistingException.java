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

package exception;


/**
 * Exception thrown by <code>UserDatabase.addUser()</code> when the 
 * identifier of the user to be added is already in use by another user.
 * 
 * @see userDatabase
 * 
 * @author Karl-Peter Fuchs
 */
public final class UserAlreadyExistingException extends Exception {
	
	/** The serialVersionUID as identifier for this serializable class. */
	private static final long serialVersionUID = 1242342345678899458L;
	
	
	/**
	 * Constructs an InvalidPortException (empty constructor).
	 */
	public UserAlreadyExistingException() {
		
	}
	
	
	/**
	 * Returns the String "This user's identifier is already in use by 
	 * another user!"
	 * @return 	"This user's identifier is already in use by another 
	 * 			user!".
	 */
	public String getMessage() {
		
		return 	"This user's identifier is already in use by another "
				+"user!";
		
	}
	
	
	/**
	 * Returns the String "UserAlreadyExistingException".
	 * @return "UserAlreadyExistingException"
	 */
	public String toString() {
		
		return "UserAlreadyExistingException";
		
	}
	
}
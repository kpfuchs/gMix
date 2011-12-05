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
 * Exception thrown when a <code>MessagePart</code> that shall be assigned is 
 * of wrong size.
 * 
 * @see message
 * 
 * @author Karl-Peter Fuchs
 */
public final class MessagePartHasWrongSizeException extends Exception {
	
	/**The serialVersionUID as identifier for this serializable class*/
	private static final long serialVersionUID = 34890534444318L;
	
	
	/**
	 * Constructs a MessagePartHasWrongSizeException (empty constructor).
	 */
	public MessagePartHasWrongSizeException() {
		
	}
	
	
	/**
	 * Returns the String "MessagePart has wrong size".
	 * @return "MessagePart has wrong size"
	 */
	public String getMessage() {
		
		return "MessagePart has wrong size";
		
	}
	
	
	/**
	 * Returns the String "MessagePartHasWrongSizeException".
	 * @return "MessagePartHasWrongSizeException"
	 */
	public String toString() {
		
		return "MessagePartHasWrongSizeException";
		
	}
	
}
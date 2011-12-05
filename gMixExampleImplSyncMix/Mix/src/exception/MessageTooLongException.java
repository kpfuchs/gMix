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
 * Exception thrown when a <code>Client</code> tries to generate a mix message, 
 * but the bypassed message (= payload) is larger than the maximum message size 
 * (see package <code>message</code> for the maximum allowed size).
 * 
 * @see message
 * 
 * @author Karl-Peter Fuchs
 */
public final class MessageTooLongException extends Exception {
	
	/**The serialVersionUID as identifier for this serializable class*/
	private static final long serialVersionUID = 34896789939458L;
	
	
	/**
	 * Constructs a MessageTooLongException (empty constructor).
	 */
	public MessageTooLongException() {
		
	}
	
	
	/**
	 * Returns the String "Message is too long".
	 * @return "Message is too long"
	 */
	public String getMessage() {
		
		return "Message is too long";
		
	}
	
	
	/**
	 * Returns the String "MessageTooLongException".
	 * @return "MessageTooLongException"
	 */
	public String toString() {
		
		return "MessageTooLongException";
		
	}
	
}

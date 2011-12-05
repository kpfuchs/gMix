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
 * Exception thrown by <code>InfromationGrabber</code> when a requested 
 * <code>Information</code> could not be retrieved.
 * 
 * @see externalInformationPort
 * 
 * @author Karl-Peter Fuchs
 */
public final class InformationRetrieveException extends Exception {
	
	/** The serialVersionUID as identifier for this serializable class. */
	private static final long serialVersionUID = 124321567889899458L;
	
	
	/**
	 * Constructs an InvalidPortException (empty constructor).
	 */
	public InformationRetrieveException() {
		
	}
	
	
	/**
	 * Returns the String "The requested information could not be retrieved!"
	 * @return "The requested information could not be retrieved!".
	 */
	public String getMessage() {
		
		return 	"The requested information could not be retrieved!";
		
	}
	
	
	/**
	 * Returns the String "InformationNotAvailableException".
	 * @return "InformationNotAvailableException"
	 */
	public String toString() {
		
		return "InformationRetrieveException";
		
	}
	
}

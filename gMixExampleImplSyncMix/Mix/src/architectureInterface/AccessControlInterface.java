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


import message.Message;


/**
 * Architecture interface for component <code>AccessControl</code>. 
 * <p>
 * Must be thread-safe.
 * 
 * @author Karl-Peter Fuchs
 */
public interface AccessControlInterface {

	
	/**
	 * Must perform an integrity check on the bypassed message.
	 * 
	 * @param message The message to be checked.
	 * 
	 * @return Result of integrity check.
	 */
	public boolean isMACCorrect(Message message);
	
}

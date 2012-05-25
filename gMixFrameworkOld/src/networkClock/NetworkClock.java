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

package networkClock;

import framework.ArchitectureInterface;


/**
 * Architecture interface for component <code>NetworkClock</code>. 
 * <p>
 * Provides time information from a synchronized clock.
 * <p>
 * Must be thread-safe.
 * 
 * @author Karl-Peter Fuchs
 */
public interface NetworkClock extends ArchitectureInterface {

	
	/**
	 * Must return the number of milliseconds since January 1, 1970, 00:00:00 
	 * GMT from a synchronized clock.
	 * 
	 * @return	The number of milliseconds since January 1, 1970, 00:00:00 GMT.
	 */
	public long getTime();
	
}
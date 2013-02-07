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
package evaluation.traceParser.engine.comparator;

import java.util.Comparator;

import framework.core.util.Util;


public class FlowGroupComparator implements Comparator<String> {

	
	/**
	 * Returns a negative integer, zero, or a positive integer as the first 
	 * argument is less than, equal to, or greater than the second.
	 * 
	 * Note: this comparator imposes orderings that are inconsistent with 
	 * equals. This comparator will only compare the 
	 * "senderId" and "start" values of class FlowGroup.
	 */
	@Override
	public int compare(String s1, String s2) {
		String id1AsString = s1.substring(0, s1.indexOf('|'));
		String id2AsString = s2.substring(0, s2.indexOf('|'));
		long id1 = Long.parseLong(id1AsString);
		long id2 = Long.parseLong(id2AsString);
		if (id1 < id2) {
			return -1;
		} else if (id1 > id2) {
			return 1;
		} else { // decide by start offset
			String offset1AsString = s1.substring(s1.indexOf('|')+1, Util.getIthIndexOf('|', s1, 2));
			String offset2AsString = s2.substring(s2.indexOf('|')+1, Util.getIthIndexOf('|', s2, 2));
			long offset1 = Long.parseLong(offset1AsString);
			long offset2 = Long.parseLong(offset2AsString);
	        return	(offset1 == offset2 ?  0 :	// Values are equal
	            	(offset1 < offset2 ? -1 :	// (-0.0, 0.0) or (!NaN, NaN)
	            	1));						// (0.0, -0.0) or (NaN, !NaN)
		}
	}
}

/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
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
 *******************************************************************************/
package evaluation.simulator.gui.helper;

import java.util.Comparator;

import evaluation.simulator.annotations.property.SimProp;

/**
 * Comparing two {@link SimProp}
 * 
 * @author nachkonvention
 * 
 */
public class SimpropComparator implements Comparator<SimProp> {

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(SimProp o1, SimProp o2) {
		
		if (o1.getPosition() != o2.getPosition()) {
			
			if (o1.getPosition() > o2.getPosition() )
				return -1;
			
			return 1;
			
		}
		return o1.getName().compareTo(o2.getName());
	}

}

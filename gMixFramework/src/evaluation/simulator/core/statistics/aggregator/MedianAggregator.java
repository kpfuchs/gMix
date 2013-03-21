/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2013  Karl-Peter Fuchs
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
package evaluation.simulator.core.statistics.aggregator;

import gnu.trove.TDoubleArrayList;


public class MedianAggregator extends AggregatorImplementation {

	@Override
	public double aggregate(TDoubleArrayList recordedValues) {
		if (recordedValues.size() == 0) {
			System.err.println("WARNING: no data recorded (MedianAggregator)"); 
			return 0d;
		}
		TDoubleArrayList clone = (TDoubleArrayList) recordedValues.clone();
		clone.sort();
		int mid = (int)Math.round((double)clone.size() / 2d);
		if (mid == 0)
			throw new RuntimeException("ERROR: no data!");
		else
			return clone.get(mid-1);
	}
		
}
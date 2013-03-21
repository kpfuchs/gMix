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


public abstract class AggregatorImplementation {
	
	/**
	 * Input: the recorded values for a distinct source, e.g. client 1 
	 * (sourceType = client; distinctClient or clientId = 1).
	 * Output: an aggregate of the recorded values, e.g. the avg value of the 
	 * bypassed values.
	 * 
	 * @param recordedValues
	 * @return
	 */
	public abstract double aggregate(TDoubleArrayList recordedValues);
	
}
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
package evaluation.traceParser.scenarioExtractor.flowFilter;

import evaluation.traceParser.engine.dataStructure.Flow;


public interface FlowFilter {

	/**
	 * returns whether the bypassed flow should be filtered/ignored (true) or 
	 * not (false).
	 * FlowFilters are used to create black or white lists.
	 */
	public boolean filterFlow(Flow flow);
	
	
	/**
	 * returns the name of the implementing flow filter. the name will be used 
	 * in the file name of extracted synthetic trace files, i.e. it should not 
	 * contain spaces etc.
	 */
	public String getName();
	
	
	/**
	 * returns the version number of the implementing flow filter. must be 
	 * incremented whenever the filter's implementation is changed (used to 
	 * distinguish between old and new traces).
	 *  
	 * the returned value will be used in the file name of extracted synthetic 
	 * trace files, i.e. it should not contain spaces etc.
	 */
	public String getVersion();
	
}

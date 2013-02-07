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

import evaluation.traceParser.engine.dataStructure.Flow;


/**
 * helper class for sorter in AbstactFlow.java
 */
public class TraceEntry implements Comparable<TraceEntry> {
	
	public long start;
	public int hostId;
	public long offset;
	
	
	public TraceEntry(String serializedFlow, long offsetInTraceFile) {
		this.offset = offsetInTraceFile;
		this.start = Long.parseLong(Flow.extractField(2, serializedFlow));
		this.hostId = Integer.parseInt(Flow.extractField(0, serializedFlow));
	}
	
	
	@Override
	public int compareTo(TraceEntry te) {
		if (this.hostId < te.hostId) {
			return -1;
		} else if (this.hostId > te.hostId) {
			return 1;
		} else { // decide by start offset
			return	(this.start == te.start ?  0 :	// Values are equal
	            	(this.start < te.start ? -1 :	// (-0.0, 0.0) or (!NaN, NaN)
	            	1));						// (0.0, -0.0) or (NaN, !NaN)
		}
	}
	
}

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

package statistics;


public enum EvaluationType {
	
	MIN("minimum", 1),
	MAX("maximum", 1),
	AVG("average", 1),
	MEDIAN("median", 1),
	SUM("sum", 2),
	EVENTS_PER_SECOND("events_per_second", 2),
	EVENTS_PER_SECOND_AND_CLIENT("events_per_second", 2),
	VOLUME_PER_SECOND("volume_per_second", 2);
	
	public String description;
	public int dataType;
	
	public static final int DATA_TYPE_DOUBLE = 1;
	public static final int DATA_TYPE_COUNTER = 2;
	public static final int DATA_TYPE_BIG_DECIMAL = 3;
	
	private EvaluationType(String description, int dataType) {
		
		this.description = description;
		this.dataType = dataType;
		
	}
	
}
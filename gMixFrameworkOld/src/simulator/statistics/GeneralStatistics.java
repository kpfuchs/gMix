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

package simulator.statistics;

import java.math.BigDecimal;
import java.util.ArrayList;

import simulator.statistics.EvaluationType;


public class GeneralStatistics {

	
	private static ArrayList<Statistics> registeredStatisticsObjects = new ArrayList<Statistics>();
	
	
	public static void reset() {
		registeredStatisticsObjects.clear();
		registeredStatisticsObjects = new ArrayList<Statistics>();
	}
	
	
	public static void registerStatisticsObject(Statistics statisticsObject) {
		registeredStatisticsObjects.add(statisticsObject);
	}

	
	public static BigDecimal getCumulativeResult(	StatisticsType st,
													EvaluationType et) {
				
			switch (et.dataType) {
		
			case EvaluationType.DATA_TYPE_DOUBLE:
				return new BigDecimal(getCumulativeResultDouble(st, et));
				
			case EvaluationType.DATA_TYPE_COUNTER:
				return getCumulativeResultBigDecimal(st, et);
				
			default:
				throw new RuntimeException("ERROR: unknown or not supported/implemented dataType! " +et.dataType +", " +st +", " +et);
		
		}
		
	}
	
	
	public static double getCumulativeResultDouble(StatisticsType statisticsType, EvaluationType evaluationType) throws NullPointerException {
		
		/*if (evaluationType.dataType != EvaluationType.DATA_TYPE_INT)
			throw new RuntimeException("ERROR: wrong data type!");
		*/
		
		Statistics cumulativeResults = new Statistics();
			
		for (Statistics s: registeredStatisticsObjects) {
				
			if (!s.containsResultsFor(statisticsType))
				continue;
			
			cumulativeResults.addValue(s.getResultDouble(statisticsType, evaluationType), statisticsType);

		}
		
		return cumulativeResults.getResultDouble(statisticsType, evaluationType);
	}
	
	
	public static BigDecimal getCumulativeResultBigDecimal(StatisticsType statisticsType, EvaluationType evaluationType) throws NullPointerException {
		
		//if (evaluationType.dataType != EvaluationType.DATA_TYPE_BIG_INTEGER)
		//	throw new RuntimeException("ERROR: wrong data type!");
		
		Statistics cumulativeResults = new Statistics();
		
		for (Statistics s: registeredStatisticsObjects) {
			
			if (!s.containsResultsFor(statisticsType))
				continue;
			
			BigDecimal result = s.getResultBigDecimal(statisticsType, evaluationType);
			
			if (result.compareTo(new BigDecimal(Double.MAX_VALUE)) > 0)
				throw new RuntimeException("ERROR: double too small... -> implement BigDecimal...");
			else 	
				cumulativeResults.addValue(result.doubleValue(), statisticsType);
			
		}
			
		return cumulativeResults.getResultBigDecimal(statisticsType, evaluationType);
		
	}
	
}

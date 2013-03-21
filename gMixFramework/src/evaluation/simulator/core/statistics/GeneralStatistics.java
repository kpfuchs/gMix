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
package evaluation.simulator.core.statistics;

import java.util.Vector;

import evaluation.simulator.core.statistics.aggregator.Aggregator;
import evaluation.simulator.pluginRegistry.StatisticsType;

import gnu.trove.TDoubleArrayList;


public class GeneralStatistics {

	private static Vector<Statistics> registeredStatisticsObjects = new Vector<Statistics>();
	
	
	public static void registerStatisticsObject(Statistics statisticsObject) {
		registeredStatisticsObjects.add(statisticsObject);
	}

	
	public static void reset() {
		registeredStatisticsObjects.clear();
		registeredStatisticsObjects = new Vector<Statistics>();
	}
	
	
	public static TDoubleArrayList getResult(StatisticsType st) {
		if (st.sourceValueAggregator == Aggregator.NONE) { // no aggregation required -> just concatenate and return values
			TDoubleArrayList result = new TDoubleArrayList(10000);
			boolean isFirst = true;
			for (Statistics s: registeredStatisticsObjects) {
				if (!s.containsData(st)) {
					continue;
				} else if (s.containsDouble(st)) {
					TDoubleArrayList list = s.getRecordedDoubleValues(st);
					for (int i=0; i<list.size(); i++)
						result.add(list.get(i));
				} else if (s.containsBigDecimal(st)) {
					result.add(s.getRecordedSum(st).doubleValue());
				} else if (s.containsBoolean(st)) {
					int[] tfctr = s.getRecordedBooleanValues(st);
					if (isFirst) {
						isFirst = false;
						result = new TDoubleArrayList(2);
						result.add((double)tfctr[0]);
						result.add((double)tfctr[1]);
					} else {
						result.set(0, result.get(0) + (double)tfctr[0]);
						result.set(1, result.get(1) + (double)tfctr[1]);
					}
				} else
					throw new RuntimeException("ERRORO: unknown data type"); 

			}
			return result;
		} else { // aggregation required
			TDoubleArrayList result = new TDoubleArrayList(10000);
			for (Statistics s: registeredStatisticsObjects) {
				if (!s.containsData(st))
					continue;
				result.add(s.getResult(st));
			}
			return result;
		}
	}
	
}

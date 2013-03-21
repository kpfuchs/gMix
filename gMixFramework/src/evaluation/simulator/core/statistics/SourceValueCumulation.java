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
package evaluation.simulator.core.statistics;

import evaluation.simulator.core.statistics.aggregator.AggregatorImplementation;
import evaluation.simulator.core.statistics.aggregator.AvgAggregator;
import evaluation.simulator.core.statistics.aggregator.BernoulliAggregator;
import evaluation.simulator.core.statistics.aggregator.MaxAggregator;
import evaluation.simulator.core.statistics.aggregator.MedianAggregator;
import evaluation.simulator.core.statistics.aggregator.MinAggregator;
import evaluation.simulator.core.statistics.aggregator.SumAggregator;


public enum SourceValueCumulation {

	MEDIAN("median", DataType.DOUBLE, new MedianAggregator()),
	MIN("minimum", DataType.DOUBLE, new MinAggregator()),
	MAX("maximum", DataType.DOUBLE, new MaxAggregator()),
	AVG("average", DataType.DOUBLE, new AvgAggregator()),
	SUM("sum", DataType.COUNTER, new SumAggregator()),
	PERCENTAGE("percentage", DataType.BOOLEAN, new BernoulliAggregator());
	
	
	public enum DataType {DOUBLE, COUNTER, BOOLEAN};

	public String description;
	public DataType dataType;
	public AggregatorImplementation svc;
	

	/**
	 * The SourceValueCumulation-EnumEntries define how the values recorded for 
	 * a distinct statistics source (e.g., a concrete client: client 1) shall 
	 * be cumulated (e.g., sum, avg, min, max).
	 * @param description
	 * @param dataType
	 * @param svc
	 */
	private SourceValueCumulation(String description, DataType dataType, AggregatorImplementation svc) {
		this.description = description;
		this.dataType = dataType;
		this.svc = svc;
	}
	
	
	

	
	// =========================================================================
	// HELPERS:
	// =========================================================================
	
	/*private static GetSumHelper getSumHelper = new GetSumHelper();
	private static DisplayAllHelper displayAllHelper = new DisplayAllHelper(false);
	private static DisplayAllHelper displayAllSeparatedHelper = new DisplayAllHelper(true);
	private static GetPercentageHelper getPercentageHelper = new GetPercentageHelper();
	*/

}

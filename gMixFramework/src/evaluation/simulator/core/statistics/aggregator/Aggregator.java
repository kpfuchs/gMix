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


public enum Aggregator {

	NONE (			InputDataType.NONE, 		null),
	AVG (			InputDataType.DOUBLE, 		new AvgAggregator()),
	MAX (			InputDataType.DOUBLE, 		new MaxAggregator()),
	MEDIAN (		InputDataType.DOUBLE, 		new MedianAggregator()),
	MIN (			InputDataType.DOUBLE, 		new MinAggregator()),
	BERNOULLI (		InputDataType.BOOLEAN, 		new BernoulliAggregator()),
	SUM (			InputDataType.BIG_DECIMAL,	new SumAggregator());
	
	
	public enum InputDataType { NONE, DOUBLE, BOOLEAN, BIG_DECIMAL };
	
	
	public AggregatorImplementation aggregator;
	public InputDataType inputDataType;
	
	
	private Aggregator(InputDataType inputDataType, AggregatorImplementation aggregator) {
		this.aggregator = aggregator;
		this.inputDataType = inputDataType;
	}
	
	
	public double aggregate(TDoubleArrayList recordedValues) {
		return this.aggregator.aggregate(recordedValues);
	}
	
	
	public InputDataType getInputDataType() {
		return this.inputDataType;
	}	
	
}

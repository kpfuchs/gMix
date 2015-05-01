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
package staticContent.evaluation.testbed.statistic;

import staticContent.evaluation.simulator.core.statistics.aggregator.Aggregator.InputDataType;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.StatisticsType;

public enum Metric {	
	TB_MIN_THROUGHPUT_SEND("THROUGHPUT_REQUEST", InputDataType.DOUBLE, StatisticsType.TB_MIN_THROUGHPUT_SEND),
	TB_MAX_THROUGHPUT_SEND("THROUGHPUT_REQUEST", InputDataType.DOUBLE, StatisticsType.TB_MAX_THROUGHPUT_SEND),
	TB_AVG_THROUGHPUT_SEND("THROUGHPUT_REQUEST", InputDataType.DOUBLE, StatisticsType.TB_AVG_THROUGHPUT_SEND),
	
	TB_MIN_THROUGHPUT_RECEIVE("THROUGHPUT_REPLY", InputDataType.DOUBLE, StatisticsType.TB_MIN_THROUGHPUT_RECEIVE),
	TB_MAX_THROUGHPUT_RECEIVE("THROUGHPUT_REPLY", InputDataType.DOUBLE, StatisticsType.TB_MAX_THROUGHPUT_RECEIVE),
	TB_AVG_THROUGHPUT_RECEIVE("THROUGHPUT_REPLY", InputDataType.DOUBLE, StatisticsType.TB_AVG_THROUGHPUT_RECEIVE),
	
	TB_MIN_THROUGHPUT_SEND_AND_RECEIVE("THROUGHPUT_REQUEST_AND_REPLY", InputDataType.DOUBLE, StatisticsType.TB_MIN_THROUGHPUT_SEND_AND_RECEIVE),
	TB_MAX_THROUGHPUT_SEND_AND_RECEIVE("THROUGHPUT_REQUEST_AND_REPLY", InputDataType.DOUBLE, StatisticsType.TB_MAX_THROUGHPUT_SEND_AND_RECEIVE),
	TB_AVG_THROUGHPUT_SEND_AND_RECEIVE("THROUGHPUT_REQUEST_AND_REPLY", InputDataType.DOUBLE, StatisticsType.TB_AVG_THROUGHPUT_SEND_AND_RECEIVE)
;
	
	public final String requiredMeasuredMetric; // sensor measured metric	
	public final InputDataType inputDataType;
	public final StatisticsType statisticsTypeReference; // defines the aggregation and plot presentation
	
	private Metric(String requiredMeasuredMetric, InputDataType inputDataType, StatisticsType statisticsTypeReference) {
		this.requiredMeasuredMetric = requiredMeasuredMetric;
		this.statisticsTypeReference = statisticsTypeReference;
		this.inputDataType = inputDataType;
	}
	
	public void activate() {
		statisticsTypeReference.isActivated = true;
	}
}

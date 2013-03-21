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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import evaluation.simulator.core.ExperimentConfig;
import evaluation.simulator.core.statistics.aggregator.Aggregator;
import evaluation.simulator.pluginRegistry.PlotType;
import evaluation.simulator.pluginRegistry.StatisticsType;
import gnu.trove.TDoubleArrayList;


public class ResultSet {

	/** [varying_value][statisticsType][validation_run]->[TDoubleArray:recorded value(s)] */
	public TDoubleArrayList[][][] results;
	public ExperimentConfig ep;
	public long simulationTime[][]; // [varying_value][validation_run]
	public int numberOfMixes[]; // [varying_value]
	public int numberOfClients[]; // [varying_value]
	
	
	
	public ResultSet(ExperimentConfig ep) {
		this.ep = ep;
		this.results = new TDoubleArrayList[ep.values.length][StatisticsType.values().length][ep.runs];
		this.simulationTime = new long[ep.values.length][ep.runs];
		this.numberOfMixes = new int[ep.values.length];
		this.numberOfClients = new int[ep.values.length];
	}
	
	
	public int getNumberOfValidationRuns() {
		return ep.runs - 1;
	}
	
	
	public int getNumberOfVaryingValues() {
		return ep.values.length;
	}
	
	
	public PlotType[] getDesiredPlotTypes() {
		return ep.desiredPlotTypes;
	}
	
	
	public String[] getDesiredPlots(PlotType plotType) {
		HashSet<String> result = new HashSet<String>();
		for (StatisticsType st: ep.desiredStatisticsTypes)
			if (st.plotType == plotType)
				result.add(st.destinationPlot);
		return result.toArray(new String[0]);
	}
	
	
	public StatisticsType[] getDesiredEvaluations(String plotName) {
		Vector<StatisticsType> result = new Vector<StatisticsType>();
		for (StatisticsType st: ep.desiredStatisticsTypes)
			if (st.destinationPlot.equals(plotName))
				result.add(st);
		return result.toArray(new StatisticsType[0]);
	}
	
	
	public boolean containsData(int varyingValueId, StatisticsType statisticsType, int runId) {
		if (this.results[varyingValueId][statisticsType.ordinal()][runId] == null || 
				this.results[varyingValueId][statisticsType.ordinal()][runId].size() == 0)
			return false;
		return true;
	}
	
	public boolean isSingleValue(int varyingValueId, StatisticsType statisticsType, int runId) {
		return this.results[varyingValueId][statisticsType.ordinal()][runId].size() == 1;
	}
	
	
	public TDoubleArrayList getResultArray(int varyingValueId, StatisticsType statisticsType, int runId) {
		return this.results[varyingValueId][statisticsType.ordinal()][runId];
	}

	
	public double getResultSingleValue(int varyingValueId, StatisticsType statisticsType, int runId) {
		return this.results[varyingValueId][statisticsType.ordinal()][runId].get(0);
	}

	
	public long getSimulationTime(int varyingValueId, int runId) {
		return this.simulationTime[varyingValueId][runId];
	}
	
	
	public int getNumberOfMixes(int varyingValueId) {
		return this.numberOfMixes[varyingValueId];
	}
	
	
	public int getNumberOfClients(int varyingValueId) {
		return this.numberOfClients[varyingValueId];
	}
	
	
	public HashMap<Aggregator, TDoubleArrayList> getResultsForAllRuns(int varyingValueId, StatisticsType statisticsType) {
		HashMap<Aggregator, TDoubleArrayList> result = new HashMap<Aggregator, TDoubleArrayList>(statisticsType.sourceAggregators.length * 2);
		int runs = this.getNumberOfValidationRuns()+1;
		for (Aggregator ag:statisticsType.sourceAggregators) {
			TDoubleArrayList resultPerAggregator = new TDoubleArrayList(runs);
			for (int i=0; i<runs; i++)
				if (this.isSingleValue(varyingValueId, statisticsType, i))
					resultPerAggregator.add(this.getResultSingleValue(varyingValueId, statisticsType, i));
				else
					resultPerAggregator.add(ag.aggregate(this.getResultArray(varyingValueId, statisticsType, i)));
			result.put(ag, resultPerAggregator);
		} 
		return result;
	}
	
}

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

import gnu.trove.TDoubleArrayList;

import java.math.BigDecimal;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.event.EventExecutor;
import evaluation.simulator.core.networkComponent.Identifiable;
import evaluation.simulator.core.statistics.aggregator.Aggregator.InputDataType;
import evaluation.simulator.pluginRegistry.StatisticsType;


public class Statistics implements EventExecutor {

	private static final int INITIAL_SIZE = 10000;

	private TDoubleArrayList[] recordedDoubleValues; // two dimensions (0:StatisticsType, 1:valueList)
	private BigDecimal[] summedUpValues; // one dimension (0:StatisticsType)
	private int[][] recordedBooleanValues; // three dimensions ([StatisticsType] [0:trueCtr, 1:falseCtr])
	
	//private Identifiable owner;
	private static boolean recordStatistics = false;
	private static Simulator simulator;
	
	
	public Statistics(Identifiable owner) { // is "owner" still needed?
		//this.owner = owner;
		GeneralStatistics.registerStatisticsObject(this);
		recordedDoubleValues = new TDoubleArrayList[StatisticsType.values().length];
		summedUpValues = new BigDecimal[StatisticsType.values().length];
		recordedBooleanValues = new int[StatisticsType.values().length][];
	}
	
	
	// dummy constructor used for scheduling start and end of statistics recording (see below)
	private Statistics(int i) {}
	
	
	public static void setSimulator(Simulator simulator) {
		Statistics.simulator = simulator;
		simulator.ts_recordStatisticsStart = Simulator.settings.getPropertyAsInt("START_RECORDING_STATISTICS_AT");
		Statistics s = new Statistics(0);
		// set start
		if (simulator.ts_recordStatisticsStart == 0) {
			recordStatistics = true;
		} else {
			simulator.scheduleEvent(new Event(s, simulator.ts_recordStatisticsStart, StatisticsEvent.START_RECORDING), s);
		}
		// set fixed end (if specified)
		if (Simulator.settings.getProperty("SIMULATION_END").equals("SIMULATION_TIME_END")) {
			int recordStatisticsEnd = Simulator.settings.getPropertyAsInt("SIMULATION_TIME_LIMIT_IN_MS");
			simulator.scheduleEvent(new Event(s, recordStatisticsEnd, StatisticsEvent.STOP_RECORDING), s);
		}
	}
	
	
	public void increment(double value, StatisticsType statisticsType) {
		if (!statisticsType.isActivated || !recordStatistics)
			return;
		checkIfDataTypeAllowed(statisticsType, InputDataType.BIG_DECIMAL);
		if (summedUpValues[statisticsType.ordinal()] == null) // first call
			summedUpValues[statisticsType.ordinal()] = new BigDecimal(0);
		summedUpValues[statisticsType.ordinal()] = summedUpValues[statisticsType.ordinal()].add(new BigDecimal(value));
	}
	
	
	public void addValue(boolean value, StatisticsType statisticsType) {
		if (!statisticsType.isActivated || !recordStatistics)
			return;
		checkIfDataTypeAllowed(statisticsType, InputDataType.BOOLEAN);
		if (recordedBooleanValues[statisticsType.ordinal()] == null)
			recordedBooleanValues[statisticsType.ordinal()] = new int[2];
		if (value)
			recordedBooleanValues[statisticsType.ordinal()][0]++;
		else
			recordedBooleanValues[statisticsType.ordinal()][1]++;
	}

	
	public void addValue(double value, StatisticsType statisticsType) {
		if (!statisticsType.isActivated || !recordStatistics)
			return;
		checkIfDataTypeAllowed(statisticsType, InputDataType.DOUBLE);
		if (recordedDoubleValues[statisticsType.ordinal()] == null)
			recordedDoubleValues[statisticsType.ordinal()] = new TDoubleArrayList(INITIAL_SIZE);
		recordedDoubleValues[statisticsType.ordinal()].add(value);
	}
	
	
	public TDoubleArrayList getRecordedDoubleValues(StatisticsType statisticsType) throws NullPointerException {
		checkIfDataAvailable(statisticsType, InputDataType.DOUBLE);
		return recordedDoubleValues[statisticsType.ordinal()];
	}
	
	
	public int[] getRecordedBooleanValues(StatisticsType statisticsType) throws NullPointerException {
		checkIfDataAvailable(statisticsType, InputDataType.BOOLEAN);
		return recordedBooleanValues[statisticsType.ordinal()];
	}
	
	
	public BigDecimal getRecordedSum(StatisticsType statisticsType) throws NullPointerException {
		checkIfDataAvailable(statisticsType, InputDataType.BIG_DECIMAL);
		return summedUpValues[statisticsType.ordinal()];
	}
	

	public double getResult(StatisticsType statisticsType) throws NullPointerException {
		if (containsDouble(statisticsType)) {
			return statisticsType.sourceValueAggregator.aggregate(recordedDoubleValues[statisticsType.ordinal()]);
		} else if (containsBigDecimal(statisticsType)) {
			return summedUpValues[statisticsType.ordinal()].doubleValue();
		} else if (containsBoolean(statisticsType)) {
			TDoubleArrayList input = new TDoubleArrayList(2); // boolean aggregator needs special input format
			input.add(recordedBooleanValues[statisticsType.ordinal()][0]);
			input.add(recordedBooleanValues[statisticsType.ordinal()][1]);
			return statisticsType.sourceValueAggregator.aggregate(input);
		} else
			throw new RuntimeException("ERROR: no results recorded for " +statisticsType +". Use containsData(StatisticsType) before calling this method.");
	}
	
	
	public boolean containsData(StatisticsType statisticsType) {
		return containsBoolean(statisticsType) || containsBigDecimal(statisticsType) || containsDouble(statisticsType);
	}
	
	
	public boolean containsData(StatisticsType statisticsType, InputDataType inputDataType) {
		if (inputDataType == InputDataType.BOOLEAN)
			return containsBoolean(statisticsType);
		else if (inputDataType == InputDataType.DOUBLE)
			return containsDouble(statisticsType);
		else if (inputDataType == InputDataType.BIG_DECIMAL)
			return containsBigDecimal(statisticsType);
		else
			throw new RuntimeException("ERROR: unknown InputDataType: " +inputDataType); 
	}
	
	
	public boolean containsBoolean(StatisticsType statisticsType) {
		return recordedBooleanValues[statisticsType.ordinal()] != null;
	}
	
	
	public boolean containsBigDecimal(StatisticsType statisticsType) {
		return summedUpValues[statisticsType.ordinal()] != null;
	}
	
	
	public boolean containsDouble(StatisticsType statisticsType) {
		return recordedDoubleValues[statisticsType.ordinal()] != null;
	}


	private void checkIfDataTypeAllowed(StatisticsType statisticsType, InputDataType inputDataType) {
		InputDataType typeAgg1 = statisticsType.sourceValueAggregator.inputDataType; 
		InputDataType typeAgg2 = statisticsType.sourceAggregators[0].inputDataType;
		if (typeAgg1 == inputDataType)
			return;
		if (typeAgg1 == InputDataType.NONE)
			if (typeAgg2 == InputDataType.NONE || typeAgg2 == inputDataType)
				return;
		throw new RuntimeException("ERROR: wrong data type for " +statisticsType 
				+". " +statisticsType +" supports " +inputDataType +" only. " +
				"Use increment(double, StatisticsType) for BIG_DECIMAL, " +
				"addValue(boolean, StatisticsType) for BOOLEAN and " +
				"addValue(double, StatisticsType) for DOUBLE.");
	}
	
	
	private void checkIfDataAvailable(StatisticsType statisticsType, InputDataType inputDataType) {
		if (!containsData(statisticsType, inputDataType))
			throw new RuntimeException("ERROR: no " +inputDataType +" data recorded for StatisticsType " +statisticsType +". Use containsData(StatisticsType, InputDataType) to check if data is available."); 
		InputDataType typeAgg1 = statisticsType.sourceValueAggregator.inputDataType; 
		InputDataType typeAgg2 = statisticsType.sourceAggregators[0].inputDataType;
		if (typeAgg1 == inputDataType)
			return;
		if (typeAgg1 == InputDataType.NONE)
			if (typeAgg2 == InputDataType.NONE || typeAgg2 == inputDataType)
				return;
		throw new RuntimeException("ERROR: This statistics object contains " +
				"data of another data-type! Use containsBoolean(StatisticsType), " +
				"containsBigDecimal(StatisticsType) and " +
				"containsDouble(StatisticsType) to determine the right data type " +
				"before requesting data from this object.");
	}
	
	
	@Override
	public void executeEvent(Event event) {
		
		if (event.getEventType() == StatisticsEvent.START_RECORDING) {
			recordStatistics = true;
			System.out.println("### START recording statistics"); 
		} else if (event.getEventType() == StatisticsEvent.STOP_RECORDING) {
			System.out.println("### STOP recording statistics"); 
			recordStatistics = false; 
			simulator.stopSimulation("simulation-time limit reached (variable SIMULATION_TIME_LIMIT_IN_MS in experiment config)");
		} else {
			throw new RuntimeException("ERROR! received unsupported event!" +event);
		}
		
	}
	
}

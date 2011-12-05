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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleProcedure;

import java.math.BigDecimal;

import simulator.Event;
import simulator.EventExecutor;
import simulator.Settings;
import simulator.Simulator;

import networkComponent.Identifiable;


public class Statistics implements EventExecutor {

	private static final int INITIAL_SIZE = 10000;
	private static GetSumHelper getSumHelper = new GetSumHelper();
	
	private boolean isCumulativeStatisticsObject = false; // statistics object used to calculate EvaluationTypes (e.g. average or minimum) from recorded statistics 
	
	private TDoubleArrayList[] recordedValues;
	private BigDecimal[] summedUpValues;
	//private Identifiable owner;
	private static boolean recordStatistics = false;
	private static Simulator simulator;
	
	
	public Statistics(Identifiable owner) { // TODO: wird "owner" noch für irgendetwas benötigt?
		//this.owner = owner;
		GeneralStatistics.registerStatisticsObject(this);
		recordedValues = new TDoubleArrayList[StatisticsType.values().length];
		summedUpValues = new BigDecimal[StatisticsType.values().length];
	}
	
	
	// constructor for class GeneralStatistics (selbstreferenzen vermeiden...)
	protected Statistics() {
		isCumulativeStatisticsObject = true;
		recordedValues = new TDoubleArrayList[StatisticsType.values().length];
		summedUpValues = new BigDecimal[StatisticsType.values().length];
	}
	
	
	private Statistics(int i) {}
	
	
	public static void setSimulator(Simulator simulator) {
		
		Statistics.simulator = simulator;
		int recordStatisticsFrom = new Integer(Settings.getProperty("RECORD_STATISTICS_FROM"));
		int recordStatisticsTo = new Integer(Settings.getProperty("RECORD_STATISTICS_TO"));
		Statistics s = new Statistics(0);
		
		if (recordStatisticsFrom == 0)
			recordStatistics = true;
		else
			simulator.scheduleEvent(new Event(s, recordStatisticsFrom, StatisticsEvent.START_RECORDING), s);
		
		simulator.scheduleEvent(new Event(s, recordStatisticsTo, StatisticsEvent.STOP_RECORDING), s);
	
	}
	
	
	public void addValue(double value, StatisticsType statisticsType) {
		
		if (!statisticsType.isActivated || (!recordStatistics && !isCumulativeStatisticsObject))
			return;
		
		if (statisticsType.intendedEvaluations.length == 1 && (statisticsType.intendedEvaluations[0] == EvaluationType.SUM || statisticsType.intendedEvaluations[0] == EvaluationType.EVENTS_PER_SECOND || statisticsType.intendedEvaluations[0] == EvaluationType.VOLUME_PER_SECOND || statisticsType.intendedEvaluations[0] == EvaluationType.EVENTS_PER_SECOND_AND_CLIENT)) {
			
			if (summedUpValues[statisticsType.ordinal()] == null)
				summedUpValues[statisticsType.ordinal()] = new BigDecimal(0);
			summedUpValues[statisticsType.ordinal()] = summedUpValues[statisticsType.ordinal()].add(new BigDecimal(value));

		} else {

			if (recordedValues[statisticsType.ordinal()] == null)
				recordedValues[statisticsType.ordinal()] = new TDoubleArrayList(INITIAL_SIZE);
			recordedValues[statisticsType.ordinal()].add(value);
			
		}
		
	}

	
	public boolean containsResultsFor(StatisticsType statisticsType) {
		
		if (statisticsType.intendedEvaluations.length == 1 && (statisticsType.intendedEvaluations[0] == EvaluationType.SUM || statisticsType.intendedEvaluations[0] == EvaluationType.EVENTS_PER_SECOND || statisticsType.intendedEvaluations[0] == EvaluationType.EVENTS_PER_SECOND || statisticsType.intendedEvaluations[0] == EvaluationType.VOLUME_PER_SECOND || statisticsType.intendedEvaluations[0] == EvaluationType.EVENTS_PER_SECOND_AND_CLIENT)) 
			return summedUpValues[statisticsType.ordinal()] != null;
		else
			return recordedValues[statisticsType.ordinal()] != null;
		
	}
	
	
	public double getResultDouble(	StatisticsType statisticsType, 
									EvaluationType evaluationType
									) throws NullPointerException {
		
		
		if (evaluationType.dataType != EvaluationType.DATA_TYPE_DOUBLE)
			throw new RuntimeException("ERROR: wrong data type!");
		
		try {
			
			switch (evaluationType) {
			
				case MIN:
					return recordedValues[statisticsType.ordinal()].min();
		
				case MAX:
					return recordedValues[statisticsType.ordinal()].max();
			
				case AVG:
					BigDecimal sum = getSumHelper.getSum(recordedValues[statisticsType.ordinal()]);
					BigDecimal avg = sum.divide(new BigDecimal(recordedValues[statisticsType.ordinal()].size()), 5, BigDecimal.ROUND_HALF_UP);
					return avg.doubleValue();
				
				case MEDIAN:
					recordedValues[statisticsType.ordinal()].sort();
					int mid = (int)Math.round((double)recordedValues[statisticsType.ordinal()].size() / 2d);
					if (mid == 0)
						throw new RuntimeException("ERROR: no data!");
					else
						return recordedValues[statisticsType.ordinal()].get(mid-1);
				
				default:
					throw new RuntimeException("ERROR: unknown or not allowed evaluation type!");
				
			}
			
		} catch (IllegalStateException e) {
			
			e.printStackTrace();
			throw new RuntimeException("ERROR: no data!");
			
		}
		
		
	}

	
	public BigDecimal getResultBigDecimal(	StatisticsType statisticsType, 
											EvaluationType evaluationType 
											) throws NullPointerException {
		
		if (evaluationType.dataType != EvaluationType.DATA_TYPE_COUNTER)
			throw new RuntimeException("ERROR: wrong data type!");
		
		try {
			
			switch (evaluationType) {
			
				case SUM:
					return summedUpValues[statisticsType.ordinal()];

				case EVENTS_PER_SECOND:
					if (isCumulativeStatisticsObject) {
						int recordStatisticsFrom = new Integer(Settings.getProperty("RECORD_STATISTICS_FROM"));
						int recordStatisticsTo = new Integer(Settings.getProperty("RECORD_STATISTICS_TO"));
						BigDecimal numberOfEvents = summedUpValues[statisticsType.ordinal()];
						BigDecimal simulationTime = new BigDecimal(recordStatisticsTo - recordStatisticsFrom);
						return numberOfEvents.divide(simulationTime.divide(new BigDecimal(1000), 5, BigDecimal.ROUND_HALF_UP), 5, BigDecimal.ROUND_HALF_UP);
					} else 
						return summedUpValues[statisticsType.ordinal()];
				
				case EVENTS_PER_SECOND_AND_CLIENT:
					if (isCumulativeStatisticsObject) {
						int recordStatisticsFrom = new Integer(Settings.getProperty("RECORD_STATISTICS_FROM"));
						int recordStatisticsTo = new Integer(Settings.getProperty("RECORD_STATISTICS_TO"));
						BigDecimal numberOfEvents = summedUpValues[statisticsType.ordinal()];
						BigDecimal simulationTime = new BigDecimal(recordStatisticsTo - recordStatisticsFrom);
						return (numberOfEvents.divide(simulationTime.divide(new BigDecimal(1000), 5, BigDecimal.ROUND_HALF_UP), 5, BigDecimal.ROUND_HALF_UP)).divide(new BigDecimal(simulator.getClients().size()), 5, BigDecimal.ROUND_HALF_UP);
					} else 
						return summedUpValues[statisticsType.ordinal()];
					
				case VOLUME_PER_SECOND:
					
					if (isCumulativeStatisticsObject) {
						int recordStatisticsFrom = new Integer(Settings.getProperty("RECORD_STATISTICS_FROM"));
						int recordStatisticsTo = new Integer(Settings.getProperty("RECORD_STATISTICS_TO"));
						BigDecimal byteCounter = summedUpValues[statisticsType.ordinal()];
						BigDecimal simulationTime = new BigDecimal(recordStatisticsTo - recordStatisticsFrom);
						return byteCounter.divide(simulationTime, 5, BigDecimal.ROUND_HALF_UP).divide(new BigDecimal(simulator.getClients().size()), 5, BigDecimal.ROUND_HALF_UP);
					} else
						return summedUpValues[statisticsType.ordinal()];
					
				default:
					throw new RuntimeException("ERROR: unknown evaluation type!");
				
			}
			
		} catch (IllegalStateException e) {
			
			e.printStackTrace();
			throw new RuntimeException("ERROR: no data!");
			
		}
		
	}
	
	
	static class GetSumHelper implements TDoubleProcedure {
		
		BigDecimal sum = new BigDecimal(0);
		
		
		public BigDecimal getSum(TDoubleArrayList data) {
			data.forEach(getSumHelper);
			BigDecimal result = sum;
			sum = new BigDecimal("0");
			return result;
		}
		
		
		@Override
		public boolean execute(double val) {
			sum = sum.add(new BigDecimal(""+val));
			return true;
		}
		
	}


	@Override
	public void executeEvent(Event event) {
		
		if (event.getEventType() == StatisticsEvent.START_RECORDING) {
			recordStatistics = true;
			System.out.println("### START recording statistics"); 
		} else if (event.getEventType() == StatisticsEvent.STOP_RECORDING) {
			System.out.println("### STOP recording statistics"); 
			recordStatistics = false; 
			if (Settings.getProperty("FORCE_QUIT").equals("TURE"))
				simulator.getEventQueue().clear();
		} else {
			throw new RuntimeException("ERROR! received unsupported event!" +event);
		}
		
	}
	
}

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
package userGeneratedContent.simulatorPlugIns.pluginRegistry;

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.core.networkComponent.Mix;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.BasicSynchronousBatch;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.Batch;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.BatchWithTimeout;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.BinomialPool;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.CottrellPool;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.CottrellRandomDelay;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.CottrellTimedPool;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.DLPABasic;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.DLPAHeuristic;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.DLPAHeuristic2;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.DistinctUserBatch;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.LossySynchronousBatch;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.NoDelay;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.OutputStrategyImpl;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.StopAndGo;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.ThresholdAndTimedBatch;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.ThresholdOrTimedBatch;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.ThresholdPool;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.TimedBatch;
import userGeneratedContent.simulatorPlugIns.plugins.outputStrategy.TimedDynamicPool;


public enum OutputStrategy {

	NO_DELAY,
	BASIC_SYNCHRONOUS_BATCH,
	BASIC_BATCH,
	BATCH_WITH_TIMEOUT,
	BINOMIAL_POOL,
	COTTRELL_POOL,
	COTTRELL_RANDOM_DELAY,
	COTTRELL_TIMED_POOL,
	DISTINCT_USER_BATCH,
	DLPA_BASIC,
	DLPA_HEURISTIC,
	DLPA_HEURISTIC_2,
	LOSSY_SYNCHRONOUS_BATCH,
	STOP_AND_GO,
	THRESHOLD_AND_TIMED_BATCH,
	THRESHOLD_OR_TIMED_BATCH,
	THRESHOLD_POOL,
	TIMED_BATCH,
	TIMED_DYNAMIC_POOL
	;
	
	
	public static OutputStrategyImpl getInstance(Mix owner) {
		String desiredImpl = Simulator.settings.getProperty("OUTPUT_STRATEGY");
		if (desiredImpl.equals("NO_DELAY")) {
			return new NoDelay(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("BASIC_SYNCHRONOUS_BATCH")) {
			return new BasicSynchronousBatch(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("BASIC_BATCH")) {
			return new Batch(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("BATCH_WITH_TIMEOUT")) {
			return new BatchWithTimeout(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("BINOMIAL_POOL")) {
			return new BinomialPool(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("COTTRELL_POOL")) {
			return new CottrellPool(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("COTTRELL_RANDOM_DELAY")) {
			return new CottrellRandomDelay(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("COTTRELL_TIMED_POOL")) {
			return new CottrellTimedPool(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("DISTINCT_USER_BATCH")) {
			return new DistinctUserBatch(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("DLPA_BASIC")) {
			return new DLPABasic(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("DLPA_HEURISTIC")) {
			return new DLPAHeuristic(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("DLPA_HEURISTIC_2")) {
			return new DLPAHeuristic2(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("LOSSY_SYNCHRONOUS_BATCH")) {
			return new LossySynchronousBatch(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("STOP_AND_GO")) {
			return new StopAndGo(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("THRESHOLD_AND_TIMED_BATCH")) {
			return new ThresholdAndTimedBatch(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("THRESHOLD_OR_TIMED_BATCH")) {
			return new ThresholdOrTimedBatch(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("THRESHOLD_POOL")) {
			return new ThresholdPool(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("TIMED_BATCH")) {
			return new TimedBatch(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("TIMED_DYNAMIC_POOL")) {
			return new TimedDynamicPool(owner, Simulator.getSimulator());
		} else
			throw new RuntimeException("ERROR: no OutputStrategy with the name \"" +desiredImpl  
				+"\" available (Key \"OUTPUT_STRATEGY\" in experiment config file. See " +
				"\"OutputStrategy.java\" for available OutputStrategies.");
	}
	
}

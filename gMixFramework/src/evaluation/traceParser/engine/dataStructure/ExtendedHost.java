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
package evaluation.traceParser.engine.dataStructure;

import java.io.IOException;
import java.util.Vector;

import evaluation.traceParser.engine.Protocol;
import evaluation.traceParser.engine.fileReader.FlowGroupFlowIterator;
import evaluation.traceParser.engine.fileReader.FlowIterator;
import evaluation.traceParser.scenarioExtractor.flowFilter.FlowFilter;
import evaluation.traceParser.statistics.calculator.Stat;
import framework.core.util.Util;


public class ExtendedHost extends Host {

	private static final long serialVersionUID = -890714043677144719L;
	
	private transient boolean statCalculationDone = false;
	
	
	public ExtendedHost(FlowFilter filter) {
		super(filter);
	}
	
	
	/**
	 * use this method to calculate and store some statistic values about this host.
	 * after the calculation, the flowGroups data structure will be cleared.
	 * this method is supposed to be used to gather statistics of large trace 
	 * files that do not fit into ram (while iterating the trace).
	 * flowGroups data structure will be no longer available after this call.
	 * use recreateFlowGroups() to load the flow groups from the trace file again 
	 * at a later point in time 
	 */
	public void calculateStatistics() {
		if (statCalculationDone)
			return;
		this.statCalculationDone = true;
		calculateStatistics(this);
		this.flowGroups.clear();
	}
	
	
	public void calculateStatistics(FlowIterator source) {
		if (statCalculationDone)
			return;
		this.statCalculationDone = true;
		calculateStatistics(source, this);
		this.flowGroups.clear();
	}
	
	
	public static void calculateStatistics(Host h) {
		if (h.flowGroups == null || h.flowGroups.size() == 0)
			throw new RuntimeException("use loadFlowGroups()");
		calculateStatistics(h.getFlowIterator(), h);
	}
	
	
	public static void calculateStatistics(FlowIterator source, Host h) {
		try {
			h.stat_numberOfFlows = 0;
			h.stat_avgUserThinkTime = Util.NOT_SET;
			h.stat_minUserThinkTime = Util.NOT_SET;
			h.stat_maxUserThinkTime = Util.NOT_SET;
			h.stat_protocolDistributionPerFlow = new int[Protocol.values().length];
			
			Stat userThinkTimePerFlowGroup = new Stat();
			Vector<Integer> userThinkTimes = new Vector<Integer>();
			Stat durationPerFlowGroup = new Stat();
			Stat flowsPerFlowGroup = new Stat();
			
			Stat requestBytesPerFlow = new Stat();
			Stat replyBytesPerFlow = new Stat();
			Stat durationPerFlow = new Stat();
			
			long firstActivity = Long.MAX_VALUE;
			long latestActivity = Long.MIN_VALUE;
			h.stat_numberOfFlowGroups = 0;
			long endOfLastFlowGroup = 0;
			while (source.hasNextFlow() && source.peekNextFlow().senderId == h.hostId) { // for each flow group
				h.stat_numberOfFlowGroups++;
				long fg_numberOfFlows = 0;
				long fg_start = Long.MIN_VALUE;
				long fg_end = Long.MAX_VALUE;
				//long fg_userThinkTime = 0;
				fg_start = source.peekNextFlow().startOfFlow;
				fg_end = source.peekNextFlow().endOfFlow;
				FlowGroupFlowIterator flows = source.getFlowGroupFlowIterator();
				Flow flow;
				while (flows.hasNext()) { // for each flow of the current flow group
					flow = flows.next();
					fg_numberOfFlows++;
					if (flow.endOfFlow > fg_end)
						fg_end = flow.endOfFlow;
					requestBytesPerFlow.addValue(flow.requestSize);
					replyBytesPerFlow.addValue(flow.replySize);
					durationPerFlow.addValue(flow.endOfFlow - flow.startOfFlow);
					assert (flow.endOfFlow - flow.startOfFlow) >= 0;
					h.stat_protocolDistributionPerFlow[flow.layer4protocol.ordinal()]++;
				} 
				if (h.stat_numberOfFlowGroups == 1) { // first flow group
					firstActivity = fg_start;
					latestActivity = fg_end;
					endOfLastFlowGroup = fg_end;
				} else {
					if (fg_end > latestActivity)
						latestActivity = fg_end;
					userThinkTimePerFlowGroup.addValue(fg_start - endOfLastFlowGroup);
					userThinkTimes.add((int) (fg_start - endOfLastFlowGroup));
					endOfLastFlowGroup = fg_end;
				}
				durationPerFlowGroup.addValue(fg_end - fg_start);
				flowsPerFlowGroup.addValue(fg_numberOfFlows);
				assert (fg_end - fg_start) >= 0;
				h.stat_numberOfFlows += fg_numberOfFlows;
			}

			// aggregate results:
			h.stat_onlineTime = (int)(latestActivity - firstActivity);
			h.firstAction = firstActivity;
			h.lastAction = latestActivity;
			h.stat_requestBytesTransferred = Math.round(requestBytesPerFlow.getSum());
			h.stat_replyBytesTransferred = Math.round(replyBytesPerFlow.getSum());
			
			if (h.stat_onlineTime > 0) { // a host that sent only one packet will have a online time of 0 ms as we need at least two packets for an interval -> ignore those hosts (-> otherwise results will be strongly biased by those hosts -> "infinity") 
				h.stat_avgRequestBytesPerSec = requestBytesPerFlow.getAvg() / ((double)h.stat_onlineTime/1000d);
				h.stat_avgReplyBytesPerSec = replyBytesPerFlow.getAvg() / ((double)h.stat_onlineTime/1000d);
				h.stat_avgNewFlowsPerSec = (double)h.stat_numberOfFlows / ((double)h.stat_onlineTime/1000d);
			}
			
			h.stat_avgFlowsPerFlowGroup = flowsPerFlowGroup.getAvg();
			h.stat_minFlowsPerFlowGroup = (int) flowsPerFlowGroup.getMin();
			h.stat_maxFlowsPerFlowGroup = (int) flowsPerFlowGroup.getMax();
			
			h.stat_avgRequestBytesPerFlow = (int) Math.round(requestBytesPerFlow.getAvg());
			h.stat_minRequestBytesPerFlow = (int) requestBytesPerFlow.getMin();
			h.stat_maxRequestBytesPerFlow = (int) requestBytesPerFlow.getMax();
			
			h.stat_avgReplyBytesPerFlow = (int) Math.round(replyBytesPerFlow.getAvg());
			h.stat_minReplyBytesPerFlow = (int) replyBytesPerFlow.getMin();
			h.stat_maxReplyBytesPerFlow = (int) replyBytesPerFlow.getMax();
			
			if (h.stat_numberOfFlowGroups > 1) {
				h.stat_avgUserThinkTime = (int) Math.round(userThinkTimePerFlowGroup.getAvg());
				h.stat_minUserThinkTime = (int) Math.round(userThinkTimePerFlowGroup.getMin());
				h.stat_maxUserThinkTime = (int) Math.round(userThinkTimePerFlowGroup.getMax());
				h.stat_userThinkTimes = Util.toIntArray(userThinkTimes);
			}
			
			h.stat_avgFlowDuration = (int) Math.round(durationPerFlow.getAvg());
			h.stat_minFlowDuration = (int) Math.round(durationPerFlow.getMin());
			h.stat_maxFlowDuration = (int) Math.round(durationPerFlow.getMax());
			
			h.stat_avgFlowGroupDuration = (int) Math.round(durationPerFlowGroup.getAvg());
			h.stat_minFlowGroupDuration = (int) Math.round(durationPerFlowGroup.getMin());
			h.stat_maxFlowGroupDuration = (int) Math.round(durationPerFlowGroup.getMax());
			
			//h.hostId = h.flowGroups.get(0).senderId;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not read " +source); 
		} 
	}
	
	
/*	public static void calculateStatistics(Host h) {
		h.stat_numberOfFlows = 0;
		h.stat_avgUserThinkTime = Util.NOT_SET;
		h.stat_minUserThinkTime = Util.NOT_SET;
		h.stat_maxUserThinkTime = Util.NOT_SET;
		h.stat_protocolDistributionPerFlow = new int[Protocol.values().length];
		
		Stat userThinkTimePerFlowGroup = new Stat();
		Stat durationPerFlowGroup = new Stat();
		Stat flowsPerFlowGroup = new Stat();
		
		Stat requestBytesPerFlow = new Stat();
		Stat replyBytesPerFlow = new Stat();
		Stat durationPerFlow = new Stat();
		
		h.stat_numberOfFlowGroups = h.flowGroups.size();
		long firstActivity = 0;
		long latestActivity = 0;
		for (int i=0; i<h.flowGroups.size(); i++) { // for each flow group
			FlowGroup flowGroup = h.flowGroups.get(i);
			if (i == 0) {
				firstActivity = flowGroup.start;
				latestActivity = flowGroup.end;
			} else {
				if (flowGroup.end > latestActivity)
					latestActivity = flowGroup.end;
				userThinkTimePerFlowGroup.addValue(flowGroup.startDelay);
			}
			durationPerFlowGroup.addValue(flowGroup.end - flowGroup.start);
			flowsPerFlowGroup.addValue(flowGroup.flows.size());
			assert (flowGroup.end - flowGroup.start) >= 0;
			h.stat_numberOfFlows += flowGroup.flows.size();
			for (Flow flow: flowGroup.flows) { // for each flow of the current flow group
				requestBytesPerFlow.addValue(flow.requestSize);
				replyBytesPerFlow.addValue(flow.replySize);
				durationPerFlow.addValue(flow.endOfFlow - flow.startOfFlow);
				assert (flow.endOfFlow - flow.startOfFlow) >= 0;
				h.stat_protocolDistributionPerFlow[flow.layer4protocol.ordinal()]++;
			}
		} 
		
		// aggregate results:
		h.stat_onlineTime = (int)(latestActivity - firstActivity);
		h.firstAction = firstActivity;
		h.lastAction = latestActivity;
		h.stat_requestBytesTransferred = Math.round(requestBytesPerFlow.getSum());
		h.stat_replyBytesTransferred = Math.round(replyBytesPerFlow.getSum());
		
		if (h.stat_onlineTime > 0) { // a host that sent only one packet will have a online time of 0 ms as we need at least two packets for an interval -> ignore those hosts (-> otherwise results will be strongly biased by those hosts -> "infinity") 
			h.stat_avgRequestBytesPerSec = requestBytesPerFlow.getAvg() / ((double)h.stat_onlineTime/1000d);
			h.stat_avgReplyBytesPerSec = replyBytesPerFlow.getAvg() / ((double)h.stat_onlineTime/1000d);
			h.stat_avgNewFlowsPerSec = (double)h.stat_numberOfFlows / ((double)h.stat_onlineTime/1000d);
		}
		
		h.stat_avgFlowsPerFlowGroup = flowsPerFlowGroup.getAvg();
		h.stat_minFlowsPerFlowGroup = (int) flowsPerFlowGroup.getMin();
		h.stat_maxFlowsPerFlowGroup = (int) flowsPerFlowGroup.getMax();
		
		h.stat_avgRequestBytesPerFlow = (int) Math.round(requestBytesPerFlow.getAvg());
		h.stat_minRequestBytesPerFlow = (int) requestBytesPerFlow.getMin();
		h.stat_maxRequestBytesPerFlow = (int) requestBytesPerFlow.getMax();
		
		h.stat_avgReplyBytesPerFlow = (int) Math.round(replyBytesPerFlow.getAvg());
		h.stat_minReplyBytesPerFlow = (int) replyBytesPerFlow.getMin();
		h.stat_maxReplyBytesPerFlow = (int) replyBytesPerFlow.getMax();
		
		if (h.flowGroups.size() > 1) {
			h.stat_avgUserThinkTime = (int) Math.round(userThinkTimePerFlowGroup.getAvg());
			h.stat_minUserThinkTime = (int) Math.round(userThinkTimePerFlowGroup.getMin());
			h.stat_maxUserThinkTime = (int) Math.round(userThinkTimePerFlowGroup.getMax());
			h.stat_userThinkTimes = new int[h.flowGroups.size()-1];
			for (int i=0; i<h.stat_userThinkTimes.length; i++)
				h.stat_userThinkTimes[i] = (int)h.flowGroups.get(i+1).startDelay;
		}
		
		h.stat_avgFlowDuration = (int) Math.round(durationPerFlow.getAvg());
		h.stat_minFlowDuration = (int) Math.round(durationPerFlow.getMin());
		h.stat_maxFlowDuration = (int) Math.round(durationPerFlow.getMax());
		
		h.stat_avgFlowGroupDuration = (int) Math.round(durationPerFlowGroup.getAvg());
		h.stat_minFlowGroupDuration = (int) Math.round(durationPerFlowGroup.getMin());
		h.stat_maxFlowGroupDuration = (int) Math.round(durationPerFlowGroup.getMax());
		
		h.hostId = h.flowGroups.get(0).senderId;
	}*/
	

}

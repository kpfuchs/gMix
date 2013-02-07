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
package evaluation.traceParser.statistics;

import evaluation.traceParser.engine.Protocol;
import evaluation.traceParser.engine.dataStructure.Host;
import evaluation.traceParser.scenarioExtractor.flowFilter.FlowFilter;
import framework.core.util.Util;


public class GeneralHostStatistics {

	private Host[] hosts;
	private HostStatistics hostStat;
	
	
	public GeneralHostStatistics(String pathToTraceFolder, FlowFilter filter) {
		this.hosts = Host.getHostIndex(pathToTraceFolder, filter);
		this.hostStat = new HostStatistics(hosts);
		displayStatistics();
	}
	
	
	private void displayStatistics() {
		
		System.out.println("\nGLOBAL STATISTICS: "); 
		double totalSeconds = (double)hostStat.total_duration / 1000d;
		System.out.println("TOTAL: file covers " +totalSeconds +" seconds " +"(start ts: " +hostStat.total_start +", end ts: " +hostStat.total_end +")");
		System.out.println("TOTAL: total hosts: " +hostStat.total_hosts);
		System.out.println("TOTAL: total flows: " +hostStat.total_flows +" (avg new flows/sec: " +(hostStat.total_flows / totalSeconds) +", "  +hostStat.total_flowGroups +" flow groups -> " +hostStat.total_flowsPerFlowGroup +" flows per flow group)");
		System.out.println("TOTAL: total data transferred: " +Util.humanReadableByteCount(hostStat.total_bytes, false) +" (" +Util.humanReadableByteCount(hostStat.total_requestBytes, false) + " request data and " +Util.humanReadableByteCount(hostStat.total_replyBytes, false) +" reply data)");
		System.out.println("TOTAL: total avg data/sec: " +Util.humanReadableByteCount((hostStat.total_bytes)/(long)totalSeconds, false) +" (" +Util.humanReadableByteCount((hostStat.total_requestBytes)/(long)totalSeconds, false) +" request, " +Util.humanReadableByteCount((hostStat.total_replyBytes)/(long)totalSeconds, false) +" reply)");
		System.out.println("TOTAL: protocol distribution:");
		for (int i=0; i<hostStat.total_protocolDistribution.length; i++)
			if (hostStat.total_protocolDistribution[i] != 0)
				System.out.println("TOTAL:     " +hostStat.total_protocolDistribution[i] +"x\t" +Protocol.getProtocol(i)); 
		
		System.out.println("\nHOST STATISTICS: "); 
		System.out.println("PER_HOST: avg data/sec of hosts: " +hostStat.perHost_avgBytesPerSec.getAvgHumanReadableByte() +" (min: " +hostStat.perHost_avgBytesPerSec.getMinHumanReadableByte() + ", max: " +hostStat.perHost_avgBytesPerSec.getMaxHumanReadableByte() +")"); 
		System.out.println("PER_HOST: \trequest only: " +hostStat.perHost_avgRequestBytesPerSec.getAvgHumanReadableByte() +" (min avg: " +hostStat.perHost_avgRequestBytesPerSec.getMinHumanReadableByte() + ", max avg: " +hostStat.perHost_avgRequestBytesPerSec.getMaxHumanReadableByte() +")"); 
		System.out.println("PER_HOST: \treply only: " +hostStat.perHost_avgReplytBytesPerSec.getAvgHumanReadableByte() +" (min avg: " +hostStat.perHost_avgReplytBytesPerSec.getMinHumanReadableByte() + ", max avg: " +hostStat.perHost_avgReplytBytesPerSec.getMaxHumanReadableByte() +")"); 
		System.out.println("PER_HOST: avg online time: " +hostStat.perHost_onlineTime.getAvg() +"ms (min: " +hostStat.perHost_onlineTime.getMin() + "ms, max: " +hostStat.perHost_onlineTime.getMax() +"ms)"); 
		System.out.println("PER_HOST: avg user think time: " +hostStat.perHost_userThinkTime.getAvgAvg() +"ms (min avg: " +hostStat.perHost_userThinkTime.getMinAvg() + "ms, max avg: " +hostStat.perHost_userThinkTime.getMaxAvg() +"ms)"); 
		System.out.println("PER_HOST: avg min user think time: " +hostStat.perHost_userThinkTime.getAvgMin() +"ms (min min: " +hostStat.perHost_userThinkTime.getMinMin() + "ms, max min: " +hostStat.perHost_userThinkTime.getMaxMin() +"ms)"); 
		System.out.println("PER_HOST: avg max user think time: " +hostStat.perHost_userThinkTime.getAvgMax() +"ms (min max: " +hostStat.perHost_userThinkTime.getMinMax() + "ms, max max: " +hostStat.perHost_userThinkTime.getMaxMax() +"ms)"); 
		
		System.out.println("\nFLOW STATISTICS: "); 
		System.out.println("FLOW_STATS: avg flows per host: " +hostStat.perHost_flows.getAvg() +" (min: " +hostStat.perHost_flows.getMin() + ", max: " +hostStat.perHost_flows.getMax() +")"); 
		System.out.println("FLOW_STATS: avg new flows/sec and host: " +hostStat.perHost_avgNewFlowsPerSec.getAvg() +" (min avg: " +hostStat.perHost_avgNewFlowsPerSec.getMin() + ", max avg: " +hostStat.perHost_avgNewFlowsPerSec.getMax() +")"); 
		System.out.println(); 
		System.out.println("FLOW_STATS: avg flow size per host: " +hostStat.perHost_bytesPerFlow.getAvgAvgHumanReadableByte() +" (min avg: " +hostStat.perHost_bytesPerFlow.getMinAvgHumanReadableByte() + ", max avg: " +hostStat.perHost_bytesPerFlow.getMaxAvgHumanReadableByte() +")"); 
		System.out.println("FLOW_STATS: \trequest only: " +hostStat.perHost_requestBytesPerFlow.getAvgAvgHumanReadableByte() +" (min avg: " +hostStat.perHost_requestBytesPerFlow.getMinAvgHumanReadableByte() + ", max avg: " +hostStat.perHost_requestBytesPerFlow.getMaxAvgHumanReadableByte() +")"); 
		System.out.println("FLOW_STATS: \treply only: " +hostStat.perHost_replyBytesPerFlow.getAvgAvgHumanReadableByte() +" (min avg: " +hostStat.perHost_replyBytesPerFlow.getMinAvgHumanReadableByte() + ", max avg: " +hostStat.perHost_replyBytesPerFlow.getMaxAvgHumanReadableByte() +")"); 
		System.out.println("FLOW_STATS: avg min flow size per host: " +hostStat.perHost_bytesPerFlow.getAvgMinHumanReadableByte() +" (min min: " +hostStat.perHost_bytesPerFlow.getMinMinHumanReadableByte() + ", max min: " +hostStat.perHost_bytesPerFlow.getMaxMinHumanReadableByte() +")"); 
		System.out.println("FLOW_STATS: \trequest only: " +hostStat.perHost_requestBytesPerFlow.getAvgMinHumanReadableByte() +" (min min: " +hostStat.perHost_requestBytesPerFlow.getMinMinHumanReadableByte() + ", max min: " +hostStat.perHost_requestBytesPerFlow.getMaxMinHumanReadableByte() +")"); 
		System.out.println("FLOW_STATS: \treply only: " +hostStat.perHost_replyBytesPerFlow.getAvgMinHumanReadableByte() +" (min min: " +hostStat.perHost_replyBytesPerFlow.getMinMinHumanReadableByte() + ", max min: " +hostStat.perHost_replyBytesPerFlow.getMaxMinHumanReadableByte() +")"); 
		System.out.println("FLOW_STATS: avg max flow size per host: " +hostStat.perHost_bytesPerFlow.getAvgMaxHumanReadableByte() +" (min max: " +hostStat.perHost_bytesPerFlow.getMinMaxHumanReadableByte() + ", max max: " +hostStat.perHost_bytesPerFlow.getMaxMaxHumanReadableByte() +")"); 
		System.out.println("FLOW_STATS: \trequest only: " +hostStat.perHost_requestBytesPerFlow.getAvgMaxHumanReadableByte() +" (min max: " +hostStat.perHost_requestBytesPerFlow.getMinMaxHumanReadableByte() + ", max max: " +hostStat.perHost_requestBytesPerFlow.getMaxMaxHumanReadableByte() +")"); 
		System.out.println("FLOW_STATS: \treply only: " +hostStat.perHost_replyBytesPerFlow.getAvgMaxHumanReadableByte() +" (min max: " +hostStat.perHost_replyBytesPerFlow.getMinMaxHumanReadableByte() + ", max max: " +hostStat.perHost_replyBytesPerFlow.getMaxMaxHumanReadableByte() +")"); 
		System.out.println(); 
		System.out.println("FLOW_STATS: avg flow duration: " +hostStat.perHost_flowDuration.getAvgAvg() +"ms (min avg: " +hostStat.perHost_flowDuration.getMinAvg() + "ms, max avg: " +hostStat.perHost_flowDuration.getMaxAvg() +"ms)"); 
		System.out.println("FLOW_STATS: avg min flow duration: " +hostStat.perHost_flowDuration.getAvgMin() +"ms (min min: " +hostStat.perHost_flowDuration.getMinMin() + "ms, max min: " +hostStat.perHost_flowDuration.getMaxMin() +"ms)"); 
		System.out.println("FLOW_STATS: avg max flow duration: " +hostStat.perHost_flowDuration.getAvgMax() +"ms (min max: " +hostStat.perHost_flowDuration.getMinMax() + "ms, max max: " +hostStat.perHost_flowDuration.getMaxMax() +"ms)"); 
		System.out.println(); 
		System.out.println("FLOW_STATS: avg flow groups per host: " +hostStat.perHost_flowGroups.getAvg() +" (min: " +hostStat.perHost_flowGroups.getMin() + ", max: " +hostStat.perHost_flowGroups.getMax() +")"); 
		System.out.println("FLOW_STATS: avg flow group duration: " +hostStat.perHost_flowGroupDuration.getAvgAvg() +"ms (min avg: " +hostStat.perHost_flowGroupDuration.getMinAvg() + "ms, max avg: " +hostStat.perHost_flowGroupDuration.getMaxAvg() +"ms)"); 
		System.out.println("FLOW_STATS: avg min flow group duration: " +hostStat.perHost_flowGroupDuration.getAvgMin() +"ms (min min: " +hostStat.perHost_flowGroupDuration.getMinMin() + "ms, max min: " +hostStat.perHost_flowGroupDuration.getMaxMin() +"ms)"); 
		System.out.println("FLOW_STATS: avg max flow group duration: " +hostStat.perHost_flowGroupDuration.getAvgMax() +"ms (min max: " +hostStat.perHost_flowGroupDuration.getMinMax() + "ms, max max: " +hostStat.perHost_flowGroupDuration.getMaxMax() +"ms)"); 
		
	}


	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		//new GeneralHostStatistics("./inputOutput/global/traces/erfTests/auckland8sample/DLPAExtractor100LongNorm.gmf", null);
		//new GeneralHostStatistics("./inputOutput/global/traces/erfTests/auckland8sample/5Min1000HostSampleAuck8MinusTopFlop5-HTTP.gmf", null);
		new GeneralHostStatistics("./inputOutput/global/traces/erfTests/auckland10sample/auck10_30min_1000user_dlpa.gmf", null);
		//new GeneralHostStatistics(PacketFilterTester.ERF_TEST_FILE_LONG, new HttpToWanWhitelist());
		//new GeneralHostStatistics("./inputOutput/global/traces/erfTests/auckland8sample/DLPAExtractor100LongNorm.gmf", null);
		//new GeneralHostStatistics("./inputOutput/global/traces/erfTests/auckland8sample/TestExtractorTest10LongNorm.gmf", null);
	} 
	
}

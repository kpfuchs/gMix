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
import evaluation.traceParser.engine.TraceInfo;
import evaluation.traceParser.engine.dataStructure.Host;
import evaluation.traceParser.scenarioExtractor.flowFilter.FlowFilter;
import framework.core.util.Util;


public class GeneralHostStatistics {

	private Host[] hosts;
	private HostStatistics hostStat;
	private TraceInfo traceInfo;
	
	
	public GeneralHostStatistics(String pathToTraceFolder, FlowFilter filter) {
		this.hosts = Host.getHostIndex(pathToTraceFolder, filter);
		this.hostStat = new HostStatistics(hosts);
		this.traceInfo = new TraceInfo(pathToTraceFolder);
	}
	
	
	public void writeStatisticsToDisk() {
		String outputPath = traceInfo.getPathToTraceFolder() + traceInfo.getNameOfTraceFileWithoutExtension() +"-stats.txt";
		writeStatisticsToDisk(outputPath);
	}
	
	
	public void writeStatisticsToDisk(String outputPath) {
		Util.writeToFile(createStatistics(), outputPath);
	}
	
	
	private String createStatistics() {
		StringBuffer result = new StringBuffer(10000);
		result.append("GLOBAL STATISTICS: ");
		double totalSeconds = (double)hostStat.total_duration / 1000d;
		result.append("\nTOTAL: file covers " +totalSeconds +" seconds " +"(start ts: " +hostStat.total_start +", end ts: " +hostStat.total_end +")");
		result.append("\nTOTAL: total hosts: " +hostStat.total_hosts);
		result.append("\nTOTAL: total flows: " +hostStat.total_flows +" (avg new flows/sec: " +(hostStat.total_flows / totalSeconds) +", "  +hostStat.total_flowGroups +" flow groups -> " +hostStat.total_flowsPerFlowGroup +" flows per flow group)");
		result.append("\nTOTAL: total data transferred: " +Util.humanReadableByteCount(hostStat.total_bytes, false) +" (" +Util.humanReadableByteCount(hostStat.total_requestBytes, false) + " request data and " +Util.humanReadableByteCount(hostStat.total_replyBytes, false) +" reply data)");
		result.append("\nTOTAL: total avg data/sec: " +Util.humanReadableByteCount((hostStat.total_bytes)/(long)totalSeconds, false) +" (" +Util.humanReadableByteCount((hostStat.total_requestBytes)/(long)totalSeconds, false) +" request, " +Util.humanReadableByteCount((hostStat.total_replyBytes)/(long)totalSeconds, false) +" reply)");
		result.append("\nTOTAL: protocol distribution:");
		for (int i=0; i<hostStat.total_protocolDistribution.length; i++)
			if (hostStat.total_protocolDistribution[i] != 0)
				result.append("\nTOTAL:     " +hostStat.total_protocolDistribution[i] +"x\t" +Protocol.getProtocol(i)); 
		
		result.append("\n\nHOST STATISTICS: "); 
		result.append("\nPER_HOST: avg data/sec of hosts: " +hostStat.perHost_avgBytesPerSec.getAvgHumanReadableByte() +" (min: " +hostStat.perHost_avgBytesPerSec.getMinHumanReadableByte() + ", max: " +hostStat.perHost_avgBytesPerSec.getMaxHumanReadableByte() +")"); 
		result.append("\nPER_HOST: \trequest only: " +hostStat.perHost_avgRequestBytesPerSec.getAvgHumanReadableByte() +" (min avg: " +hostStat.perHost_avgRequestBytesPerSec.getMinHumanReadableByte() + ", max avg: " +hostStat.perHost_avgRequestBytesPerSec.getMaxHumanReadableByte() +")"); 
		result.append("\nPER_HOST: \treply only: " +hostStat.perHost_avgReplytBytesPerSec.getAvgHumanReadableByte() +" (min avg: " +hostStat.perHost_avgReplytBytesPerSec.getMinHumanReadableByte() + ", max avg: " +hostStat.perHost_avgReplytBytesPerSec.getMaxHumanReadableByte() +")"); 
		result.append("\nPER_HOST: avg online time: " +hostStat.perHost_onlineTime.getAvg() +"ms (min: " +hostStat.perHost_onlineTime.getMin() + "ms, max: " +hostStat.perHost_onlineTime.getMax() +"ms)"); 
		result.append("\nPER_HOST: avg user think time: " +hostStat.perHost_userThinkTime.getAvgAvg() +"ms (min avg: " +hostStat.perHost_userThinkTime.getMinAvg() + "ms, max avg: " +hostStat.perHost_userThinkTime.getMaxAvg() +"ms)"); 
		result.append("\nPER_HOST: avg min user think time: " +hostStat.perHost_userThinkTime.getAvgMin() +"ms (min min: " +hostStat.perHost_userThinkTime.getMinMin() + "ms, max min: " +hostStat.perHost_userThinkTime.getMaxMin() +"ms)"); 
		result.append("\nPER_HOST: avg max user think time: " +hostStat.perHost_userThinkTime.getAvgMax() +"ms (min max: " +hostStat.perHost_userThinkTime.getMinMax() + "ms, max max: " +hostStat.perHost_userThinkTime.getMaxMax() +"ms)"); 
		
		result.append("\n\nFLOW STATISTICS: "); 
		result.append("\nFLOW_STATS: avg flows per host: " +hostStat.perHost_flows.getAvg() +" (min: " +hostStat.perHost_flows.getMin() + ", max: " +hostStat.perHost_flows.getMax() +")"); 
		result.append("\nFLOW_STATS: avg new flows/sec and host: " +hostStat.perHost_avgNewFlowsPerSec.getAvg() +" (min avg: " +hostStat.perHost_avgNewFlowsPerSec.getMin() + ", max avg: " +hostStat.perHost_avgNewFlowsPerSec.getMax() +")"); 
		
		result.append("\n\nFLOW_STATS: avg flow size per host: " +hostStat.perHost_bytesPerFlow.getAvgAvgHumanReadableByte() +" (min avg: " +hostStat.perHost_bytesPerFlow.getMinAvgHumanReadableByte() + ", max avg: " +hostStat.perHost_bytesPerFlow.getMaxAvgHumanReadableByte() +")"); 
		result.append("\nFLOW_STATS: \trequest only: " +hostStat.perHost_requestBytesPerFlow.getAvgAvgHumanReadableByte() +" (min avg: " +hostStat.perHost_requestBytesPerFlow.getMinAvgHumanReadableByte() + ", max avg: " +hostStat.perHost_requestBytesPerFlow.getMaxAvgHumanReadableByte() +")"); 
		result.append("\nFLOW_STATS: \treply only: " +hostStat.perHost_replyBytesPerFlow.getAvgAvgHumanReadableByte() +" (min avg: " +hostStat.perHost_replyBytesPerFlow.getMinAvgHumanReadableByte() + ", max avg: " +hostStat.perHost_replyBytesPerFlow.getMaxAvgHumanReadableByte() +")"); 
		result.append("\nFLOW_STATS: avg min flow size per host: " +hostStat.perHost_bytesPerFlow.getAvgMinHumanReadableByte() +" (min min: " +hostStat.perHost_bytesPerFlow.getMinMinHumanReadableByte() + ", max min: " +hostStat.perHost_bytesPerFlow.getMaxMinHumanReadableByte() +")"); 
		result.append("\nFLOW_STATS: \trequest only: " +hostStat.perHost_requestBytesPerFlow.getAvgMinHumanReadableByte() +" (min min: " +hostStat.perHost_requestBytesPerFlow.getMinMinHumanReadableByte() + ", max min: " +hostStat.perHost_requestBytesPerFlow.getMaxMinHumanReadableByte() +")"); 
		result.append("\nFLOW_STATS: \treply only: " +hostStat.perHost_replyBytesPerFlow.getAvgMinHumanReadableByte() +" (min min: " +hostStat.perHost_replyBytesPerFlow.getMinMinHumanReadableByte() + ", max min: " +hostStat.perHost_replyBytesPerFlow.getMaxMinHumanReadableByte() +")"); 
		result.append("\nFLOW_STATS: avg max flow size per host: " +hostStat.perHost_bytesPerFlow.getAvgMaxHumanReadableByte() +" (min max: " +hostStat.perHost_bytesPerFlow.getMinMaxHumanReadableByte() + ", max max: " +hostStat.perHost_bytesPerFlow.getMaxMaxHumanReadableByte() +")"); 
		result.append("\nFLOW_STATS: \trequest only: " +hostStat.perHost_requestBytesPerFlow.getAvgMaxHumanReadableByte() +" (min max: " +hostStat.perHost_requestBytesPerFlow.getMinMaxHumanReadableByte() + ", max max: " +hostStat.perHost_requestBytesPerFlow.getMaxMaxHumanReadableByte() +")"); 
		result.append("\nFLOW_STATS: \treply only: " +hostStat.perHost_replyBytesPerFlow.getAvgMaxHumanReadableByte() +" (min max: " +hostStat.perHost_replyBytesPerFlow.getMinMaxHumanReadableByte() + ", max max: " +hostStat.perHost_replyBytesPerFlow.getMaxMaxHumanReadableByte() +")"); 
		
		result.append("\n\nFLOW_STATS: avg flow duration: " +hostStat.perHost_flowDuration.getAvgAvg() +"ms (min avg: " +hostStat.perHost_flowDuration.getMinAvg() + "ms, max avg: " +hostStat.perHost_flowDuration.getMaxAvg() +"ms)"); 
		result.append("\nFLOW_STATS: avg min flow duration: " +hostStat.perHost_flowDuration.getAvgMin() +"ms (min min: " +hostStat.perHost_flowDuration.getMinMin() + "ms, max min: " +hostStat.perHost_flowDuration.getMaxMin() +"ms)"); 
		result.append("\nFLOW_STATS: avg max flow duration: " +hostStat.perHost_flowDuration.getAvgMax() +"ms (min max: " +hostStat.perHost_flowDuration.getMinMax() + "ms, max max: " +hostStat.perHost_flowDuration.getMaxMax() +"ms)"); 
		
		result.append("\n\nFLOW_STATS: avg flow groups per host: " +hostStat.perHost_flowGroups.getAvg() +" (min: " +hostStat.perHost_flowGroups.getMin() + ", max: " +hostStat.perHost_flowGroups.getMax() +")"); 
		result.append("\nFLOW_STATS: avg flow group duration: " +hostStat.perHost_flowGroupDuration.getAvgAvg() +"ms (min avg: " +hostStat.perHost_flowGroupDuration.getMinAvg() + "ms, max avg: " +hostStat.perHost_flowGroupDuration.getMaxAvg() +"ms)"); 
		result.append("\nFLOW_STATS: avg min flow group duration: " +hostStat.perHost_flowGroupDuration.getAvgMin() +"ms (min min: " +hostStat.perHost_flowGroupDuration.getMinMin() + "ms, max min: " +hostStat.perHost_flowGroupDuration.getMaxMin() +"ms)"); 
		result.append("\nFLOW_STATS: avg max flow group duration: " +hostStat.perHost_flowGroupDuration.getAvgMax() +"ms (min max: " +hostStat.perHost_flowGroupDuration.getMinMax() + "ms, max max: " +hostStat.perHost_flowGroupDuration.getMaxMax() +"ms)"); 
		
		return result.toString();
	}
	
	
	public void displayStatistics() {
		System.out.println(createStatistics()); 
	}


	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		//new GeneralHostStatistics("./inputOutput/global/traces/auck8/auck8_120min_1000user_dlpa.gmf", null).displayStatistics();
		//new GeneralHostStatistics("./inputOutput/global/traces/auck8/auck8_120min_1000user_dpe.gmf", null).displayStatistics();
		//new GeneralHostStatistics("./inputOutput/global/traces/auck8/auck8_10min_10000user_dlpa.gmf", null).displayStatistics();
		//new GeneralHostStatistics("./inputOutput/global/traces/auck8/auck8_10min_10000user_dpe.gmf", null).displayStatistics();
		
		new GeneralHostStatistics("./inputOutput/global/traces/auck10/auck10_120min_1000user_dlpa.gmf", null).displayStatistics();
		new GeneralHostStatistics("./inputOutput/global/traces/auck10/auck10_120min_1000user_dpe.gmf", null).displayStatistics();
		new GeneralHostStatistics("./inputOutput/global/traces/auck10/auck10_10min_10000user_dlpa.gmf", null).displayStatistics();
		new GeneralHostStatistics("./inputOutput/global/traces/auck10/auck10_10min_10000user_dpe.gmf", null).displayStatistics();
		
		//new GeneralHostStatistics("./inputOutput/global/traces/erfTests/auckland8sample/DLPAExtractor100LongNorm.gmf", null).displayStatistics();
		//new GeneralHostStatistics("./inputOutput/global/traces/erfTests/auckland8sample/5Min1000HostSampleAuck8MinusTopFlop5-HTTP.gmf", null).displayStatistics();
		//new GeneralHostStatistics("./inputOutput/global/traces/erfTests/auckland10sample/auck10_30min_1000user_dlpa.gmf", null).displayStatistics();
		//new GeneralHostStatistics(PacketFilterTester.ERF_TEST_FILE_LONG, new HttpToWanWhitelist()).displayStatistics();
		//new GeneralHostStatistics("./inputOutput/global/traces/erfTests/auckland8sample/DLPAExtractor100LongNorm.gmf", null).displayStatistics();
		//new GeneralHostStatistics("./inputOutput/global/traces/erfTests/auckland8sample/TestExtractorTest10LongNorm.gmf", null).displayStatistics();
	} 
	
}

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

import java.security.SecureRandom;
import java.util.Vector;

import evaluation.traceParser.engine.Protocol;
import evaluation.traceParser.engine.dataStructure.Host;
import evaluation.traceParser.statistics.calculator.MultiStat;
import evaluation.traceParser.statistics.calculator.Stat;
import framework.core.util.Util;


public class HostStatistics {

	public int total_hosts;
	public int total_flowGroups = 0;
	public int total_flows = 0;
	public int total_duration;
	public long total_start = Long.MAX_VALUE;
	public long total_end = Long.MIN_VALUE;
	public long total_requestBytes = 0;
	public long total_replyBytes = 0;
	public long total_bytes = 0;
	public long[] total_protocolDistribution = new long[Protocol.values().length];
	public double total_flowsPerFlowGroup;
	
	public int[] all_userThinkTimes;
	
	public Stat perHost_flowGroups = new Stat();
	public Stat perHost_flows = new Stat();
	public Stat perHost_onlineTime = new Stat();
	
	public MultiStat perHost_requestBytesPerFlow = new MultiStat();
	public MultiStat perHost_replyBytesPerFlow = new MultiStat();
	public MultiStat perHost_bytesPerFlow = new MultiStat();
	
	public Stat perHost_avgRequestBytesPerSec = new Stat();
	public Stat perHost_avgReplytBytesPerSec = new Stat();
	public Stat perHost_avgBytesPerSec = new Stat();
	public Stat perHost_avgNewFlowsPerSec = new Stat();
	
	public MultiStat perHost_userThinkTime = new MultiStat();
	public MultiStat perHost_flowDuration = new MultiStat();
	public MultiStat perHost_flowGroupDuration = new MultiStat();
	
	
	public HostStatistics(Host[] hosts) {
		calculate(hosts);
	} 
	
	
	private void calculate(Host[] hosts) {
		this.total_hosts = hosts.length;
		Vector<Integer> userThinkTimes = new Vector<Integer>(hosts.length * 5);
		for (int i=0; i<hosts.length; i++) {
			
			this.total_flowGroups += hosts[i].stat_numberOfFlowGroups;
			this.total_flows += hosts[i].stat_numberOfFlows;
			if (hosts[i].firstAction < total_start)
				this.total_start = hosts[i].firstAction;
			if (hosts[i].lastAction > total_end)
				this.total_end = hosts[i].lastAction;
			this.total_requestBytes += hosts[i].stat_requestBytesTransferred;
			this.total_replyBytes += hosts[i].stat_replyBytesTransferred;
			this.total_bytes += hosts[i].stat_requestBytesTransferred + hosts[i].stat_replyBytesTransferred;
			for (int j=0; j<hosts[i].stat_protocolDistributionPerFlow.length; j++)
				this.total_protocolDistribution[j] += hosts[i].stat_protocolDistributionPerFlow[j];
			
			if (hosts[i].stat_userThinkTimes != null)
				for (int j=0; j<hosts[i].stat_userThinkTimes.length; j++)
					userThinkTimes.add(hosts[i].stat_userThinkTimes[j]); 
			
			this.perHost_flowGroups.addValue(hosts[i].stat_numberOfFlowGroups);
			this.perHost_flows.addValue(hosts[i].stat_numberOfFlows);
			this.perHost_onlineTime.addValue(hosts[i].stat_onlineTime);
			
			this.perHost_requestBytesPerFlow.addMinValue(hosts[i].stat_minRequestBytesPerFlow);
			this.perHost_requestBytesPerFlow.addAvgValue(hosts[i].stat_avgRequestBytesPerFlow);
			this.perHost_requestBytesPerFlow.addMaxValue(hosts[i].stat_maxRequestBytesPerFlow);
			this.perHost_replyBytesPerFlow.addMinValue(hosts[i].stat_minReplyBytesPerFlow);
			this.perHost_replyBytesPerFlow.addAvgValue(hosts[i].stat_avgReplyBytesPerFlow);
			this.perHost_replyBytesPerFlow.addMaxValue(hosts[i].stat_maxReplyBytesPerFlow);
			this.perHost_bytesPerFlow.addMinValue(hosts[i].stat_minRequestBytesPerFlow + hosts[i].stat_minReplyBytesPerFlow);
			this.perHost_bytesPerFlow.addAvgValue(hosts[i].stat_avgRequestBytesPerFlow + hosts[i].stat_avgReplyBytesPerFlow);
			this.perHost_bytesPerFlow.addMaxValue(hosts[i].stat_maxRequestBytesPerFlow + hosts[i].stat_maxReplyBytesPerFlow);
			
			this.perHost_avgRequestBytesPerSec.addValue(hosts[i].stat_avgRequestBytesPerSec);
			this.perHost_avgReplytBytesPerSec.addValue(hosts[i].stat_avgReplyBytesPerSec);
			this.perHost_avgBytesPerSec.addValue(hosts[i].stat_avgRequestBytesPerSec + hosts[i].stat_avgReplyBytesPerSec);
			this.perHost_avgNewFlowsPerSec.addValue(hosts[i].stat_avgNewFlowsPerSec);
			
			if (hosts[i].stat_minUserThinkTime != Util.NOT_SET) {
				this.perHost_userThinkTime.addMinValue(hosts[i].stat_minUserThinkTime);
				this.perHost_userThinkTime.addAvgValue(hosts[i].stat_avgUserThinkTime);
				this.perHost_userThinkTime.addMaxValue(hosts[i].stat_maxUserThinkTime);
			}
			
			this.perHost_flowDuration.addMinValue(hosts[i].stat_minFlowDuration);
			this.perHost_flowDuration.addAvgValue(hosts[i].stat_avgFlowDuration);
			this.perHost_flowDuration.addMaxValue(hosts[i].stat_maxFlowDuration);
			
			this.perHost_flowGroupDuration.addMinValue(hosts[i].stat_minFlowGroupDuration);
			this.perHost_flowGroupDuration.addAvgValue(hosts[i].stat_avgFlowGroupDuration);
			this.perHost_flowGroupDuration.addMaxValue(hosts[i].stat_maxFlowGroupDuration);
			
		}
		
		this.total_duration = (int) (total_end - total_start);
		this.total_flowsPerFlowGroup = (double)total_flows / (double)total_flowGroups;
		this.all_userThinkTimes = Util.toIntArray(userThinkTimes);
		
		this.perHost_requestBytesPerFlow.calculateValues();
		this.perHost_replyBytesPerFlow.calculateValues();
		this.perHost_bytesPerFlow.calculateValues();
		this.perHost_userThinkTime.calculateValues();
		this.perHost_flowDuration.calculateValues();
		this.perHost_flowGroupDuration.calculateValues();
	}
	
	
	public int drawRandomSample_userThinkTime(SecureRandom secureRandom) {
		return all_userThinkTimes[secureRandom.nextInt(all_userThinkTimes.length)];
	}

}

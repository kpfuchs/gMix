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
package evaluation.traceParser.statistics;

import evaluation.traceParser.engine.dataStructure.Host;
import evaluation.traceParser.scenarioExtractor.flowFilter.FlowFilter;


public class ThroughputStatistics {

	private Host[] hosts;
	private HostStatistics hostStat;
	
	
	public ThroughputStatistics(String pathToTraceFolder, FlowFilter filter) {
		this.hosts = Host.getHostIndex(pathToTraceFolder, filter);
		this.hostStat = new HostStatistics(hosts);
		displayStatistics();
	}
	
	
	private void displayStatistics() {
		double totalSeconds = (double)hostStat.total_duration / 1000d;
		for (Host h: hosts) {
			double kbps = h.stat_requestBytesTransferred + h.stat_replyBytesTransferred;
			System.out.println((kbps/1024d)/totalSeconds); 
		}
	}


	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		new ThroughputStatistics("./inputOutput/global/traces/erfTests/auckland10sample/auck10_5min_1000user_dlpa.gmf", null);
	} 
	
}

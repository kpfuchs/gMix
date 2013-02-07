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
package evaluation.traceParser.scenarioExtractor;

import java.util.HashSet;

import evaluation.traceParser.engine.dataStructure.Flow;
import evaluation.traceParser.engine.dataStructure.FlowGroup;
import evaluation.traceParser.engine.dataStructure.Host;
import evaluation.traceParser.engine.dataStructure.ModifiableHost;
import evaluation.traceParser.engine.filter.PacketFilterTester;
import evaluation.traceParser.scenarioExtractor.flowFilter.FlowFilter;
import evaluation.traceParser.scenarioExtractor.flowFilter.HttpHttpsToWanWhitelist;
import evaluation.traceParser.statistics.HostSampleSource.UrnModel;


public class DLPAExtractor extends Extractor {

	private HttpHttpsToWanWhitelist filter;
	
	
	public DLPAExtractor(String pathToTraceFolder) {
		super(pathToTraceFolder);
	}
	

	@Override
	public String getFileName() {
		return "DLPAExtractor";
	}
	
	
	@Override
	public FlowFilter getFlowFilter() {
		if (filter == null)
			filter = new HttpHttpsToWanWhitelist();
		return filter;
	}
	
	
	@Override
	public void extract() {
		long duration = 1001*60*1000; // 101 minutes
		urn = createUrn(UrnModel.DRAW_WITH_REPLACEMENT);
		for (int i=0; i<2; i++) { // 100 hosts/constant flows
			ModifiableHost host = new ModifiableHost(getFlowFilter());
			host.hostId = i;
			host.firstAction = 0;
			host.lastAction = 0;
			long now = 0;
			while (host.lastAction < duration) {
				Flow flow = urn.drawRandomFlow();
				flow.resetStart(now);
				FlowGroup fg = new FlowGroup(flow);
				host.flowGroups.add(fg);
				now += (fg.end -fg.start);
				host.lastAction = fg.end;
			}
			if (host.lastAction > duration)
				host.cutOffAfter(duration);
			writeToDestinationTrace(host);
		}
	}

	
	@Override
	public HashSet<Host> createHostBlackList() {
		float lowCutoff = 0.05f; // cut off 5%
		float highCutoff = 0.05f;
		HashSet<Host> blacklist = new HashSet<Host>(hosts.length);
		int lowBound = Math.round((float)hosts.length * lowCutoff);
		int highBound = Math.round((float)hosts.length - ((float)hosts.length * highCutoff));
		for (Host h: hosts) {
			if (	h.stat_rank_byAvgNewFlowsPerSec < lowBound 
					|| h.stat_rank_byAvgNewFlowsPerSec > highBound
					) // flow filter
				blacklist.add(h);
			else if (h.stat_rank_byAvgBytesPerSec < lowBound 
					|| h.stat_rank_byAvgBytesPerSec > highBound
					) // throughput filter
				blacklist.add(h);
		}
		return blacklist;
	}

	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		new DLPAExtractor(PacketFilterTester.AUCK_8);
	}

}
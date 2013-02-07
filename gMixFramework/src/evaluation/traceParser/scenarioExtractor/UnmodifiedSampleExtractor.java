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

import evaluation.traceParser.engine.dataStructure.Host;
import evaluation.traceParser.engine.dataStructure.ModifiableHost;
import evaluation.traceParser.engine.filter.PacketFilterTester;
import evaluation.traceParser.scenarioExtractor.flowFilter.FlowFilter;
import evaluation.traceParser.statistics.HostSampleSource.UrnModel;

public class UnmodifiedSampleExtractor extends Extractor {

	
	public UnmodifiedSampleExtractor(String pathToTraceFolder) {
		super(pathToTraceFolder);
	}
	

	@Override
	public String getFileName() {
		return "UnmodifiedSampleExtractor";
	}
	
	
	@Override
	public FlowFilter getFlowFilter() {
		return null;
	}
	
	
	@Override
	public void extract() {
		urn = createUrn(UrnModel.DRAW_WITH_REPLACEMENT);
		for (int i=0; i<1000; i++) {
			ModifiableHost h = urn.drawRandomHost();
			writeToDestinationTrace(h);
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
		new UnmodifiedSampleExtractor(PacketFilterTester.AUCK_10);
	}
	
}

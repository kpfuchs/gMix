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
import evaluation.traceParser.scenarioExtractor.flowFilter.FlowFilter;
import evaluation.traceParser.scenarioExtractor.flowFilter.HttpWhitelist;
import evaluation.traceParser.statistics.HostSampleSource.UrnModel;

public class FixedLengthExtractorDPE extends Extractor {

	private static int users;
	private static int lengthInMs;
	private static String outputFileName;
	
	
	public FixedLengthExtractorDPE(String pathToTraceFolder, int users, int lengthInMs, String outputFileName) {
		super(setParameters(pathToTraceFolder, users, lengthInMs, outputFileName));
	}
	

	@Override
	public String getFileName() {
		return outputFileName;
	}
	
	
	@Override
	public FlowFilter getFlowFilter() {
		return new HttpWhitelist();
	}
	
	
	@Override
	public void extract() {
		urn = createUrn(UrnModel.DRAW_WITH_REPLACEMENT);
		for (int i=0; i<users; i++) {
			ModifiableHost h = urn.drawRandomHost();
			h.resetStart(0);
			h.removeOfflinePhases(10*60*1000, urn);
			while (h.lastAction < lengthInMs || h.lastAction > (lengthInMs + (1*60*1000))) {
				h.concat(urn.drawRandomHost(), urn);
				h.removeOfflinePhases(10*60*1000, urn);
				h.cutOffAfter(lengthInMs + (1*60*1000));
			}
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
			else if (h.stat_rank_byRequestBytesTransferred < lowBound 
					|| h.stat_rank_byRequestBytesTransferred > highBound
					) // total request bytes transferred filter
				blacklist.add(h);
			else if (h.stat_rank_byReplyBytesTransferred < lowBound 
					|| h.stat_rank_byReplyBytesTransferred > highBound
					) // total request bytes transferred filter
				blacklist.add(h);
		}
		return blacklist;
	}

	
	private static String setParameters(String pathToTraceFolder, int users, int lengthInMs, String outputFileName) {
		FixedLengthExtractorDPE.outputFileName = outputFileName;
		FixedLengthExtractorDPE.users = users;
		FixedLengthExtractorDPE.lengthInMs = lengthInMs;
		return pathToTraceFolder;
	}
	
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		new FixedLengthExtractorDPE("./inputOutput/global/traces/auck8/", 1000, 120*60*1000,"auck8_120min_1000user_dpe");
		new FixedLengthExtractorDPE("./inputOutput/global/traces/auck10/", 1000, 120*60*1000,"auck10_120min_1000user_dpe");
		new FixedLengthExtractorDPE("./inputOutput/global/traces/auck8/", 10000, 10*60*1000,"auck8_10min_10000user_dpe");
		new FixedLengthExtractorDPE("./inputOutput/global/traces/auck10/", 10000, 10*60*1000,"auck10_10min_10000user_dpe");
	}

}
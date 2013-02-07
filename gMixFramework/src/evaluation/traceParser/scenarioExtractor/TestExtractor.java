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
package evaluation.traceParser.scenarioExtractor;

import java.util.HashSet;

import evaluation.traceParser.engine.dataStructure.Host;
import evaluation.traceParser.engine.dataStructure.ModifiableHost;
import evaluation.traceParser.engine.filter.PacketFilterTester;
import evaluation.traceParser.scenarioExtractor.flowFilter.FlowFilter;
import evaluation.traceParser.scenarioExtractor.flowFilter.HttpWhitelist;
import evaluation.traceParser.statistics.HostSampleSource.UrnModel;


public class TestExtractor extends Extractor {

	
	public TestExtractor(String pathToTraceFolder) {
		super(pathToTraceFolder);
	}
	

	@Override
	public String getFileName() {
		return "TestExtractorTest";
	}
	
	
	@Override
	public FlowFilter getFlowFilter() {
		return new HttpWhitelist();
	}
	
	
	@Override
	public void extract() {
		urn = createUrn(UrnModel.DRAW_WITH_REPLACEMENT);
		for (int i=0; i<1000; i++) {
			ModifiableHost h = urn.drawRandomHost();
			//System.out.println(h.hostId); 
			h.resetStart(0);
			h.removeOfflinePhases(10*60*1000, urn);
			while (h.lastAction < 5*60*1000 || h.lastAction > 6*60*1000) {
				h.concat(urn.drawRandomHost(), urn);
				h.removeOfflinePhases(10*60*1000, urn);
				h.cutOffAfter(6*60*1000);
			}
			//System.out.println(h.cutOffAfter(5*60*1000));
			//System.out.println(i +" la: " +h.lastAction); 
			writeToDestinationTrace(h);
		}
		/*String path = Util.removeFileExtension(traceInfo.getPathToTraceFile()) +".gmf";
		try {
			for (int i=0; i<hosts.length; i++) {
				sourceTrace.seek(hosts[i].offsetInTraceFile);
				System.out.println("first line of host " +hosts[i].hostId +" (offset: " +hosts[i].offsetInTraceFile +"): " +sourceTrace.readLine()); 
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not load flow groups from " +path); 
		}*/
		
		/*for (int i=0; i<hosts.length; i++) {
			System.out.println(hosts[i].firstAction +", " +hosts[i].lastAction); 
			urn = createUrn(UrnModel.DRAW_WITH_REPLACEMENT);
			if (i == (hosts.length-1))
				System.out.println("last"); 
			hosts[i].loadFlowGroups(urn.sourceTrace);
			ModifiableHost mh = new ModifiableHost(hosts[i]);
			System.out.println(mh.firstAction +", " +mh.lastAction); 
			mh.resetStart(0);
			System.out.println(mh.firstAction +", " +mh.lastAction); 
			ExtendedHost.calculateStatistics(mh);
			System.out.println(mh.firstAction +", " +mh.lastAction); 
			System.out.println(); 
			writeToDestinationTrace(mh);
		} */
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

	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		new TestExtractor(PacketFilterTester.AUCK_8);
	}
	
}

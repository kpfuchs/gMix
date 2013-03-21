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

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;

import evaluation.traceParser.engine.TraceInfo;
import evaluation.traceParser.engine.dataStructure.Host;
import evaluation.traceParser.engine.dataStructure.ModifiableHost;
import evaluation.traceParser.engine.filter.PacketFilterTester;
import evaluation.traceParser.scenarioExtractor.flowFilter.FlowFilter;
import evaluation.traceParser.statistics.GeneralHostStatistics;
import evaluation.traceParser.statistics.HostSampleSource;
import evaluation.traceParser.statistics.HostSampleSource.UrnModel;
import framework.core.util.Util;


public abstract class Extractor {

	protected Host[] hosts;
	protected int[] blacklist;
	protected TraceInfo traceInfo;
	protected String sourceTracePath;
	protected String destTracePath;
	protected RandomAccessFile sourceTrace;
	protected Writer destTrace;
	protected HostSampleSource urn;
	protected SecureRandom secureRandom;
	private boolean writeDone = false;
	private int hostIdCounter = -1;
	private final long PRNG_SEED;

	
	/**
	 * an Extractor is a class that parses a flow trace file (GMF) and
	 * extracts and rearranges flows, e.g. to assert that 
	 * - only X hosts are contained in the result trace
	 * - all hosts are online for Y ms
	 * - no host has a higher sending rate than Z
	 * - ...
	 *  
	 * extend this class with a custom Extractor ("implementation").
	 * this class provides standard methods for reading and writing hosts.
	 * 
	 * for an example of usage see TestExtractor.java
	 * 
	 * @param pathToTraceFolder
	 */
	public Extractor(String pathToTraceFolder) {
		this.traceInfo = new TraceInfo(pathToTraceFolder);
		this.PRNG_SEED = traceInfo.getPrngSeed();
		this.hosts = Host.getHostIndex(traceInfo, getFlowFilter());
		initTraceFiles(traceInfo);
		HashSet<Host> bl = createHostBlackList();
		this.hosts = Host.applyBlacklist(hosts, bl);
		this.blacklist = new int[bl.size()];
		int ctr = 0;
		for (Host h:bl) 
			this.blacklist[ctr++] = h.hostId;
		Arrays.sort(this.blacklist);
		try {
			this.secureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("no \"SHA1PRNG\" available on this workstation. will use default PRNG." +
					"\nthe EXTRACTION PROCESS WILL NOT BE REPEATABLE!"); 
			this.secureRandom = new SecureRandom();
		} catch (NoSuchProviderException e) {
			System.err.println("no \"SUN\" SecureRandom provider available on this workstation. will use default PRNG." +
					"\nthe EXTRACTION PROCESS WILL NOT BE REPEATABLE!"); 
			this.secureRandom = new SecureRandom();
		}
		this.secureRandom.setSeed(PRNG_SEED);
		long start = System.currentTimeMillis();
		System.out.println("starting " +getFileName()); 
		extract();
		close();
		System.out.println("finished extracting (duration: " +((double)(System.currentTimeMillis() - start)/1000d) +" sec)"); 
		System.out.println("creating stats.txt"); 
		new GeneralHostStatistics(this.destTracePath , null).writeStatisticsToDisk();
		System.out.println("done"); 
		
	}
	
	
	/**
	 * returns the file name that shall be used for writing the result trace.
	 * should only be the file name (not the complete path).
	 * 
	 * implementation example:
	 * @Override
	 * public String getFileName() {
	 *  return "TestExtractor";
	 * }
	 */
	public abstract String getFileName();
	
	
	/**
	 * returns the FlowFilter that shall be used to filter the flows for the 
	 * implementing class (see package flowFilter). Most FlowFilters are simple 
	 * protocol filters (e.g. HTTP only)., i.e. the host array (see instance 
	 * variable) will only contain hosts that contain at least one flow that 
	 * was not filtered.
	 * the FlowFilter is applied BEFORE the Host filter, i.e. this method will 
	 * be called before "createHostBlackList()" is called. subclasses can be 
	 * sure that the flows in the hosts array will have passed the filter. 
	 * 
	 * return "null" if no FlowFilter should be used
	 * 
	 * implementation example:
	 * @Override
	 * public FlowFilter getFlowFilter() {
	 *  return new HttpHttpsWhitelist();
	 * }
	 */
	public abstract FlowFilter getFlowFilter();
	
	
	/**
	 * hosts contained in the returned black list (HashSet) will be removed 
	 * from the hosts array of this class.
	 * this method will be called before "extract()" is called, i.e. subclasses 
	 * can be sure that the hosts array will contain no black list hosts. 
	 *  
	 * implementation example:
	 * @Override
	 * public HashSet<Host> createHostBlackList() {
	 *  HashSet<Host> blacklist = new HashSet<Host>(hosts.length);
	 *  for (Host h: hosts)
	 *   if (h.value != INTERESTING)
	 *    blacklist.add(h);
	 *  return blacklist;
	 * }
	 */
	public abstract HashSet<Host> createHostBlackList();
	
	
	/**
	 * selects hosts from the hosts array and writes them to the result trace.
	 * the hosts array will contain only hosts that passed both flow and host 
	 * filters (see getFlowFilter() and createHostBlackList())
	 * 
	 * implementation example:
	 * @Override
	 * public void extract() { // extract 10 random hosts
	 *  for (int i=0; i<10; i++) {
	 *   urn = createUrn(UrnModel.DRAW_WITH_REPLACEMENT);
	 *   ModifiableHost h = urn.drawRandomHost();
	 *   h.resetStart(0);
	 *   writeToDestinationTrace(h);
	 *  }
	 * }
	 */
	public abstract void extract();
	
	
	protected HostSampleSource createUrn(UrnModel urnModel) {
		this.urn = new HostSampleSource(urnModel, this.hosts, this.traceInfo, this.sourceTrace, getFlowFilter(), this.secureRandom, blacklist);
		return this.urn;
	}
	
	
	protected void writeToDestinationTrace(ModifiableHost[] hosts) {
		if (writeDone)
			throw new RuntimeException("this method may only be called once\nuse writeToDestinationTrace(Host host) to write several times"); 
		if (hosts[0].flowGroups == null || hosts[0].flowGroups.size() == 0)
			throw new RuntimeException("the bypassed host contains no flows. make sure you add synthetic flows manually or recreate original flows by calling host.loadFlowGroups()"); 
		try {
			for (ModifiableHost host: hosts) {
				host.reassignHostId(++hostIdCounter);
				host.serialize(destTrace);
			}
		} catch (IOException e) {
			throw new RuntimeException("could not write to file " +destTracePath); 
		}
		writeDone = true;
	}
	
	
	protected void writeToDestinationTrace(ModifiableHost host) {
		if (host.flowGroups == null || host.flowGroups.size() == 0)
			throw new RuntimeException("the bypassed host contains no flows. make sure you add synthetic flows manually or recreate original flows by calling host.loadFlowGroups()"); 
		host.reassignHostId(++hostIdCounter);
		try {
			host.serialize(destTrace);
		} catch (IOException e) {
			throw new RuntimeException("could not write to file " +destTracePath); 
		}
	}
	
	
	private void initTraceFiles(TraceInfo traceInfo) {
		this.sourceTracePath = Util.removeFileExtension(traceInfo.getPathToTraceFile()) +".gmf";
		this.destTracePath = traceInfo.getPathToTraceFolder() +Util.removeFileExtension(getFileName()) +".gmf";
		try {
			this.sourceTrace = new RandomAccessFile(sourceTracePath, "r");
		} catch (IOException e) {
			throw new RuntimeException("ERROR: could not open file "  +sourceTracePath);
		}
		try {
			this.destTrace = new BufferedWriter(new OutputStreamWriter(new DataOutputStream(new FileOutputStream(destTracePath))));
		} catch (IOException e) {
			throw new RuntimeException("ERROR: could not create file "  +destTracePath);
		}
	}
	
	
	private void close() {
		try {
			this.sourceTrace.close();
			this.destTrace.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		new TestExtractor(PacketFilterTester.GMF_TEST_FILE_1);
	}
	
}

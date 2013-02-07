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

import java.io.BufferedReader;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.channels.Channels;
import java.util.HashSet;
import java.util.Vector;

import evaluation.traceParser.engine.Protocol;
import evaluation.traceParser.engine.TraceInfo;
import evaluation.traceParser.engine.fileReader.FlowGroupReader;
import evaluation.traceParser.engine.fileReader.FlowIterator;
import evaluation.traceParser.engine.fileReader.FlowReader;
import evaluation.traceParser.engine.fileReader.HostIndexCreator;
import evaluation.traceParser.engine.fileReader.TransientFlowIterator;
import evaluation.traceParser.scenarioExtractor.flowFilter.FlowFilter;
import framework.core.util.Util;


public class Host implements Serializable {
	
	private static final long serialVersionUID = 3693971555574677995L;
	
	public int hostId;
	public long firstAction = Long.MAX_VALUE; // offset from start of trace (timestamp)
	public long lastAction = Long.MIN_VALUE;
	public long offsetInTraceFile;
	
	/** 
	 * only available after calling loadFlowGroups() (the Host 
	 * data structure is read from an index file. a subset of flows is supposed 
	 * to be selected from the index (based on the stat_* variables below). to 
	 * access the actual flows of a host, flows must be read from the trace 
	 * file (not the index file)). this read operation is triggered by calling 
	 * loadFlowGroups().
	 * (to get the index, call getHostIndex())
	 */
	public transient Vector<FlowGroup> flowGroups = new Vector<FlowGroup>();
	private transient boolean flowGroupsLoaded = false;
	
	public transient FlowFilter filter;
	
	
	// statistics values:
	public int stat_numberOfFlows = 0;
	public int stat_numberOfFlowGroups = 0;
	
	public int stat_onlineTime = 0;

	public long stat_requestBytesTransferred = 0;
	public long stat_replyBytesTransferred = 0;
	
	public double stat_avgRequestBytesPerSec = 0d;
	public double stat_avgReplyBytesPerSec = 0d;
	public double stat_avgNewFlowsPerSec = 0d;
	
	public double stat_avgFlowsPerFlowGroup = 0d;
	public int stat_minFlowsPerFlowGroup = 0;
	public int stat_maxFlowsPerFlowGroup = 0;
	
	public int stat_avgRequestBytesPerFlow = 0;
	public int stat_minRequestBytesPerFlow = 0;
	public int stat_maxRequestBytesPerFlow = 0;
	
	public int stat_avgReplyBytesPerFlow = 0;
	public int stat_minReplyBytesPerFlow = 0;
	public int stat_maxReplyBytesPerFlow = 0;
	
	public int stat_avgUserThinkTime = Util.NOT_SET;
	public int stat_minUserThinkTime = Util.NOT_SET;
	public int stat_maxUserThinkTime = Util.NOT_SET;
	public int[] stat_userThinkTimes;
			
	public int stat_avgFlowDuration = 0;
	public int stat_minFlowDuration = 0;
	public int stat_maxFlowDuration = 0;
	
	public int stat_avgFlowGroupDuration = 0;
	public int stat_minFlowGroupDuration = 0;
	public int stat_maxFlowGroupDuration = 0;
	
	public int[] stat_protocolDistributionPerFlow = new int[Protocol.values().length];
	
	public int stat_rank_byNumberOfFlows = Util.NOT_SET;
	public int stat_rank_byOnlineTime = Util.NOT_SET;
	public int stat_rank_byAvgBytesPerSec = Util.NOT_SET;
	public int stat_rank_byAvgRequestBytesPerSec = Util.NOT_SET;
	public int stat_rank_byAvgReplyBytesPerSec = Util.NOT_SET;
	public int stat_rank_byTotalBytesTransferred = Util.NOT_SET;
	public int stat_rank_byRequestBytesTransferred = Util.NOT_SET;
	public int stat_rank_byReplyBytesTransferred = Util.NOT_SET;
	public int stat_rank_byAvgNewFlowsPerSec = Util.NOT_SET;
	

	public Host(FlowFilter filter) {
		this.filter = filter;
	}
	
	
	/*public void loadFlowGroups(String pathToTraceFolder) {
		loadFlowGroups(new TraceInfo(pathToTraceFolder));
	}*/
	
	
	public void loadFlowGroups(String pathToTraceFolder) {
		loadFlowGroups(new TraceInfo(pathToTraceFolder));
	}
	
	
	/*public void loadFlowGroups(TraceInfo traceInfo) {
		loadFlowGroups(traceInfo, null);
	}*/
	
	
	public void loadFlowGroups(TraceInfo traceInfo) {
		String path = Util.removeFileExtension(traceInfo.getPathToTraceFile()) +".gmf";
		try {
			RandomAccessFile raf = new RandomAccessFile(path, "r");
			loadFlowGroups(raf);
			raf.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not load flow groups from " +path); 
		}
	}
	
	
	public void loadFlowGroups(RandomAccessFile raf) {
		if (flowGroupsLoaded)
			return;
		try {
			raf.seek(offsetInTraceFile);
			// use BufferedReader to read line instead of raf.readLine() for performance reasons:
			BufferedReader reader = new BufferedReader(Channels.newReader(raf.getChannel(), "ISO-8859-1"));
			FlowGroupReader fgReader = new FlowGroupReader(reader, filter);
			assert fgReader.peekNextFlowGroup().senderId == hostId: "" +fgReader.peekNextFlowGroup().senderId +" != " +hostId +" (offset: " +offsetInTraceFile +")";
			flowGroups = new Vector<FlowGroup>();
			while (fgReader.peekNextFlowGroup() != null && fgReader.peekNextFlowGroup().senderId == hostId)
				flowGroups.add(fgReader.readFlowGroup());
			flowGroupsLoaded = true;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not load flow groups from " +raf);
		}
	}
	
	
	public FlowReader getFlowReader(String pathToTraceFolder) {
		return getFlowReader(new TraceInfo(pathToTraceFolder));
	}
	
	
	public FlowReader getFlowReader(TraceInfo traceInfo) {
		FlowReader result = null;
		String path = Util.removeFileExtension(traceInfo.getPathToTraceFile()) +".gmf";
		try {
			RandomAccessFile raf = new RandomAccessFile(path, "r");
			result = getFlowReader(raf);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not read " +path); 
		}
		return result;
	}
	
	
	public FlowReader getFlowReader(RandomAccessFile raf) {
		FlowReader result = null;
		try {
			raf.seek(offsetInTraceFile);
			// use BufferedReader to read line instead of raf.readLine() for performance reasons:
			BufferedReader reader = new BufferedReader(Channels.newReader(raf.getChannel(), "ISO-8859-1"));
			result = new FlowReader(reader, filter);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not read " +raf);
		}
		return result;
	}

	
	public FlowGroupReader getFlowGroupReader(String pathToTraceFolder) {
		return getFlowGroupReader(new TraceInfo(pathToTraceFolder));
	}
	
	
	public FlowGroupReader getFlowGroupReader(TraceInfo traceInfo) {
		FlowGroupReader result = null;
		String path = Util.removeFileExtension(traceInfo.getPathToTraceFile()) +".gmf";
		try {
			RandomAccessFile raf = new RandomAccessFile(path, "r");
			result = getFlowGroupReader(raf);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not read " +path); 
		}
		return result;
	}

	
	public FlowGroupReader getFlowGroupReader(RandomAccessFile raf) {
		FlowGroupReader result = null;
		try {
			raf.seek(offsetInTraceFile);
			// use BufferedReader to read line instead of raf.readLine() for performance reasons:
			BufferedReader reader = new BufferedReader(Channels.newReader(raf.getChannel(), "ISO-8859-1"));
			result = new FlowGroupReader(reader, filter);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not read " +raf);
		}
		return result;
	}
	

	/*public void loadFlowGroups(RandomAccessFile raf) {
		try {
			raf.seek(offsetInTraceFile);
			// use BufferedReader to read line instead of raf.readLine() for performance reasons:
			BufferedReader reader = new BufferedReader(Channels.newReader(raf.getChannel(), "ISO-8859-1"));
			String line = reader.readLine();
			FlowGroup fg = new FlowGroup(line);
			flowGroups = new Vector<FlowGroup>(); 
			do {
				assert fg.senderId == hostId;
				flowGroups.add(fg);
				line = reader.readLine();
				fg = line == null ? null : new FlowGroup(line);
			} while(fg != null && fg.senderId == hostId);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not load flow groups from " +raf); 
		}
	}*/
	
	
	/**
	 * use this method to create a modifiable host from a real host. 
	 * a modifiable host is a host who's contents (flows) can be modified 
	 * through several methods, e.g. flows can be cut off after x ms with 
	 * cutOffAfter(long).
	 * most modifiable hosts are based on "real" hosts (i.e., hosts that were
	 * present in a real trace file and are thus listed in the HostIndex data 
	 * structure (see HostIndexCreator.java)).
	 * use this method to clone a host from the host index in order to 
	 * modify it. 
	 * hosts in the HostIndex should NEVER be modified directly (the HostIndex 
	 * will be used by several methods to draw random samples. if the hosts of 
	 * the HostIndex are modified, the random samples will be drawn from 
	 * modified hosts which may introduce none-negligible errors).
	 */
	public ModifiableHost cloneHost() {
		return ModifiableHost.cloneHost(this);
	}
	
	
	/*@Override
	public boolean equals(Object obj) {
		Host h = (Host)obj;
		if (	h.hostId == hostId &&
				h.firstAction == firstAction &&
				h.lastAction == lastAction &&
				h.flowGroups.size() == flowGroups.size()
			)
			return true;
		return false;
	}
	
	public String visualEquals(Object obj) {
		Host h = (Host)obj;
		StringBuffer sb = new StringBuffer();
		sb.append("\nhostId: " +h.hostId +", h.hostId: " +hostId);
		sb.append("\nfirstAction: " +h.firstAction +", h.firstAction: " +firstAction);
		sb.append("\nlastAction: " +h.lastAction +", h.lastAction: " +lastAction);
		sb.append("\nflowGroups.size(): " +h.flowGroups.size() +", h.flowGroups.size(): " +flowGroups.size());
		return sb.toString();
	}*/
	
	
	public FlowIterator getFlowIterator() {
		return new TransientFlowIterator(this);
	}
	
	
	public static Host[] applyBlacklist(Host[] hosts, HashSet<Host> blacklist) {
		if (blacklist == null || blacklist.size() == 0)
			return hosts;
		Host[] result = new Host[hosts.length - blacklist.size()];
		int index = 0;
		for (int i=0; i<hosts.length; i++)
			if (!blacklist.contains(hosts[i]))
				result[index++] = hosts[i];
		return result;
	}
	
	
	/*public static Host[] getHostIndex(String pathToTraceFolder) {
		return getHostIndex(new TraceInfo(pathToTraceFolder));
	}*/
	
	
	/*public static Host[] getHostIndex(TraceInfo traceInfo) {
		return HostIndexCreator.getHostIndex(traceInfo);
	}*/

	public static Host[] getHostIndex(String pathToTraceFolder, FlowFilter filter) {
		return getHostIndex(new TraceInfo(pathToTraceFolder), filter);
	}
	
	
	public static Host[] getHostIndex(TraceInfo traceInfo, FlowFilter filter) {
		return HostIndexCreator.getHostIndex(traceInfo, filter);
	}

}

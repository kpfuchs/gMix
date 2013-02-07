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
package evaluation.traceParser.engine.fileReader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.util.Arrays;
import java.util.Vector;

import evaluation.traceParser.engine.TraceInfo;
import evaluation.traceParser.engine.dataStructure.Flow;
import evaluation.traceParser.engine.dataStructure.ExtendedHost;
import evaluation.traceParser.engine.dataStructure.FlowGroup;
import evaluation.traceParser.engine.dataStructure.Host;
import evaluation.traceParser.engine.filter.PacketFilterTester;
import evaluation.traceParser.scenarioExtractor.flowFilter.FlowFilter;
import evaluation.traceParser.statistics.HostComparator.*;
import framework.core.util.Util;


public class HostIndexCreator {
	
	/*public static Host[] getHostIndex(TraceInfo traceInfo) {
		Host[] index = readIndex(traceInfo, null);
		return index == null ? createIndex(traceInfo, null) : index;
	}*/
	
	
	public static Host[] getHostIndex(TraceInfo traceInfo, FlowFilter filter) {
		Host[] index = readIndex(traceInfo, filter);
		return index == null ? createIndex(traceInfo, filter) : index;
	}
	
	
	private static Host[] createIndex(TraceInfo traceInfo, FlowFilter filter) {
		long start = System.currentTimeMillis();
		System.out.println("creating index"); 
		try {
			Vector<ExtendedHost> allHosts = new Vector<ExtendedHost>(10000);
			FlowReader flowReader = new FlowReader(traceInfo, filter, false);
			while (flowReader.hasNextFlow()) {
				ExtendedHost actualHost = new ExtendedHost(filter);
				actualHost.hostId = flowReader.peekNextFlow().senderId;
				actualHost.offsetInTraceFile = flowReader.getOffsetOfNextFlow();
				actualHost.calculateStatistics(flowReader);
				allHosts.add(actualHost);
			}
			ExtendedHost[] hosts = allHosts.toArray(new ExtendedHost[0]); 
			calculateRanks(hosts);
			// serialize index:
			try {
				ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(getIndexPath(traceInfo, filter))));
				oos.write(Util.intToByteArray(hosts.length));
				Arrays.sort(hosts, new HostIdComparator());
				for (int i=0; i<hosts.length; i++)
					oos.writeObject((Host)hosts[i]);
				oos.close();
			} catch (Exception e) {
				throw new RuntimeException("ERROR: could not write index to " +getIndexPath(traceInfo, filter));
			}
			System.out.println("finished creating index (duration: " +(System.currentTimeMillis() - start) +"ms)"); 
			return hosts;
		} catch (FileNotFoundException e1) {
			throw new RuntimeException("ERROR: could not read index from " +getIndexPath(traceInfo, filter));
		} catch (IOException e1) {
			throw new RuntimeException("ERROR: could not read index from " +getIndexPath(traceInfo, filter));
		}
	}
	
	
	/*private static Host[] createIndex(TraceInfo traceInfo, FlowFilter filter) {
		long start = System.currentTimeMillis();
		System.out.println("creating index"); 
		try {
			Vector<ExtendedHost> allHosts = new Vector<ExtendedHost>(10000);
			ExtendedHost actualHost = null;
			FlowGroupReader flowGroupReader = new FlowGroupReader(traceInfo, filter);
			FlowGroup actualFlowGroup;
			while (flowGroupReader.hasNextFlowGroup()) {
				if (actualHost == null || flowGroupReader.peekNextFlowGroup().senderId != actualHost.hostId) { // new host
					if (actualHost != null) { // calculate and store statistics for current host
						actualHost.calculateStatistics();
						allHosts.add(actualHost);
					}
					actualHost = new ExtendedHost(filter);
					actualHost.hostId = flowGroupReader.peekNextFlowGroup().senderId;
					actualHost.offsetInTraceFile = flowGroupReader.getOffsetOfNextFlowGroup();
				}
				// we now know that the next flow group of the flowGroupReader belongs to the "actualHost"
				actualFlowGroup = flowGroupReader.readFlowGroup();
				actualHost.flowGroups.add(actualFlowGroup);
				if (actualHost.firstAction > actualFlowGroup.start)
					actualHost.firstAction = actualFlowGroup.start;
				if (actualHost.lastAction > actualFlowGroup.end)
					actualHost.lastAction = actualFlowGroup.end;
			}
			if (actualHost != null) { // don't forget about last host...
				actualHost.calculateStatistics();
				allHosts.add(actualHost); 
			}
			ExtendedHost[] hosts = allHosts.toArray(new ExtendedHost[0]); 
			calculateRanks(hosts);
			// serialize index:
			try {
				ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(getIndexPath(traceInfo, filter))));
				oos.write(Util.intToByteArray(hosts.length));
				Arrays.sort(hosts, new HostIdComparator());
				for (int i=0; i<hosts.length; i++)
					oos.writeObject((Host)hosts[i]);
				oos.close();
			} catch (Exception e) {
				throw new RuntimeException("ERROR: could not write index to " +getIndexPath(traceInfo, filter));
			}
			System.out.println("finished creating index (duration: " +(System.currentTimeMillis() - start) +"ms)"); 
			return hosts;
		} catch (FileNotFoundException e1) {
			throw new RuntimeException("ERROR: could not read index from " +getIndexPath(traceInfo, filter));
		} catch (IOException e1) {
			throw new RuntimeException("ERROR: could not read index from " +getIndexPath(traceInfo, filter));
		}
	}*/
	
	
/*	
	private static Host[] createIndex(TraceInfo traceInfo, FlowFilter filter) {
		long start = System.currentTimeMillis();
		System.out.println("creating index"); 
		try {
			Vector<ExtendedHost> allHosts = new Vector<ExtendedHost>(10000);
			ExtendedHost actualHost = null;
			//CountingFlowGroupIterator flowGroupIterator = new CountingFlowGroupIterator(traceInfo, filter);
			FlowGroupReader flowGroupReader = new FlowGroupReader(traceInfo, filter);
			FlowGroup actualFlowGroup;
			int currentUserId = Util.NOT_SET;
			long hostOffset = 0;
			while (flowGroupReader.hasNextFlowGroup()) {
				actualFlowGroup = flowGroupReader.readFlowGroup();
				//System.out.println("read flow group (hostid: " +actualFlowGroup.senderId +")"); 
				if (currentUserId != actualFlowGroup.senderId) { // new host
					if (actualHost != null) { // calculate statistics for current host
						actualHost.offsetInTraceFile = hostOffset;
						hostOffset = flowGroupReader.getOffsetOfLastFlowGroup();
						actualHost.calculateStatistics();
						allHosts.add(actualHost);
					}
					//System.out.println("creating new host data structure (sender id: " +actualFlowGroup.senderId +")"); 
					currentUserId = actualFlowGroup.senderId;
					actualHost = new ExtendedHost();
				}
				actualHost.flowGroups.add(actualFlowGroup);
				if (actualHost.firstAction > actualFlowGroup.start)
					actualHost.firstAction = actualFlowGroup.start;
				if (actualHost.lastAction > actualFlowGroup.end)
					actualHost.lastAction = actualFlowGroup.end;
			}
			if (actualHost != null) { // don't forget about last host...
				actualHost.offsetInTraceFile = hostOffset;
				actualHost.calculateStatistics();
				allHosts.add(actualHost); 
			}
			ExtendedHost[] hosts = allHosts.toArray(new ExtendedHost[0]); 
			calculateRanks(hosts);
			// serialize index:
			try {
				ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(getIndexPath(traceInfo, filter))));
				oos.write(Util.intToByteArray(hosts.length));
				Arrays.sort(hosts, new HostIdComparator());
				for (int i=0; i<hosts.length; i++)
					oos.writeObject((Host)hosts[i]);
				oos.close();
			} catch (Exception e) {
				throw new RuntimeException("ERROR: could not write index to " +getIndexPath(traceInfo, filter));
			}
			System.out.println("finished creating index (duration: " +(System.currentTimeMillis() - start) +"ms)"); 
			return hosts;
		} catch (FileNotFoundException e1) {
			throw new RuntimeException("ERROR: could not read index from " +getIndexPath(traceInfo, filter));
		} catch (IOException e1) {
			throw new RuntimeException("ERROR: could not read index from " +getIndexPath(traceInfo, filter));
		}
	}
*/	
	
	// returns null if no index available
	private static Host[] readIndex(TraceInfo traceInfo, FlowFilter filter) {
		try {
			ObjectInputStream trace = new ObjectInputStream(new BufferedInputStream(new FileInputStream(getIndexPath(traceInfo, filter))));
			System.out.println("detected existing index file");
			int numberOfHosts = Util.forceReadInt(trace);
			Host[] hosts = new Host[numberOfHosts];
			for (int i=0; i<hosts.length; i++) {
				hosts[i] = (Host)trace.readObject();
				hosts[i].filter = filter;
			}
			return hosts;
		} catch (IOException e) {
			return null;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}
	
	
	private static String getIndexPath(TraceInfo traceInfo, FlowFilter filter) {
		String filterName = filter == null ? "No" : filter.getName();
		String filterVersion = filter == null ? "Filter" : filter.getVersion();
		return Util.removeFileExtension(traceInfo.getPathToTraceFile()) +"-" +filterName +filterVersion + ".gmi";
	}
	
	
	public static void calculateRanks(Host[] hosts) {
		System.out.println("calculating ranks"); 
		Arrays.sort(hosts, new NumberOfFlowsComparator());
		for (int i=0; i<hosts.length; i++)
			hosts[i].stat_rank_byNumberOfFlows = i;
		Arrays.sort(hosts, new OnlineTimeComparator());
		for (int i=0; i<hosts.length; i++)
			hosts[i].stat_rank_byOnlineTime = i;
		Arrays.sort(hosts, new AvgBytesPerSecComparator());
		for (int i=0; i<hosts.length; i++)
			hosts[i].stat_rank_byAvgBytesPerSec = i;
		Arrays.sort(hosts, new AvgRequestBytesPerSecComparator());
		for (int i=0; i<hosts.length; i++)
			hosts[i].stat_rank_byAvgRequestBytesPerSec = i;
		Arrays.sort(hosts, new AvgReplyBytesPerSecComparator());
		for (int i=0; i<hosts.length; i++)
			hosts[i].stat_rank_byAvgReplyBytesPerSec = i;
		Arrays.sort(hosts, new TotalBytesTransferredComparator());
		for (int i=0; i<hosts.length; i++)
			hosts[i].stat_rank_byTotalBytesTransferred = i;
		Arrays.sort(hosts, new RequestBytesTransferredComparator());
		for (int i=0; i<hosts.length; i++)
			hosts[i].stat_rank_byRequestBytesTransferred = i;
		Arrays.sort(hosts, new ReplyBytesTransferredComparator());
		for (int i=0; i<hosts.length; i++)
			hosts[i].stat_rank_byReplyBytesTransferred = i;
		Arrays.sort(hosts, new AvgNewFlowsPerSecComparator());
		for (int i=0; i<hosts.length; i++)
			hosts[i].stat_rank_byAvgNewFlowsPerSec = i;
		System.out.println("calculating ranks done"); 
	} 
	
	
	public static void testLoadFlowGroupsMechanism(String tracePath, FlowFilter filter) throws IOException {
		if (!Util.assertionsEnabled())
			throw new RuntimeException("assertions must be enabled for this test mechanism.\n(use vm parameter \"-ea\")"); 
		TraceInfo traceInfo = new TraceInfo(tracePath);
		String path = Util.removeFileExtension(traceInfo.getPathToTraceFile()) +".gmf";
		
		CountingBufferedReader br = new CountingBufferedReader(path);
		String line;
		String reconstr;
		while (true) {
			line = br.readLine();
			if (line == null)
				break;
			reconstr = Util.readLine(br.getPositionOfLastLine(), path);
			boolean equals = line.equals(reconstr);
			//System.out.println("last: " +br.getPositionOfLastLine() +": equals: " +equals); 
			assert equals: "line:      " +line +"\nreconstr.: " +reconstr;
		}
		br.close();
		System.out.println("CountingBufferedReader seems to be working"); 
		
		FlowReader fr = new FlowReader(traceInfo, filter, false);
		while (true) {
			Flow af = fr.readFlow();
			if (af == null)
				break;
			line = af.serialize();
			reconstr = Util.readLine(fr.getOffsetOfLastFlow(), path);
			boolean equals = line.equals(reconstr);
			//System.out.println("last: " +fr.getOffsetOfLastFlow() +", next: " +fr.getOffsetOfNextFlow() +": equals: " +equals); 
			assert equals: "\nline:      " +line +"\nreconstr.: " +reconstr;
		}
		fr.close();
		System.out.println("FlowReader seems to be working"); 
		
		FlowGroupReader fgr = new FlowGroupReader(traceInfo, filter);
		RandomAccessFile raf = new RandomAccessFile(path, "r");
		while (true) {
			FlowGroup fg = fgr.readFlowGroup();
			if (fg == null)
				break;
			boolean equals = true;
			raf.seek(fgr.getOffsetOfLastFlowGroup());
			BufferedReader reader = new BufferedReader(Channels.newReader(raf.getChannel(), "ISO-8859-1"));
			FlowGroupReader fgReader = new FlowGroupReader(reader, filter);
			FlowGroup fg2 = fgReader.readFlowGroup();
			for (int i=0; i<fg.flows.size(); i++) {
				if (!fg.flows.get(i).serialize().equals(fg2.flows.get(i).serialize()))
					equals = false;
			} 
			//System.out.println("last: " +fgr.getOffsetOfLastFlowGroup() +", size: " +fg.flows.size()  +", next: " +fgr.getOffsetOfNextFlowGroup() +", size: " +fg.flows.size() +"; equals: " +equals); 
			assert equals;
			//assert equals: "\nline:      " +line +"\nreconstr.: " +reconstr;
		}
		fr.close();
		raf.close();
		System.out.println("FlowGroupReader seems to be working"); 
		
		
		HostReader hr = new HostReader(traceInfo, filter);
		raf = new RandomAccessFile(path, "r");
		while (true) {
			Host ho = hr.readHost();
			if (ho == null)
				break;
			boolean equals = true;
			raf.seek(hr.getOffsetOfLastHost());
			BufferedReader reader = new BufferedReader(Channels.newReader(raf.getChannel(), "ISO-8859-1"));
			HostReader hReader = new HostReader(reader, filter);
			Host ho2 = hReader.readHost();
			for (int i=0; i<ho.flowGroups.size(); i++) {
				for (int j=0; j<ho.flowGroups.get(i).flows.size(); j++) {
					if (!ho.flowGroups.get(i).flows.get(j).serialize().equals(ho2.flowGroups.get(i).flows.get(j).serialize()))
						equals = false;
				}
			} 
			assert equals;
		}
		hr.close();
		raf.close();
		System.out.println("HostReader seems to be working"); 
		
		Host[] hosts = Host.getHostIndex(traceInfo, filter);
		HostReader hostIterator = new HostReader(new BufferedReader(new FileReader(Util.removeFileExtension(traceInfo.getPathToTraceFile()) +".gmf")), filter);
		for (int i=0; i<hosts.length; i++) {
			Host h = hostIterator.readHost();
			hosts[i].loadFlowGroups(tracePath);
			for (int j=0; j<h.flowGroups.size(); j++) {
				assert h.flowGroups != null: h.hostId;
				assert h.flowGroups.size() > 0: h.hostId;
				assert hosts[i].flowGroups != null: h.hostId;
				assert hosts[i].flowGroups.size() > 0: h.hostId;
				assert h.flowGroups.get(j).serialize().equals(hosts[i].flowGroups.get(j).serialize()): "not working:\n" 
						+"host: " +h.flowGroups.get(j).senderId +", flows: " +h.flowGroups.get(j).flows.size() + ", first flow:: " +h.flowGroups.get(j).flows.get(0) +", last flow: " +h.flowGroups.get(j).flows.get(h.flowGroups.get(j).flows.size()-1) +"\n" 
						+"host: " +hosts[i].flowGroups.get(j).senderId  +", flows: " +hosts[i].flowGroups.get(j).flows.size() + ", first flow: " +hosts[i].flowGroups.get(j).flows.get(0) +", last flow: " +hosts[i].flowGroups.get(j).flows.get(hosts[i].flowGroups.get(j).flows.size()-1) +"\n" 
						;//+"fg " +j +" (reader): " +h.flowGroups.get(j).serialize()  +"\n" 
						//+"fg " +j +" (array):  " +hosts[i].flowGroups.get(j).serialize();
			}
		} 
		System.out.println("everything seems to be working"); 
	}
	
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		testLoadFlowGroupsMechanism(PacketFilterTester.AUCK_8, null);
	}
	
}

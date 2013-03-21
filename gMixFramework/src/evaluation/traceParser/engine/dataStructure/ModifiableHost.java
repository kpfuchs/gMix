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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;

import evaluation.traceParser.engine.TraceInfo;
import evaluation.traceParser.scenarioExtractor.flowFilter.FlowFilter;
import evaluation.traceParser.statistics.HostSampleSource;


public class ModifiableHost extends Host {

	private static final long serialVersionUID = -3180501668371218665L;


	public ModifiableHost(FlowFilter filter) {
		super(filter);
	}
	
	
	public ModifiableHost(Host source) {
		super(source.filter);
		cloneHost(source, this);
	}
	
	
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
	public static ModifiableHost cloneHost(Host h) {
		ModifiableHost result = new ModifiableHost(h.filter);
		cloneHost(h, result);
		return result;
	}
	
	
	private static void cloneHost(Host source, ModifiableHost destination) {
		destination.hostId = source.hostId;
		destination.firstAction = source.firstAction;
		destination.lastAction = source.lastAction;
		destination.offsetInTraceFile = -1; // the modifiable host will get a new offset when the result trace gets written
		for (FlowGroup fg: source.flowGroups)
			destination.flowGroups.add(new FlowGroup(fg.serialize()));
		ExtendedHost.calculateStatistics(destination);
	}
	
	
	public void reassignHostId(int newHostId) {
		int flowIdCounter = 0;
		stopIfNoFlowGroups();
		this.hostId = newHostId;
		if (flowGroups != null && flowGroups.size() != 0)
			for (FlowGroup fg: flowGroups) {
				fg.senderId = newHostId;
				for (Flow f:fg.flows) {
					f.senderId = newHostId;
					f.flowId = flowIdCounter++;
				}
			} 	
	}
	
	
	public void resetStart(long newStart) {
		stopIfNoFlowGroups();
		long dif = -1 * (this.firstAction - newStart);
		this.firstAction += dif;
		this.lastAction += dif;
		for (FlowGroup fg: flowGroups)
			fg.resetStart(fg.start + dif);
		ExtendedHost.calculateStatistics(this);
	}
	
	
	/**
	 * removes any transactions of this host that start at least 
	 * cutOffAsOffsetFromFirstAction ms later than the first action of this host
	 * (see instance variable "firstAction")
	 * 
	 * @return returns the new length of this host (result is shorter than 
	 * "cutOffAsOffsetFromFirstAction" ms if there is no transaction after 
	 * "cutOffAsOffsetFromFirstAction" ms, i.e. returns the end of the last 
	 * transaction <= "cutOffAsOffsetFromFirstAction")
	 * returns -1 if cut is not possible (as the result would contain no 
	 * transactions). if -1 is returned, this ModifiableHost can still be used 
	 * (no changes are made).
	 */
	public long cutOffAfter(long cutOffAsOffsetFromFirstAction) {
		stopIfNoFlowGroups();
		long maxEnd = firstAction + cutOffAsOffsetFromFirstAction;
		if (maxEnd >= lastAction) // already short enough
			return lastAction - firstAction;
		int cutCounter = 0;
		for (int i=flowGroups.size()-1; i>=0; i--) { 
			FlowGroup fg = flowGroups.get(i);
			if (fg.start >= maxEnd) { // flow group starts later than (or equal to) max end -> drop whole flow group
				cutCounter++;
			} else if (fg.end > maxEnd) { // flow group starts before maxEnd and ends later than maxEnd -> try to cut flow group
				assert fg.start < maxEnd;
				boolean cutSuccessful = fg.cutOff(maxEnd);
				if (!cutSuccessful) { // drop whole flow group
					cutCounter++;
				} else {
					assert fg.end <= maxEnd: "" +fg.end +" > " +maxEnd;
					break;
				}
			} else {
				break;
			}
		} 
		if (cutCounter == flowGroups.size()) { // no flow groups left
			return -1;
		} else {
			for (int i=0; i<cutCounter; i++) // remove flow groups
				flowGroups.remove(flowGroups.size()-1);
			lastAction = flowGroups.get(flowGroups.size()-1).end;
			ExtendedHost.calculateStatistics(this);
			if ((lastAction - firstAction) == 7260352)
				System.out.println("hm"); 
			assert lastAction - firstAction <= cutOffAsOffsetFromFirstAction: "" +(lastAction - firstAction) + " > " +cutOffAsOffsetFromFirstAction;
			return lastAction - firstAction;
		}
	}
	
	
	/**
	 * removes offline phases from this host, i.e. searches for periods with 
	 * no connections/flows (see FlowGroup.startDelay) that are equal to or 
	 * longer than assumeOfflineInterval ms and shortens theses periods to a 
	 * length drawn from sampleSource (samples are chosen at random among all 
	 * flow groups with a FlowGroup.startDelay smaller than 
	 * assumeOfflineInterval)
	 */
	public void removeOfflinePhases(int assumeOfflineInterval, HostSampleSource sampleSource) {
		stopIfNoFlowGroups();
		long toShorten = 0;
		for (FlowGroup fg: flowGroups) {
			if (fg.startDelay >= assumeOfflineInterval) {
				int newDelay = sampleSource.drawRandomSample_userThinkTime(assumeOfflineInterval);
				assert newDelay < fg.startDelay;
				toShorten += fg.startDelay - newDelay;
				fg.startDelay = newDelay;
			}
			if (toShorten > 0)
				fg.resetStart(fg.start - toShorten);	
		}
		if (toShorten > 0) {
			lastAction = flowGroups.get(flowGroups.size()-1).end;
			ExtendedHost.calculateStatistics(this);
		}
	}
	
	
	/**
	 * adds the flow groups of the bypassed host to this host. the bypassed 
	 * value "delayInMs" will be used as delay between the current last action 
	 * of this host and the first action of the appended flow groups
	 * @param host
	 */
	public void concat(Host host, int delayInMs) {
		stopIfNoFlowGroups();
		long oldStart = host.firstAction;
		long newStart = this.lastAction + delayInMs;
		long dif = -1 * (oldStart - newStart);
		boolean isFirst = true;
		for (FlowGroup fg: host.flowGroups) {
			fg = new FlowGroup(fg.serialize()); // create copy to not modify original host
			if (isFirst) { // store new delay for first flow group (the delay of others will remain constant)
				isFirst = false;
				fg.startDelay = delayInMs;
			}
			fg.resetStart(fg.start + dif);
			this.flowGroups.add(fg);
		}
		this.lastAction = this.flowGroups.get(this.flowGroups.size()-1).end;
		reassignHostId(this.hostId);
		ExtendedHost.calculateStatistics(this);
	}
	
	
	/**
	 * adds the flow groups of the bypassed host to this host. the delay 
	 * between the current last action of this host and the first action of the 
	 * appended flow groups is randomly drawn from "sampleSource".
	 * @param host
	 */
	public void concat(Host host, HostSampleSource sampleSource) {
		stopIfNoFlowGroups();
		concat(host, sampleSource.drawRandomSample_userThinkTime());
	}
	
	
	/**
	 * adds the flow groups of the bypassed host to this host. the delay 
	 * between the current last action of this host and the first action of the 
	 * appended flow groups is randomly drawn from "sampleSource" with the 
	 * restriction that the drawn delay must be smaller than 
	 * "exclusiveUpperBound".
	 * @param host
	 */
	public void concat(Host host, HostSampleSource sampleSource, int exclusiveUpperBound) {
		stopIfNoFlowGroups();
		concat(host, sampleSource.drawRandomSample_userThinkTime(exclusiveUpperBound));
	}
	
	
	
	private void stopIfNoFlowGroups() {
		if (flowGroups == null || flowGroups.size() == 0)
			throw new RuntimeException("cannot access the flows of this host as it contains now flow groups.\nuse loadFlowGroups() to restore the original flow groups or create new modifiable flow groups manually before calling this method."); 
	}
	
	
	public void serialize(Writer writer) throws IOException {
		stopIfNoFlowGroups();
		if (flowGroups.get(0).senderId != this.hostId) // reassign host ids
			reassignHostId(hostId);
		for (FlowGroup fg: flowGroups) {
			for (Flow flow:fg.flows) {
				writer.append(flow.serialize());
				writer.append("\n");
			}
		}
	}
	
	
	@Override
	public void loadFlowGroups(String pathToTraceFolder) {
		throw new RuntimeException("cannot load the flow groups of a modifiable host from a trace file. this host only exists in ram"); 
	}
	
	
	@Override
	public void loadFlowGroups(TraceInfo traceInfo) {
		loadFlowGroups("");
	}
	
	
	@Override
	public void loadFlowGroups(RandomAccessFile raf) {
		loadFlowGroups("");
	}
	
}

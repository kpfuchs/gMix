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
import java.io.Writer;
import java.util.Vector;

import framework.core.util.Util;


public class FlowGroup {

	public int flowGroupId;
	public static int flowGroupIdCounter = 0;
	public int flowCounter = 0;
	
	public int senderId;
	
	/**
	 * start replaying this flow group "startDelay" ms after the previous flow 
	 * group
	 */
	public long startDelay;
	
	/**
	 * (start of) first transfer of this flow group; offset from start of trace 
	 * (it is NOT required to calculate start + startDelay to get the actual 
	 * start)
	 */
	public long start;
	
	/**
	 * (end of) last transfer of this flow group; offset from start of trace; 
	 * use "end - start" to calculate the duration of this flow group (does 
	 * not include the "startDelay")
	 */
	public long end;
	
	
	public Vector<Flow> flows;
	
	
	public FlowGroup(Flow firstFlow) {
		this.flowGroupId = flowGroupIdCounter++;
		this.flows = new Vector<Flow>();
		//firstFlow.flowId = flowCounter++;
		this.flows.add(firstFlow);
		this.senderId = firstFlow.senderId;
		this.start = firstFlow.startOfFlow;
		this.end = firstFlow.endOfFlow;
	}
	
	
	public FlowGroup(long startDelay, Flow firstFlow) {
		this(firstFlow);
		this.startDelay = startDelay;
	}
	
	
	public FlowGroup(String serializedFlowGroup) {
		this.flowGroupId = flowGroupIdCounter++;
		this.flows = new Vector<Flow>();
		init(serializedFlowGroup);
	}


	public void init(String serializedFlowGroup) {
		String[] columns = serializedFlowGroup.split("\\|");
		if (columns.length < 5)
			throw new RuntimeException("unrecognized trace file format: " +serializedFlowGroup);
		this.senderId = Integer.parseInt(columns[0]);
		this.start = Long.parseLong(columns[1]);
		this.startDelay = Long.parseLong(columns[2]);
		this.end = Long.parseLong(columns[3]);
		int numberOfFlows = columns.length - 4;
		for (int i=0; i<numberOfFlows; i++)
			flows.add(new Flow(columns[4 + i]));
	}
	
	
	public void resetStart(long newStart) {
		long dif = -1 * (this.start - newStart);
		this.start += dif;
		this.end += dif;
		for (Flow flow: flows)
			flow.resetStart(flow.startOfFlow + dif);
	}
	
	
	public long getEndOfLatestFlow() {
		assert flows != null && flows.size() != 0;
		long latest = 0;
		for (Flow flow:flows)
			if (flow.endOfFlow > latest)
				latest = flow.endOfFlow;
		return latest;
	}
	
	
	/**
	 * removes any flows and transactions of this flow group that start after 
	 * "maxEnd".
	 * 
	 * @return returns whether the cut off was successful (true) or not 
	 * (false). a not successful cut (false) means that the flow group could 
	 * not be cut to the desired length as the resulting flow group would 
	 * contain no more transactions
	 */
	public boolean cutOff(long maxEnd) {
		if (maxEnd >= this.end) { // already short enough
			//if (Util.assertionsEnabled())
			//	for (Flow f:flows)
			//		assert f.endOfFlow <= maxEnd;
			return true;
		}
		for (int i=flows.size()-1; i>=0; i--) {
			Flow flow = flows.get(i);
			if (flow.startOfFlow >= maxEnd) { // flow starts later than (or equal to) max end -> drop whole flow
				flows.remove(i);
			} else if (flow.endOfFlow > maxEnd) { // flow starts before maxEnd and ends later than maxEnd -> try to cut flow
				boolean cutSuccessful = flow.cutOff(maxEnd);
				if (!cutSuccessful) { // drop whole flow
					flows.remove(i);
				} else {
					assert flow.endOfFlow <= maxEnd;
				}
				//break;
			} else {
				assert flow.endOfFlow <= maxEnd;
				//break;
			}
		} 
		if (flows.size() == 0) { // no flows left
			return false;
		} else {
			end = Long.MIN_VALUE;
			for (Flow flow:flows)
				if (flow.endOfFlow > end)
					end = flow.endOfFlow;
			assert end <= maxEnd;
			return true;
		}
	}
	
	
	/*@Override
	public String toString() {
		return "flowGroup "+flowGroupId +": " +serialize();
	}*/
	
	
	public String serialize() {
		StringBuffer sb = new StringBuffer();
		serialize(sb);
		return sb.toString();
	}
	
	
	public StringBuffer serialize(StringBuffer bufferToAppend) {
		bufferToAppend.append(senderId +"|");
		bufferToAppend.append(start +"|");
		bufferToAppend.append(startDelay +"|");
		bufferToAppend.append(end +"|");
		//int ctr = 0; // reassign flow ids
		for (int i=0; i<flows.size(); i++) {
			Flow flow = flows.get(i);
			//flow.flowId = ctr++;
			bufferToAppend.append(flow.serialize());
			if (i<(flows.size()-1))
				bufferToAppend.append("|");
		}
		return bufferToAppend;
	}
	
	
	public void serialize(Writer destination) throws IOException {
		destination.write(senderId +"|");
		destination.write(start +"|");
		destination.write(startDelay +"|");
		destination.write(end +"|");
		//int ctr = 0; // reassign flow ids
		for (int i=0; i<flows.size(); i++) {
			Flow flow = flows.get(i);
			//flow.flowId = ctr++;
			destination.write(flow.serialize());
			if (i<(flows.size()-1))
				destination.write("|");
		}
	}
	
	
	/*@Override
	public boolean equals(Object obj) {
		FlowGroup fg = (FlowGroup)obj;
		if (	fg.senderId == senderId &&
				fg.startDelay == startDelay &&
				fg.start == start &&
				fg.end == end &&
				fg.flows.size() == flows.size()
			)
			return true;
		return false;
	}*/
	
	
	/*public static void sort(String inputFilePath, String outputFilePath) throws IOException {
		BufferedReader in = null;
		Writer resultTrace = null;
		try {
			in = new BufferedReader(new FileReader(inputFilePath));
			resultTrace = new BufferedWriter(new OutputStreamWriter(new DataOutputStream(new FileOutputStream(outputFilePath))));
			PriorityQueue<String> entries = new PriorityQueue<String>(1000, new FlowGroupComparator());
			String currentLine;
			while (true) {
				currentLine = in.readLine();
				if (currentLine == null)
					break;
				entries.add(currentLine);
			}
			in.close();
			while (true) {
				currentLine = entries.poll();
				if (currentLine == null)
					break;
				resultTrace.write(currentLine);
				resultTrace.write("\n");
			}
		} catch (IOException e) { // close reader + writer and forward exception
			try {
				if (in != null)
					in.close(); 
				if (resultTrace != null)
					resultTrace.close();
			} catch (Exception e1) {}
			throw e;
		}
		resultTrace.close();
	}*/
	
	
	/** 
	 * for fast access of fields of a serialized flow (without creating an instance)
	 * pos 0: senderId (type: int) (get value with Integer.parseInt(String))
	 * pos 1: start (type: long) (get value with Long.parseLong(String))
	 * pos 2: startDelay (type: long) (get value with Long.parseLong(String))
	 * pos 3: end (type: long) (get value with Long.parseLong(String))
	 * pos 3 + i: ith flow (type: Flow) (get value with new Flow(String))
	 */
	public static String extractField(int position, String serializedFlow) {
		return Util.extractField(position, "\\|", serializedFlow);
	}
	
}

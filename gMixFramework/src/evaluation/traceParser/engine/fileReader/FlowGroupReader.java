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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import evaluation.traceParser.engine.TraceInfo;
import evaluation.traceParser.engine.dataStructure.Flow;
import evaluation.traceParser.engine.dataStructure.FlowGroup;
import evaluation.traceParser.engine.filter.PacketFilterTester;
import evaluation.traceParser.scenarioExtractor.flowFilter.FlowFilter;
import framework.core.util.Util;


public class FlowGroupReader extends FlowReader {

	private FlowGroup nextFlowGroup;
	private boolean wasHasNextCalled = false;
	private long offsetOfLastFlowGroup = 0;
	private long offsetOfNextFlowGroup = 0;
	private long tmp_offsetOfLastFlowGroup = 0;
	private long tmp_offsetOfNextFlowGroup = 0;
	private FgFlowIterator lastFlowGroup = null;
	
	
	/*public FlowGroupReader(String pathToTraceFolder) throws FileNotFoundException, IOException {
		this(new TraceInfo(pathToTraceFolder), null);
	}*/
	
	
	public FlowGroupReader(String pathToTraceFolder, FlowFilter filter) throws FileNotFoundException, IOException {
		this(new TraceInfo(pathToTraceFolder), filter);
	}
	
	
	/*public FlowGroupReader(TraceInfo traceInfo) throws FileNotFoundException, IOException {
		this(traceInfo, null);
	}*/
	
	
	public FlowGroupReader(TraceInfo traceInfo, FlowFilter filter) throws FileNotFoundException, IOException {
		super(traceInfo, filter);
	}
	
	
	/*public FlowGroupReader(BufferedReader bufferedReader) throws FileNotFoundException, IOException {
		super(bufferedReader);
	}*/
	
	
	public FlowGroupReader(BufferedReader bufferedReader, FlowFilter filter) throws FileNotFoundException, IOException {
		super(bufferedReader, filter);
	}
	
	
	public boolean hasNextFlowGroup() throws IOException {
		return peekNextFlowGroup() != null;
	}
	
	
	public FlowGroup peekNextFlowGroup() throws IOException {
		if (!wasHasNextCalled) {
			try {
				this.nextFlowGroup = tryReadFlowGroupFromFile();
			} catch (IOException e) {
				e.printStackTrace();
				super.close();
				nextFlowGroup = null;
				return null;
			}
			wasHasNextCalled = true;
		}
		return this.nextFlowGroup;	
	}
	
	
	protected FlowGroup tryReadFlowGroupFromFile() throws IOException {
		if (super.peekNextFlow() == null) { // no more flows
			this.tmp_offsetOfLastFlowGroup = this.tmp_offsetOfNextFlowGroup;
			this.tmp_offsetOfNextFlowGroup = super.getOffsetOfNextFlow();
			return null;
		}
		// determine startDelay of flowGroup:
		boolean isFirstFlowGroupOfThisHost = lastFlowGroup == null || lastFlowGroup.hostId != super.peekNextFlow().senderId;
		long startDelay = isFirstFlowGroupOfThisHost ? 0 : super.peekNextFlow().startOfFlow - lastFlowGroup.endOfLatestFlow;
		// read flow group:
		FgFlowIterator flowIterator = new FgFlowIterator();
		Flow flow = flowIterator.next();
		this.tmp_offsetOfLastFlowGroup = this.tmp_offsetOfNextFlowGroup;
		FlowGroup result = new FlowGroup(startDelay, flow);
		while (flowIterator.hasNext()) {
			flow = flowIterator.next();
			result.flows.add(flow);
			//flow.flowId = result.flowCounter++;
			if (flow.endOfFlow > result.end)
				result.end = flow.endOfFlow;
		} 
		lastFlowGroup = flowIterator;
		this.tmp_offsetOfLastFlowGroup = this.tmp_offsetOfNextFlowGroup;
		this.tmp_offsetOfNextFlowGroup = super.getOffsetOfNextFlow();
		return result;
	}
	
	
	public FlowGroup readFlowGroup() throws IOException {
		if (!wasHasNextCalled)
			hasNextFlowGroup();
		wasHasNextCalled = false;
		this.offsetOfLastFlowGroup = this.tmp_offsetOfLastFlowGroup;
		this.offsetOfNextFlowGroup = this.tmp_offsetOfNextFlowGroup;
		return nextFlowGroup;
	}
	
	
	public long getOffsetOfLastFlowGroup() {
		return offsetOfLastFlowGroup;
	}
	
	
	public long getOffsetOfNextFlowGroup() {
		return offsetOfNextFlowGroup;
	}


	public void reset() {
		super.reset();
		this.nextFlowGroup = null;
		this.wasHasNextCalled = false;
		this.offsetOfLastFlowGroup = 0;
		this.offsetOfNextFlowGroup = 0;
		this.tmp_offsetOfLastFlowGroup = 0;
		this.tmp_offsetOfNextFlowGroup = 0;
	}
	
	
	public static void test(String tracePath, FlowFilter filter) throws IOException {
		TraceInfo traceInfo = new TraceInfo(tracePath);
		FlowGroupReader fgr = new FlowGroupReader(new BufferedReader(new FileReader(Util.removeFileExtension(traceInfo.getPathToTraceFile()) +".gmf")), filter);
		System.out.println("start reading host");
		long start = System.currentTimeMillis();
		fgr.tryReadFlowGroupFromFile();
		long dur = System.currentTimeMillis() - start;
		System.out.println("finished reading host (" +dur +"ms)");
	}
	
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 * @throws IOException 
	 */
	public static void main(String[] args) {
		try {
			test(PacketFilterTester.AUCK_8, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

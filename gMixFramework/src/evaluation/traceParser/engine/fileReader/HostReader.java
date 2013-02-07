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
import java.io.IOException;

import evaluation.traceParser.engine.TraceInfo;
import evaluation.traceParser.engine.dataStructure.FlowGroup;
import evaluation.traceParser.engine.dataStructure.Host;
import evaluation.traceParser.scenarioExtractor.flowFilter.FlowFilter;


public class HostReader extends FlowGroupReader {

	private Host nextHost;
	private boolean wasHasNextCalled = false;
	private long offsetOfLastHost = 0;
	private long offsetOfNextHost = 0;
	private long tmp_offsetOfLastHost = 0;
	private long tmp_offsetOfNextHost = 0;
	
	
	/*public HostReader(String pathToTraceFolder) throws FileNotFoundException, IOException {
		this(new TraceInfo(pathToTraceFolder), null);
	}*/
	
	
	public HostReader(String pathToTraceFolder, FlowFilter filter) throws FileNotFoundException, IOException {
		this(new TraceInfo(pathToTraceFolder), filter);
	}
	
	
	/*public HostReader(TraceInfo traceInfo) throws FileNotFoundException, IOException {
		this(traceInfo, null);
	}*/
	
	
	public HostReader(TraceInfo traceInfo, FlowFilter filter) throws FileNotFoundException, IOException {
		super(traceInfo, filter);
	}
	
	
	/*public HostReader(BufferedReader bufferedReader) throws FileNotFoundException, IOException {
		super(bufferedReader);
	}*/
	
	
	public HostReader(BufferedReader bufferedReader, FlowFilter filter) throws FileNotFoundException, IOException {
		super(bufferedReader, filter);
	}
	
	
	public boolean hasNextHost() throws IOException {
		return peekNextHost() != null;
	}
	
	
	public Host peekNextHost() throws IOException {
		if (!wasHasNextCalled) {
			try {
				this.nextHost = tryReadHostFromFile();
			} catch (IOException e) {
				e.printStackTrace();
				super.close();
				nextHost = null;
				return null;
			}
			wasHasNextCalled = true;
		}
		return nextHost;
	}
	
	
	protected Host tryReadHostFromFile() throws IOException {
		if (!super.hasNextFlowGroup()) {
			this.tmp_offsetOfLastHost = this.tmp_offsetOfNextHost;
			this.tmp_offsetOfNextHost = super.getOffsetOfNextFlowGroup();
			return null;
		} else {
			Host result = new Host(filter);
			this.tmp_offsetOfLastHost = this.tmp_offsetOfNextHost;
			FlowGroup flowGroup = super.readFlowGroup();
			result.hostId = flowGroup.senderId;
			result.flowGroups.add(flowGroup);
			result.firstAction = flowGroup.start;
			result.lastAction = flowGroup.end;
			if (super.peekNextFlowGroup() == null || super.peekNextFlowGroup().senderId != result.hostId) { // host with 1 flow group
				this.tmp_offsetOfNextHost = super.getOffsetOfNextFlowGroup();
				return result;
			} else {
				// for all flow groups of this host (note that source trace is ordered by senderId)
				while (super.peekNextFlowGroup() != null && super.peekNextFlowGroup().senderId == result.hostId) {
					FlowGroup fg = super.readFlowGroup();
					if (result.lastAction < fg.end)
						result.lastAction = fg.end;
					result.flowGroups.add(fg);
				}
				this.tmp_offsetOfNextHost = super.getOffsetOfNextFlowGroup();
				return result;
			}
		}
	}
	
	
	public Host readHost() throws IOException {
		if (!wasHasNextCalled)
			hasNextHost();
		wasHasNextCalled = false;
		this.offsetOfLastHost = this.tmp_offsetOfLastHost;
		this.offsetOfNextHost = this.tmp_offsetOfNextHost;
		return nextHost;
	}
	
	
	public long getOffsetOfLastHost() {
		return offsetOfLastHost;
	}
	
	
	public long getOffsetOfNextHost() {
		return offsetOfNextHost;
	}


	public void reset() {
		super.reset();
		this.nextHost = null;
		this.wasHasNextCalled = false;
		this.offsetOfLastHost = 0;
		this.offsetOfNextHost = 0;
		this.tmp_offsetOfLastHost = 0;
		this.tmp_offsetOfNextHost = 0;
	}
	
}

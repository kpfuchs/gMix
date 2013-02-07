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

import java.io.IOException;
import java.util.Vector;

import evaluation.traceParser.engine.dataStructure.Flow;
import evaluation.traceParser.engine.dataStructure.FlowGroup;
import evaluation.traceParser.engine.dataStructure.Host;


public class TransientFlowIterator implements FlowIterator {

	//private Host source;
	private Vector<FlowGroup> allFlowGroups;
	private int flowPointer = 0;
	private int flowGroupPointer = 0;
	
	
	public TransientFlowIterator(Host source) {
		if (source.flowGroups == null || source.flowGroups.size() == 0)
			throw new RuntimeException("no flow grops -> use host.loadFlowGroups()"); 
		//this.source = source;
		this.allFlowGroups = source.flowGroups;
	}
	
	
	@Override
	public boolean hasNextFlow() throws IOException {
		if (	flowGroupPointer >= allFlowGroups.size() || 
				(flowGroupPointer == (allFlowGroups.size()-1)) && 
				flowPointer > (allFlowGroups.get(flowGroupPointer).flows.size()-1))
			return false;
		return true;
	}

	
	@Override
	public Flow peekNextFlow() throws IOException {
		return allFlowGroups.get(flowGroupPointer).flows.get(flowPointer);
	}

	
	@Override
	public Flow readFlow() throws IOException {
		int tmp_flowPointer = this.flowPointer;
		int tmp_flowGroupPointer = this.flowGroupPointer;
		this.flowPointer++;
		if (this.flowPointer >= allFlowGroups.get(this.flowGroupPointer).flows.size()) {
			this.flowPointer = 0;
			this.flowGroupPointer++;
		}
		return allFlowGroups.get(tmp_flowGroupPointer).flows.get(tmp_flowPointer);
	}

	
	@Override
	public FlowGroupFlowIterator getFlowGroupFlowIterator() throws IOException {
		return new TfgFlowIterator();
	}
	
	
	private class TfgFlowIterator implements FlowGroupFlowIterator {

		private int flowGroupId;
		
		
		public TfgFlowIterator() {
			this.flowGroupId = flowGroupPointer;
		}
		
		
		@Override
		public boolean hasNext() throws IOException {
			return flowGroupId == flowGroupPointer;
		}

		
		@Override
		public Flow next() throws IOException {
			return readFlow();
		}
		
	}

}

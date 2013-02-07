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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.util.Vector;

import evaluation.traceParser.engine.Protocol;
import evaluation.traceParser.engine.TraceInfo;
import evaluation.traceParser.engine.converter.EndOfFlow;
import evaluation.traceParser.engine.converter.ToGMF;
import evaluation.traceParser.engine.converter.TransactionInfo;
import evaluation.traceParser.engine.dataStructure.Flow;
import evaluation.traceParser.engine.dataStructure.Flow.Restriction;
import evaluation.traceParser.scenarioExtractor.flowFilter.FlowFilter;
import framework.core.util.Util;


public class FlowReader extends CountingBufferedReader implements FlowIterator {

	protected FlowFilter filter;
	private Flow nextFlow;
	private boolean wasHasNextCalled = false;
	private long offsetOfLastFlow = 0;
	private long offsetOfNextFlow = 0;
	private long tmp_offsetOfLastFlow = 0;
	private long tmp_offsetOfNextFlow = 0;
	private long endOfLastFlowGroup = 0;
	//private int idOfLatestFinishedFlowOfLastFlowGroup = Util.NOT_SET;
	private int ownerOfLastFlowGroup = Util.NOT_SET;
	private final boolean calculateRestrictions;
	private EndOfFlow latestFinishedFlowOfLastFlowGroup = null;
	
	
	/*public FlowReader(String pathToTraceFolder) throws FileNotFoundException, IOException {
		this(new TraceInfo(pathToTraceFolder), null);
	}*/
	
	
	public FlowReader(String pathToTraceFolder, FlowFilter filter) throws FileNotFoundException, IOException {
		this(new TraceInfo(pathToTraceFolder), filter);
	}
	
	
	public FlowReader(String pathToTraceFolder, FlowFilter filter, boolean calculateRestrictions) throws FileNotFoundException, IOException {
		this(new TraceInfo(pathToTraceFolder), filter, calculateRestrictions);
	}
	
	/*public FlowReader(TraceInfo traceInfo) throws FileNotFoundException, IOException {
		this(traceInfo, null);
	}*/
	
	
	public FlowReader(TraceInfo traceInfo, FlowFilter filter) throws FileNotFoundException, IOException {
		super(getGmfFilePointer(traceInfo));
		this.filter = filter;
		this.calculateRestrictions = true;
	}
	
	
	public FlowReader(TraceInfo traceInfo, FlowFilter filter, boolean calculateRestrictions) throws FileNotFoundException, IOException {
		super(getGmfFilePointer(traceInfo));
		this.filter = filter;
		this.calculateRestrictions = calculateRestrictions;
	}
	
	
	/*public FlowReader(BufferedReader bufferedReader) throws FileNotFoundException, IOException {
		super(bufferedReader);
	}*/
	
	
	public FlowReader(BufferedReader bufferedReader, FlowFilter filter) throws FileNotFoundException, IOException {
		super(bufferedReader);
		this.filter = filter;
		this.calculateRestrictions = true;
	}
	
	
	public FlowReader(BufferedReader bufferedReader, FlowFilter filter, boolean calculateRestrictions) throws FileNotFoundException, IOException {
		super(bufferedReader);
		this.filter = filter;
		this.calculateRestrictions = calculateRestrictions;
	}
	
	
	
	/**
	 * returns the path to the created (or loaded if already existing) .gmf file
	 */
	protected static String getGmfFilePointer(TraceInfo traceInfo) {
		Util.displayWarningOnLowReservedMemory();
		String outputFile = Util.removeFileExtension(traceInfo.getPathToTraceFile()) + ".gmf";
		if (traceInfo.getTraceFormat() != Protocol.GMF) { // no .gmf selected -> detect/create .gmf
			// detect if .gmf file is already present: (but was not selected in traceInfo.txt, e.g. because it was created automatically last run)
			boolean alreadyPresent = false;
			try {
				FileInputStream trace = new FileInputStream(outputFile);
				trace.close();
				alreadyPresent = true;
				System.out.println("detected existing .gmf file"); 
			} catch (Exception e) {}
			if (!alreadyPresent) { // use PacketSource as source (will converter to .gmp if not present) and then use toGMF converter to create .gmf.
				System.out.println("extracting flows (converting to .gmf)"); 
				PacketSource packetSource = new PacketSource(traceInfo);
				new ToGMF(packetSource, traceInfo).convert();
				System.out.println("finished extracting flows (converting to .gmf)"); 
			}
		}
		return outputFile;
	}
	
	
	public boolean hasNextFlow() throws IOException {
		return peekNextFlow() != null;
	}
	
	
	public Flow peekNextFlow() throws IOException {
		if (!wasHasNextCalled) {
			try {
				this.nextFlow = tryReadFlowFromFile();
			} catch (IOException e) {
				e.printStackTrace();
				super.close();
				nextFlow = null;
				return null;
			}
			wasHasNextCalled = true;
		}
		return this.nextFlow;
	}
	
	
	protected Flow tryReadFlowFromFile() throws IOException {
		String serializedFlow = super.readLine();
		if (filter == null) {
			this.tmp_offsetOfLastFlow = this.tmp_offsetOfNextFlow;
			this.tmp_offsetOfNextFlow = super.getPositionOfNextLine();
			return serializedFlow == null ? null : new Flow(serializedFlow);
		} else {
			if (serializedFlow == null) {
				this.tmp_offsetOfLastFlow = this.tmp_offsetOfNextFlow;
				this.tmp_offsetOfNextFlow = super.getPositionOfNextLine();
				return null;
			}
			Flow flow = new Flow(serializedFlow);
			while (filter.filterFlow(flow)) {
				this.tmp_offsetOfNextFlow = super.getPositionOfNextLine();
				serializedFlow = super.readLine();
				if (serializedFlow == null) {
					this.tmp_offsetOfLastFlow = this.tmp_offsetOfNextFlow;
					this.tmp_offsetOfNextFlow = super.getPositionOfNextLine();
					return null;
				}
				flow = new Flow(serializedFlow);
			} 
			this.tmp_offsetOfLastFlow = this.tmp_offsetOfNextFlow;
			this.tmp_offsetOfNextFlow = super.getPositionOfNextLine();
			return flow;
		}
	}
	
	
	public Flow readFlow() throws IOException {
		if (!wasHasNextCalled)
			hasNextFlow();
		wasHasNextCalled = false;
		this.offsetOfLastFlow = this.tmp_offsetOfLastFlow;
		this.offsetOfNextFlow = this.tmp_offsetOfNextFlow;
		return nextFlow;
	}
	
	
	public FlowGroupFlowIterator getFlowGroupFlowIterator() throws IOException {
		return new FgFlowIterator();
	}
	
	
	@Override
	public final long getPositionOfLastLine() {
		return getOffsetOfLastFlow();
	}
	
	
	public long getOffsetOfLastFlow() {
		return offsetOfLastFlow;
	}
	
	
	public long getOffsetOfNextFlow() {
		return offsetOfNextFlow;
	}


	public void reset() {
		this.nextFlow = null;
		this.wasHasNextCalled = false;
		this.offsetOfLastFlow = 0;
		this.offsetOfNextFlow = 0;
		this.tmp_offsetOfLastFlow = 0;
		this.tmp_offsetOfNextFlow = 0;
	}
	
	
	public class FgFlowIterator implements FlowGroupFlowIterator {
		
		public int hostId;	
		public long startOffset;
		public long offsetOfNextFlowGroup;
		public long endOfLatestFlow = Util.NOT_SET;
		private long endOfCurrentFlowGroup = Long.MIN_VALUE;
		private Vector<EndOfFlow> openFlows = new Vector<EndOfFlow>(100);
		private EndOfFlow latestFinishedFlow = null;
		private boolean hasNext = false;
		private boolean wasHasNextCalled = false;
		private long lastRestriction = Long.MIN_VALUE;

		
		public FgFlowIterator() throws IOException {
			if (peekNextFlow() == null)
				throw new RuntimeException("cannot init FlowGroupFlowIterator as Flow(Group)Reader has no more flows"); 
			this.hostId = peekNextFlow().senderId;
			this.startOffset = getOffsetOfNextFlow();
		}
		
		
		public boolean hasNext() throws IOException {
			if (wasHasNextCalled)
				return hasNext;
			wasHasNextCalled = true;
			if (peekNextFlow() == null || peekNextFlow().senderId != hostId) { // no more flows at all || next flow belongs to another host
				hasNext = false;
				offsetOfNextFlowGroup = getOffsetOfNextFlow();
				return false;
			} else {
				// check if next flow belongs to a new flow group (due to user think time):
				if (openFlows.size() == 0) { // first flow
					hasNext = true;
					return true;
				} else { // not first flow -> drop finished flows
					long now = peekNextFlow().startOfFlow; // note that flows is ordered by flow.startOfFlow
					for (int i=0; i<openFlows.size(); i++) {
						EndOfFlow flow = openFlows.get(i);
						if (flow.endOfFlow <= now) {
							openFlows.remove(i);
							if (latestFinishedFlow == null || latestFinishedFlow.endOfFlow < flow.endOfFlow) 
								latestFinishedFlow = flow;
							i--;
						}
					}
					if (openFlows.size() == 0) { // end of flow group
						hasNext = false;
						offsetOfNextFlowGroup = getOffsetOfNextFlow();
						return false;
					} else { // still open flows -> NOT end of flow group
						hasNext = true;
						return true;
					}
				}
			}
		}
		
		
		public Flow next() throws IOException {
			if (!hasNext())
				throw new RuntimeException("no more flows in this flow group. use hasNext() to avoid this exception"); 
			wasHasNextCalled = false;
			Flow result = readFlow();
			if (calculateRestrictions) {
				if (openFlows.size() == 0 && ownerOfLastFlowGroup != result.senderId) { // first flow of current host
					result.restriction = Restriction.NONE;
					//System.out.println("flow " +result.flowId +" is not restricted at all (ownerOfLastFlowGroup: " +ownerOfLastFlowGroup +", current owner: " +result.senderId +")"); 
				} else { // not the first flow of the current host
					TransactionInfo blockingTransaction = EndOfFlow.getTransactionWithLatestReplyBefore(result.startOfFlow, openFlows);
					boolean isBlockedByTransaction = blockingTransaction != null;
					if (blockingTransaction != null && blockingTransaction.endOfRestrictingReply <= endOfLatestFlow) {
						//System.out.println("override: " +blockingTransaction.endOfRestrictingReply + " <= " +endOfLatestFlow); 
						isBlockedByTransaction = false;
					}
					if (isBlockedByTransaction) { // flow IS blocked by a reply of an open flow
						result.restriction = Restriction.NOT_BEFORE_END_OF_TRANSACTION;
						result.idOfRestrictingFlow = blockingTransaction.idOfRestrictingFlow;
						result.idOfRestrictingTransaction = blockingTransaction.arrayOffsetOfRestrictingTransaction;
						result.idOfRestrictingReply = blockingTransaction.arrayOffsetOfRestrictingReply;
						result.offsetFromRestriction = (int) (result.startOfFlow - blockingTransaction.endOfRestrictingReply);
						//System.out.println("flow " +result.flowId +" is restricted by reply " +blockingTransaction.arrayOffsetOfRestrictingReply +" of transaction " +blockingTransaction.arrayOffsetOfRestrictingTransaction +" of flow " +blockingTransaction.idOfRestrictingFlow +" -> offsetFromRestriction: " +result.offsetFromRestriction +" (" +result.startOfFlow +" - " +blockingTransaction.endOfRestrictingReply +")"); 
						assert blockingTransaction.endOfRestrictingReply >= lastRestriction;
						lastRestriction = blockingTransaction.endOfRestrictingReply;
					} else { // flow IS NOT blocked by a reply of an open flow -> use end of latest finished flow as restriction
						if (latestFinishedFlow == null) { // no flows finished yet -> use offset from end of last flow group or start of trace as delay
							if (ownerOfLastFlowGroup != result.senderId) { // no previous flow group -> use offset from start of trace as delay
								result.restriction = Restriction.SIMPLE_DELAY;
								result.offsetFromRestriction = (int)result.startOfFlow; // note that the trace always starts at 0
								//System.out.println("flow " +result.flowId +" is restricted by start of trace. offsetFromRestriction: " +result.offsetFromRestriction +"(" +result.startOfFlow +" - 0)");
								assert 0 >= lastRestriction;
								lastRestriction = 0;
							} else { // previous flow group PRESENT-> use offset from last flow group as delay
								result.restriction = Restriction.NOT_BEFORE_END_OF_OTHER_FLOW;
								result.idOfRestrictingFlow = latestFinishedFlowOfLastFlowGroup.flowId;
								result.offsetFromRestriction = (int) (result.startOfFlow - endOfLastFlowGroup);
								assert latestFinishedFlowOfLastFlowGroup.endOfFlow == endOfLastFlowGroup;
								//System.out.println("flow " +result.flowId +" is restricted by previous flow group only. idOfRestrictingFlow: " +result.idOfRestrictingFlow +", offsetFromRestriction: " +result.offsetFromRestriction +" (" +result.startOfFlow +" - " +endOfLastFlowGroup +")"); 
								assert endOfLastFlowGroup >= lastRestriction;
								lastRestriction = endOfLastFlowGroup;
							}
						} else { // latest finished flow is available -> use offset from latest finished flow
							result.restriction = Restriction.NOT_BEFORE_END_OF_OTHER_FLOW;
							result.idOfRestrictingFlow = latestFinishedFlow.flowId;
							result.offsetFromRestriction = (int) (result.startOfFlow - latestFinishedFlow.endOfFlow);
							//System.out.println("flow " +result.flowId +" is restricted by latest finished flow. idOfRestrictingFlow: " +result.idOfRestrictingFlow +", offsetFromRestriction: " +result.offsetFromRestriction +" (" +result.startOfFlow +" - " +latestFinishedFlow.endOfFlow +")"); 
							assert latestFinishedFlow.endOfFlow >= lastRestriction;
							lastRestriction = latestFinishedFlow.endOfFlow;
						}
					}
				}
			}
			openFlows.add(new EndOfFlow(result));
			if (result.endOfFlow > endOfLatestFlow)
				endOfLatestFlow = result.endOfFlow;
			if (result.endOfFlow > endOfCurrentFlowGroup)
				endOfCurrentFlowGroup = result.endOfFlow;
			if (!hasNext()) {
				endOfLastFlowGroup = endOfCurrentFlowGroup;
				latestFinishedFlowOfLastFlowGroup = latestFinishedFlow;
				ownerOfLastFlowGroup = result.senderId;
				//System.out.println("end of flow group"); 
			}
			return result;
		}
		
	}
	
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		try {
			testRestrictionMechanism();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} 
	
	
	public static void testRestrictionMechanism() throws IOException {
		FlowReader readerOfClient;
		try {
			RandomAccessFile raf = new RandomAccessFile("./inputOutput/global/traces/pcapTests/localCapture3/TestExtractorTest.gmf", "r");
			raf.seek(0);
			// use BufferedReader to read line instead of raf.readLine() for performance reasons:
			BufferedReader reader = new BufferedReader(Channels.newReader(raf.getChannel(), "ISO-8859-1"));
			readerOfClient = new FlowReader(reader, null);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		FlowGroupFlowIterator currentFlowGroup = null;
		while (readerOfClient.hasNextFlow()) {
			if (currentFlowGroup == null || !currentFlowGroup.hasNext()) {
				//System.out.println("next flow group");
				currentFlowGroup = readerOfClient.getFlowGroupFlowIterator();
			}
			currentFlowGroup.next();
		} 
		
	}
	
}

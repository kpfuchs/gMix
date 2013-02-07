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
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.channels.Channels;
import java.util.PriorityQueue;
import java.util.Vector;

import evaluation.traceParser.engine.Protocol;
import evaluation.traceParser.engine.TraceInfo;
import evaluation.traceParser.engine.comparator.TraceEntry;
import evaluation.traceParser.engine.fileReader.CountingBufferedReader;
import framework.core.util.Util;


public class Flow {

	public enum Restriction {NONE, SIMPLE_DELAY, NOT_BEFORE_END_OF_OTHER_FLOW, NOT_BEFORE_END_OF_TRANSACTION};
	public enum FlowDirection {UNKNOWN, TO_WAN, FROM_WAN}; // when captured on an internet gateway: who established the connection? a local host or one in the WAN?
	public static int flowIdCounter = 0;
	
	public int senderId;
	public int flowId;
	public long startOfFlow; // start of flow (as offset from startOftrace) (ms accuracy)
	public long endOfFlow; // end of flow (as offset from startOftrace) (ms accuracy)
	public int requestSize; // payload of highest level protocol only (e.g. HTTP)
	public int replySize; 
	public String senderAddress; // e.g. IPv4 or IPv6 address as string
	public String receiverAddress; // e.g. IPv4 or IPv6 address as string
	public int receiverID;
	public int senderPort; 
	public int receiverPort; // e.g. 80 for http
	public Protocol layer4protocol;
	public String protocolAsString;
	public FlowDirection flowDirection;
	
	public Vector<ExtendedTransaction> transactions = new Vector<ExtendedTransaction>();
	
	// not serialized:
	public Restriction restriction;
	public int idOfRestrictingFlow = Util.NOT_SET;
	public int idOfRestrictingTransaction = Util.NOT_SET;
	public int idOfRestrictingReply = Util.NOT_SET;
	public int offsetFromRestriction = Util.NOT_SET; // last flow, last restricting flow or last restricting transaction (depends on restriction)
	//public int notBeforeRestriction = Util.NOT_SET; // contains the flowId of the latest ENDING flow that must be completed before this flow can be replayed
	//public int offsetFromRestrictingFlow = Util.NOT_SET; // if notBeforeRestriction is given, this variable indicates the time that should be waited after the restricting flow is finished until this flow is replayed 
	//public int offsetFromLastFlow = Util.NOT_SET; // if notBeforeRestriction is NOT given, this variable indicates the time that should be waited after starting the replaying of the last flow until this flow is replayed 

	
	public Flow() {
		this.flowId = flowIdCounter++;
	}
	

	public Flow(RandomAccessFile raf, long offsetInTraceFile) {
		try {
			raf.seek(offsetInTraceFile);
			// use BufferedReader to read line instead of raf.readLine() for performance reasons:
			BufferedReader reader = new BufferedReader(Channels.newReader(raf.getChannel(), "ISO-8859-1"));
			String line = reader.readLine();
			init(line);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not load flows from " +raf); 
		}
	}
	
	
	public Flow(String serializedFlow) {
		init(serializedFlow);
	}

	
	public void init(String serializedFlow) {
		serializedFlow = Util.removeLineBreakAtEnd(serializedFlow);
		String[] columns = serializedFlow.split("#");
		if (columns.length < 15)
			throw new RuntimeException("unrecognized trace file format: " +serializedFlow);
		this.senderId = Integer.parseInt(columns[0]);
		this.flowId = Integer.parseInt(columns[1]);
		this.startOfFlow = Long.parseLong(columns[2]);
		this.endOfFlow = Long.parseLong(columns[3]);
		this.requestSize = Integer.parseInt(columns[4]);
		this.replySize = Integer.parseInt(columns[5]);
		this.senderAddress = columns[6];
		this.receiverAddress = columns[7];
		this.receiverID = Integer.parseInt(columns[8]);
		this.senderPort = Integer.parseInt(columns[9]);
		this.receiverPort = Integer.parseInt(columns[10]);
		this.layer4protocol = Protocol.valueOf(columns[11]);
		this.protocolAsString = columns[12];
		this.flowDirection = FlowDirection.valueOf(columns[13]);
		int numberOfTransactions = columns.length - 14;
		for (int i=0; i<numberOfTransactions; i++)
			transactions.add(new ExtendedTransaction(columns[14 + i]));
	}
	
	
	/** 
	 * returns whether this flow ends before the START of the bypassed flow or 
	 * not (not the end of the bypassed flow)
	 */
	public boolean endsBefore(Flow flow) {
		return this.endOfFlow <= flow.startOfFlow;
	}


	/** 
	 * returns whether this flow ends after (the end of) the bypassed flow or 
	 * not
	 */
	public boolean endsAfter(Flow flow) {
		return this.endOfFlow >= flow.endOfFlow;
	}

	
	/** 
	 * returns whether this flow start before the bypassed flow or not
	 */
	public boolean startsBefore(Flow flow) {
		return this.startOfFlow <= flow.startOfFlow;
	}
	
	
	/** 
	 * returns whether this flow starts after the END of the bypassed flow or 
	 * not (not the start of the bypassed flow)
	 */
	public boolean startsAfter(Flow flow) {
		return this.startOfFlow >= flow.endOfFlow;
	}
	
	
	/** 
	 * returns whether this and the bypassed flow were open at the same time.
	 */
	public boolean isOverlapping(Flow flow) {
		if (startsAfter(flow) || endsBefore(flow))
			return false;
		return true;
	}
	
	
	public Vector<Flow> getOverlapping(Vector<Flow> flowsToCompare) {
		Vector<Flow> overlapping = new Vector<Flow>();
		for (Flow flow: flowsToCompare)
			if (isOverlapping(flow))
				overlapping.add(flow);
		return overlapping;
	}
	
	
	public void resetStart(long newStart) {
		long dif = -1 * (this.startOfFlow - newStart);
		this.startOfFlow += dif;
		this.endOfFlow += dif;
		for (ExtendedTransaction transaction: transactions)
			transaction.resetStart(transaction.startOfRequest + dif);
	}
	
	
	/**
	 * removes any transactions of this flow that start after "maxEnd".
	 * 
	 * @return returns whether the cut off was successful (true) or not 
	 * (false). a not successful cut (false) means that the flow could 
	 * not be cut to the desired length as the resulting flow would 
	 * contain no more transactions
	 */
	public boolean cutOff(long maxEnd) {
		if (maxEnd >= this.endOfFlow) // already short enough
			return true;
		int cutCounter = 0;
		int newRequestSize = requestSize;
		int newReplySize = replySize;
		boolean fill = false;
		for (int i=transactions.size()-1; i>=0; i--) {
			ExtendedTransaction transaction = transactions.get(i);
			if (transaction.startOfRequest >= maxEnd) { // transaction starts later than (or equal to) max end -> drop whole transaction
				cutCounter++;
				newRequestSize -= transaction.requestSize;
				newReplySize -= transaction.getTotalReplySize();
			} else if (transaction.getTimestampOfLastActivity() > maxEnd) { // transaction starts before maxEnd and ends later than maxEnd -> try to cut transaction
				boolean cutSuccessful = transaction.cutOff(maxEnd);
				if (!cutSuccessful) { // drop whole transaction
					cutCounter++;
					newRequestSize -= transaction.requestSize;
					newReplySize -= transaction.getTotalReplySize();
				}
				break;
			} else { // no data in the flow a that time
				fill = true;
				break;
			}
		} 
		if (cutCounter == transactions.size()) { // no transactions left
			return false;
		} else {
			for (int i=0; i<cutCounter; i++) // remove transactions
				transactions.remove(transactions.size()-1);
			endOfFlow = fill ? maxEnd : transactions.get(transactions.size()-1).getTimestampOfLastActivity();
			requestSize = newRequestSize;
			replySize = newReplySize;
			return true;
		}
	}
	
	
	public static Flow loadFlow(String pathToTraceFolder, long offset) {
		return loadFlow(new TraceInfo(pathToTraceFolder), offset);
	}
	
	
	public static Flow loadFlow(TraceInfo traceInfo, long offset) {
		String path = Util.removeFileExtension(traceInfo.getPathToTraceFile()) +".gmf";
		Flow af = null;
		try {
			RandomAccessFile raf = new RandomAccessFile(path, "r");
			af = loadFlow(raf, offset);
			raf.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not load flows from " +path); 
		}
		return af;
	}
	
	
	public static Flow loadFlow(RandomAccessFile raf, long offset) {
		return new Flow(raf, offset);
	}
	
	
	@Override
	public String toString() {
		return "flow "+flowId +": " +serialize();
	}
	
	
	public String serialize() {
		StringBuffer sb = new StringBuffer();
		serialize(sb);
		return sb.toString();
	}
	
	
	public StringBuffer serialize(StringBuffer bufferToAppend) {
		bufferToAppend.append(senderId +"#");
		bufferToAppend.append(flowId +"#");
		bufferToAppend.append(startOfFlow +"#");
		bufferToAppend.append(endOfFlow +"#");
		bufferToAppend.append(requestSize +"#");
		bufferToAppend.append(replySize +"#");
		bufferToAppend.append(senderAddress +"#");
		bufferToAppend.append(receiverAddress +"#");
		bufferToAppend.append(receiverID +"#");
		bufferToAppend.append(senderPort +"#");
		bufferToAppend.append(receiverPort +"#");
		bufferToAppend.append(layer4protocol +"#");
		bufferToAppend.append(protocolAsString +"#");
		bufferToAppend.append(flowDirection.toString() +"#");
		for (int i=0; i<transactions.size(); i++) {
			ExtendedTransaction message = transactions.get(i);
			bufferToAppend.append(message.serializeExtended());
			if (i<(transactions.size()-1))
				bufferToAppend.append("#");
		}
		return bufferToAppend;
	}
	
	
	public void serialize(Writer destination) throws IOException {
		destination.write(senderId +"#");
		destination.write(flowId +"#");
		destination.write(startOfFlow +"#");
		destination.write(endOfFlow +"#");
		destination.write(requestSize +"#");
		destination.write(replySize +"#");
		destination.write(senderAddress +"#");
		destination.write(receiverAddress +"#");
		destination.write(receiverID +"#");
		destination.write(senderPort +"#");
		destination.write(receiverPort +"#");
		destination.write(layer4protocol +"#");
		destination.write(protocolAsString +"#");
		destination.write(flowDirection.toString() +"#");
		for (int i=0; i<transactions.size(); i++) {
			ExtendedTransaction message = transactions.get(i);
			message.serializeExtended(destination);
			if (i<(transactions.size()-1))
				destination.write("#");
		}
	}
	
	
	public static void sort(String inputFilePath, String outputFilePath) throws IOException {
		CountingBufferedReader in = null;
		Writer resultTrace = null;
		RandomAccessFile raf = null;
		try {
			// read and sort index:
			in = new CountingBufferedReader(inputFilePath);
			resultTrace = new BufferedWriter(new OutputStreamWriter(new DataOutputStream(new FileOutputStream(outputFilePath))));
			PriorityQueue<TraceEntry> entries = new PriorityQueue<TraceEntry>(1000);
			String currentLine;
			while (true) {
				currentLine = in.readLine();
				if (currentLine == null)
					break;
				entries.add(new TraceEntry(currentLine, in.getPositionOfLastLine()));
			}
			in.close();
			// write result trace (restore flows from source trace through index and write to dest. trace):
			raf = new RandomAccessFile(inputFilePath, "r");
			int flowCounter = 0;
			while (true) {
				TraceEntry currentEntry = entries.poll();
				if (currentEntry == null)
					break;
				Flow af = loadFlow(raf, currentEntry.offset);
				af.flowId = flowCounter++;
				af.serialize(resultTrace);
				resultTrace.write("\n");
			}
			raf.close();
		} catch (IOException e) { // close reader + writer and forward exception
			try {
				if (in != null)
					in.close(); 
				if (raf != null)
					raf.close(); 
				if (resultTrace != null)
					resultTrace.close();
			} catch (Exception e1) {}
			throw e;
		}
		resultTrace.close();
	}

	
	public void reuse() {
		this.flowId = 0;
		this.senderId = 0; 
		this.startOfFlow = 0;
		this.requestSize = 0; 
		this.replySize = 0; 
		this.senderAddress = null;
		this.receiverAddress = null;
		this.receiverID = 0; 
		this.senderPort = 0; 
		this.receiverPort = 0; 
		this.layer4protocol = null;
		this.protocolAsString = null;
		this.flowDirection = null; 
		this.restriction = null;
		this.idOfRestrictingFlow = Util.NOT_SET;
		this.idOfRestrictingTransaction = Util.NOT_SET;
		this.offsetFromRestriction = Util.NOT_SET;
		this.transactions = new Vector<ExtendedTransaction>();
	}
	
	
	/** 
	 * for fast access of fields of a serialized flow (without creating an instance)
	 * pos 0: senderId (type: int) (get value with Integer.parseInt(String))
	 * pos 1: flowId (type: int) (get value with Integer.parseInt(String))
	 * pos 2: startOfFlow (type: long) (get value with Long.parseLong(String))
	 * pos 3: endOfFlow (type: long) (get value with Long.parseLong(String))
	 * pos 4: requestSize (type: int) (get value with Integer.parseInt(String))
	 * pos 5: replySize (type: int) (get value with Integer.parseInt(String))
	 * pos 6: senderAddress (type: String)
	 * pos 7: receiverAddress (type: String)
	 * pos 8: receiverID (type: int) (get value with Integer.parseInt(String))
	 * pos 9: senderPort (type: int) (get value with Integer.parseInt(String))
	 * pos 10: receiverPort (type: int) (get value with Integer.parseInt(String))
	 * pos 11: layer4protocol (type: Protocol) (get value with Protocol.valueOf(String))
	 * pos 12: protocolAsString (type: String)
	 * pos 13: flowDirection (type: FlowDirection) (get value with FlowDirection.valueOf(String))
	 * pos 14 + i: ith transaction (type: ExtendedTransaction) (get value with new ExtendedTransaction(String))
	 */
	public static String extractField(int position, String serializedFlow) {
		return Util.extractField(position, "#", serializedFlow);
	}
	
}

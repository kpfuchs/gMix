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
import java.util.Arrays;

import framework.core.util.Util;


/**
 * Transaction that additionally stores absolute timestamps of sending 
 * times. Offers higher accuracy and simpler access then direct use of 
 * Transaction.
 * This class is used by Flow.java to store transactions. 
 */
public class ExtendedTransaction extends Transaction {

	// there is no "private long startOfTrace;" -> use the flow object that this ExtendedTransaction belongs to for that information
	public long startOfRequest = Util.NOT_SET; // offset from start of trace
	public long endOfRequest = Util.NOT_SET; // offset from start of trace
	public long[] startsOfReplies = null; // start of i-th reply; offsets from start of TRACE (NOT from start of flow); last entry represents the "end of transaction" (use for calculation of delays between transactions)
	public long[] endsOfReplies = null; // end of i-th reply; offsets from start of TRACE


	/**
	 * Used by by evaluation.traceParser.extractors.
	 * @param serializedFlow
	 */
	public ExtendedTransaction(String serializedTransaction) {
		super();
		String[] fields = serializedTransaction.split(";");
		if (fields.length < 8)
			throw new RuntimeException("unknown format (not a serialized ExtendedTransaction): " +serializedTransaction); 
		super.transactionId = idCounter++; 
		super.sendDelay = Integer.parseInt(fields[0]);
		this.startOfRequest = Long.parseLong(fields[1]);
		this.endOfRequest = Long.parseLong(fields[2]);
		super.requestSize = Integer.parseInt(fields[3]);
		super.serverId = Integer.parseInt(fields[4]);
		int replyRecords = (fields.length - 5) / 3;
		if (replyRecords > 0) {
			this.startsOfReplies = new long[replyRecords];
			this.endsOfReplies = new long[replyRecords];
			super.distinctReplySizes = new int[replyRecords];
			for (int i=0; i<replyRecords; i++) {
				this.startsOfReplies[i] = Long.parseLong(fields[5+i*3]);
				this.endsOfReplies[i] = Long.parseLong(fields[6+i*3]);
				super.distinctReplySizes[i] = Integer.parseInt(fields[7+i*3]);
				if (super.distinctReplySizes[i] == 0) {
					this.startsOfReplies = null;
					this.endsOfReplies = null;
					super.distinctReplySizes = null;
					break;
				}
			}
		}
		calculateDelayTrace();
	}

	
	/**
	 * Used by TCPflowExtractor to create ExtendedTransactions.
	 */
	public ExtendedTransaction(	int sendDelay,
										long startOfRequest,
										long endOfRequest,
										int requestSize,
										int serverId,
										long[] startReplyOffsets,
										long[] endReplyOffsets,
										int[] replySizes
			) {
		super();
		super.sendDelay = sendDelay;
		this.startOfRequest = startOfRequest;
		this.endOfRequest = endOfRequest;
		super.serverId = serverId;
		super.requestSize = requestSize;
		this.startsOfReplies = startReplyOffsets;
		this.endsOfReplies = endReplyOffsets;
		super.distinctReplySizes = replySizes;
		calculateDelayTrace();
	}
	
	
	/**
	 * offset from start of trace, NOT start of flow
	 */
	public long getTimestampOfLastActivity() {
		if (!containsReplies())
			return endOfRequest;
		else
			return endsOfReplies[endsOfReplies.length-1];
		
	}
	
	
	/**
	 * Calculates the delays that will be needed to replay this 
	 * ExtendedTransaction (source: replyOffsets array bypassed in
	 * constructor).
	 * The parameter firstDelayInMilliSec will be set as "sendDelay" (= client 
	 * shall start with sending this transaction after sendDelay ms). The 
	 * firstDelayInMilliSec can for example be the delay between this 
	 * transaction and the previous one.
	 */
	public void calculateDelayTrace(int firstDelayInMilliSec) {
		super.sendDelay = firstDelayInMilliSec;
		calculateDelayTrace();
	}
	
	
	private void calculateDelayTrace() {
		if (!containsReplies()) { // request only. no replies
			super.totalReplySize = 0;
			super.simplexReplyDelay = 0;
		} else {
			super.totalReplySize = 0;
			for (int i=0; i<super.distinctReplySizes.length; i++)
				super.totalReplySize += super.distinctReplySizes[i];
			super.simplexReplyDelay = (int) (endsOfReplies[endsOfReplies.length-1] - startOfRequest); // delta between the p.o.t. (point of time) the last packet of the request  left the client and the p.o.t. the last packet of the reply was received  by the client.
			assert endsOfReplies[endsOfReplies.length-1] >= endOfRequest;
			//System.out.println(endsOfReplies[endsOfReplies.length-1] + " - " +endOfRequest); 
			super.distinctReplyDelays = new int[startsOfReplies.length];
			super.distinctReplyDelays[0] = (int)(startsOfReplies[0] - startOfRequest); // time delta between the start of sending the first request (= timestamp first request packet) and the the start of the first reply reached the client (timestamp of first reply packet)
			for (int i=1; i<super.distinctReplyDelays.length; i++)
				super.distinctReplyDelays[i] = (int)(startsOfReplies[i] - endsOfReplies[i-1]); // time delta between the ith reply is fully received (= timestamp of last reply packet for reply i)  and the the start of the i-1th reply reached the client (timestamp of first i-1 reply packet)
		}
	}
	
	
	public void resetStart(long newStart) {
		long dif = -1 * (this.startOfRequest - newStart);
		this.startOfRequest += dif;
		this.endOfRequest += dif;
		if (!containsReplies()) // no replies
			return;
		for (int i=0; i<distinctReplySizes.length; i++) {
			startsOfReplies[i] += dif; 
			endsOfReplies[i] += dif;
		}
	}
	
	
	/**
	 * removes any replies of this transaction that finish later than "maxEnd".
	 * 
	 * @return returns whether the cut off was successful (true) or not 
	 * (false). a not successful cut (false) means that the transaction could 
	 * not be cut to the desired length as the resulting transaction would 
	 * contain no more data (i.e. the request is not finished before "maxEnd")
	 */
	public boolean cutOff(long maxEnd) {
		if (maxEnd >= this.getTimestampOfLastActivity()) // already short enough
			return true;
		if (this.endOfRequest > maxEnd)
			return false;
		int cutCounter = 0;
		for (int i=distinctReplySizes.length-1; i>=0; i--) {
			if (endsOfReplies[i] > maxEnd) // reply ends after maxEnd -> drop the reply
				cutCounter++;
			else
				break;
		} 
		int toKeep = distinctReplySizes.length - cutCounter;
		if (toKeep == 0) { // no replies left
			distinctReplySizes = null;
			startsOfReplies = null;
			endsOfReplies = null;
			calculateDelayTrace();
			return true;
		} else {
			distinctReplySizes = Arrays.copyOf(distinctReplySizes, toKeep);
			startsOfReplies = Arrays.copyOf(startsOfReplies, toKeep);
			endsOfReplies = Arrays.copyOf(endsOfReplies, toKeep);
			calculateDelayTrace();
			return true;
		}
	}
	
	
	@Override
	public String toString() {
		return "transaction "+transactionId +"; " +serializeExtended();
	}
	
	
	@Override
	public String serialize() {
		throw new RuntimeException("use serializeExtended() or serializeNoneExtended()");
	}
	
	
	public String serializeExtended() {
		StringBuffer sb = new StringBuffer();
		serializeExtended(sb);
		return sb.toString();
	}
	
	
	public String serializeNoneExtended() {
		return super.serialize();
	}
	
	
	@Override
	public StringBuffer serialize(StringBuffer bufferToAppend) {
		throw new RuntimeException("use serializeExtended() or serializeNoneExtended()"); 
	}

	
	public StringBuffer serializeExtended(StringBuffer bufferToAppend) {
		bufferToAppend.append(Integer.toString(sendDelay));
		bufferToAppend.append(";");
		bufferToAppend.append(Long.toString(startOfRequest));
		bufferToAppend.append(";");
		bufferToAppend.append(Long.toString(endOfRequest));
		bufferToAppend.append(";");
		bufferToAppend.append(Integer.toString(requestSize));
		bufferToAppend.append(";");
		bufferToAppend.append(Integer.toString(serverId));
		if (!containsReplies())
			bufferToAppend.append(";0;0;0");
		else
			for (int i=0; i<super.distinctReplySizes.length; i++) {
				bufferToAppend.append(";");
				bufferToAppend.append(Long.toString(startsOfReplies[i]));
				bufferToAppend.append(";");
				bufferToAppend.append(Long.toString(endsOfReplies[i]));
				bufferToAppend.append(";");
				bufferToAppend.append(Integer.toString(super.distinctReplySizes[i]));
			}
		return bufferToAppend;
	}
	
	
	public StringBuffer serializeNoneExtended(StringBuffer bufferToAppend) {
		return super.serialize(bufferToAppend);
	}
	
	
	@Override
	public void serialize(Writer destination) throws IOException {
		throw new RuntimeException("use serializeExtended() or serializeNoneExtended()"); 
	}
	
	
	public void serializeExtended(Writer destination) throws IOException {
		destination.write(Integer.toString(sendDelay));
		destination.write(";");
		destination.write(Long.toString(startOfRequest));
		destination.write(";");
		destination.write(Long.toString(endOfRequest));
		destination.write(";");
		destination.write(Integer.toString(requestSize));
		destination.write(";");
		destination.write(Integer.toString(serverId));
		if (!containsReplies())
			destination.write(";0;0;0");
		else
			for (int i=0; i<super.distinctReplySizes.length; i++) {
				destination.write(";");
				destination.write(Long.toString(startsOfReplies[i]));
				destination.write(";");
				destination.write(Long.toString(endsOfReplies[i]));
				destination.write(";");
				destination.write(Integer.toString(super.distinctReplySizes[i]));
			}
	}
	
	
	public void serializeNoneExtended(Writer destination) throws IOException {
		super.serialize(destination);
	}
	
	
	public long getStartOfRequest() {
		return startOfRequest;
	}


	public void setStartOfRequest(long startOfRequest) {
		this.startOfRequest = startOfRequest;
	}
	
	
	public long getEndOfRequest() {
		return endOfRequest;
	}


	public void setEndOfRequest(long endOfRequest) {
		this.endOfRequest = endOfRequest;
	}


	public long[] getStartReplyOffsets() {
		return startsOfReplies;
	}


	public void setStartReplyOffsets(long[] startsOfReplies) {
		this.startsOfReplies = startsOfReplies;
	}
	
	
	public long[] getEndReplyOffsets() {
		return endsOfReplies;
	}


	public void setEndReplyOffsets(long[] endsOfReplies) {
		this.endsOfReplies = endsOfReplies;
	}

}

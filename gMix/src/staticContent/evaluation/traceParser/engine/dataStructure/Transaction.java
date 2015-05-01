/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
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
 *******************************************************************************/
package staticContent.evaluation.traceParser.engine.dataStructure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.security.SecureRandom;

import staticContent.framework.util.FragmentedMessage;
import staticContent.framework.util.Util;


/**
 * A Transaction consists of exactly one request and zero or more replies (cf. 
 * [1]). This data structure (class) can be used to replay Transactions in 
 * simulation or emulation setups. It contains data required on client- and 
 * server-side to determine the size of and delay between requests and replies.
 * 
 * supported modes (cf. [1]):
 * MODE I:   SIMPLEX_OPEN_LOOP (for SIMPLEX simulation scenarios WITH UNbounded 
 *           communication links (unlimited bandwidth; 0 latency))
 * MODE II:  SIMPLEX_WITH_FEEDBACK (for SIMPLEX simulations that takes feedback 
 *           from the communication links into account, e.g. TCP congestion 
 *           control effects etc. (in fact anything else than unlimited 
 *           bandwidth and 0 latency))
 * MODE III: DUPLEX (for DUPLEX simulations that takes feedback 
 *           from the communication links into account, e.g. TCP congestion 
 *           control effects etc.)
 *           
 * [1] Karl-Peter Fuchs, Dominik Herrmann, and Hannes Federrath: "Generating 
 * Realistic Application Workloads for Mix-Based Systems for Controllable, 
 * Repeatable and Usable Experimentation", IFIP SEC 2013
 */
public class Transaction {
	
	protected static SecureRandom random = new SecureRandom();
	
	
	/** 
	 * counter for the transactionId variable
	 */
	protected static int idCounter = 0;
		
	/** unique id for this transaction */
	protected int transactionId = Util.NOT_SET;
	
	/** id of the server who shall perform this transaction */
	protected int serverId = Util.NOT_SET;
	
	/** 
	 * client shall start this transaction (i.e. send the request (see below)) 
	 * after "sendDelay" ms. 
	 * Note: this value is referred to as "user think time" in many papers.
	 * Note: this variable corresponds to "T1-T0" in Fig.5 of [1] (or "T13-T12",
	 * if we assume that this is the second transaction of Fig.5 in [1])
	 * 
	 * Note: this variable is relevant for all modes
	 * Note: used on client-side only
	 */ 
	protected int sendDelay = Util.NOT_SET;
	
	/** 
	 * size of the request (i.e. the message from the client to the server) 
	 * in byte (0 if server shall send first message)
	 * 
	 * Note: this variable corresponds to "A_1" and "A_2" in Fig.3 of [1]
	 * Note: this variable is relevant for all modes
	 * Note: used on client-side only
	 */
	protected int requestSize = Util.NOT_SET;
	
	/** 
	 * size of all reply messages in total (0 if no message from server to 
	 * client shall be sent)
	 * 
	 * Note: this variable corresponds to "B_1 + B_2" in Fig.3 of [1]
	 * Note (informational): this variable is the sum of all individual sizes 
	 * stored in the "distinctReplySizes"-array below
	 * 
	 * Note: this variable is relevant for DUPLEX-Mode only
	 * Note: used on server/mix-side only
	 */
	protected int totalReplySize = Util.NOT_SET;
	
	/** 
	 * delta between the p.o.t. (point of time) the first packet of the request  
	 * left the client and the p.o.t. the last packet of the reply was received  
	 * by the client in the original trace in ms. 0 if no reply contained.
	 * 
	 * Note: this variable corresponds to "T12-T1" in Fig.5 of [1]
	 * 
	 * Note: this variable is relevant for SIMPLEX_OPEN_LOOP-Mode only
	 * 
	 * Note: used on client-side only
	 * 
	 * how to use:
	 *   1. wait "sendDelay" ms
	 *   2. send request and at the same (simulation) time wait 
	 *      "simplexReplyDelay" ms
	 *   3. load the next transaction and repeat with 1.
	*/
	protected int simplexReplyDelay = Util.NOT_SET;	
	
	/** 
	 * array containing the sizes of each reply of this transaction. the size 
	 * of the first reply is found at index 0 of the array. array is null if 
	 * this transaction does not contain any replies. 
	 * 
	 * see comment for "distinctReplySizes" below on how to use this array
	 * 
	 * Note: this array corresponds to "B_1" and B_2" in Fig.3 of [1], i.e., 
	 * distinctReplySizes[0] = "B_1" and distinctReplySizes[1] = "B_2". 
	 * 
	 * Note: this variable is relevant for DUPLEX-Mode only
	 * Note: used on server/mix-side only
	 */
	protected int[] distinctReplySizes = null;
	
	/**
	 * array containing the delays (in ms) after which each reply of this 
	 * transaction shall be sent by the last mix (right after the last mix has 
	 * fully received the request of this transaction). 
	 * the delays are supposed to assure that the simulated server doesn't 
	 * reply faster than the original server did (based on observations 
	 * extracted from the trace file this transaction was created from 
	 * 
	 * Note: uses the same index as the "distinctReplySizes"-array above
	 * Note: distinctReplyDelays[0] corresponds to "T6-T1" in Fig.5 of [1] and 
	 *       distinctReplyDelays[1] corresponds to "T10-T1" in Fig.5 of [1]
	 * 
	 * Note: this variable is relevant for DUPLEX-Mode only
	 * Note: used on server/mix-side only
	 * 
	 * how to use:
	 *      1. wait until the request of this transaction is fully received 
	 *         at the last mix (= T1)
	 *      2. schedule each reply of this message for later sending (without 
	 *         waiting):
	 *           for (int i=0; i<distinctReplyDelays.length; i++)
	 *              scheduler.sendReplyIn(distinctReplyDelays[i],
	 *                                    distinctReplySizes[i]);
	 */
	protected int[] distinctReplyDelays = null;
	
	/**
	 * array containing the delays (in ms) after which the arrival of replies 
	 * should be simulated in SIMPLEX_WITH_FEEDBACK-Mode. 
	 * 
	 * Note: uses the same index as the "distinctReplySizes"-array above
	 * Note: distinctReplyDelays[0] corresponds to "T8-T1" in Fig.5 of [1] and 
	 *       distinctReplyDelays[1] corresponds to "T12-T8" in Fig.5 of [1]
	 *       
	 * Note: this variable is relevant for SIMPLEX_WITH_FEEDBACK-Mode only
	 * Note: used on clinet-side only
	 * 
	 * how to use:
	 *     1. wait "sendDelay" ms
	 *     2. send request and wait until it is fully sent, i.e., wait until 
	 *        the write()-method returns (note: we assume a blocking socket 
	 *        here)
	 *     3. schedule the arrival of the next reply (not all replies!):
	 *        scheduler.simulateReplyArrivalIn(distinctSimplexWithFeedbackReplyDelays[0]);
	 *     4. on arrival of the reply (that was scheduled in 3.): 
	 *        schedule next reply, i.e. GOTO 3.:
	 *        (scheduler.simulateReplyArrivalIn(distinctSimplexWithFeedbackReplyDelays[i++]);)
	 *              
	 */
	protected int[] distinctSimplexWithFeedbackReplyDelays = null;
	
	
	/**
	 * for statistics. set when messages is sent via load generator or 
	 * simulator; in ms; offset from 1.1.1970 UTC for load generator (use 
	 * with System.currentTimeMillis()); for simulator: offset from start of 
	 * simulation (Simulator.getNow()).
	 */
	protected long timestampSend = Util.NOT_SET; // 
	
	
	public Object attachment;
	
	
	/**
	 * See class comment.
	 */
	public Transaction() {
		
	}

	
	/**
	 * See class comment.
	 */
	public Transaction(	int transactionId,
						int sendDelay,
						int serverId,
						int requestSize,
						int[] replySizes,
						int[] replyDelays
						) {
		this.transactionId = transactionId;
		this.sendDelay = sendDelay;
		this.serverId = serverId;
		this.requestSize = requestSize;
		this.distinctReplySizes = replySizes;
		this.distinctReplyDelays = replyDelays;
	}
	
	
	/**
	 * See class comment.
	 */
	public Transaction(	int transactionId,
						int sendDelay,
						int serverId,
						int requestSize
								) {
		this(	transactionId,
				sendDelay,
				serverId,
				requestSize,
				null,
				null
				);
	}
	
	
	/**
	 * See class comment.
	 */
	public Transaction(String serializedTransaction) {
		String[] fields = serializedTransaction.split("(,|;|\\s)");
		if (fields.length < 5)
			throw new RuntimeException("unknown format (not a serialized Transaction): " +serializedTransaction); 
		assert (fields.length % 2) == 1;
		this.transactionId = idCounter++; 
		this.sendDelay = Integer.parseInt(fields[0]);
		//this.clientId = Integer.parseInt(fields[1]);
		this.serverId = Integer.parseInt(fields[1]);
		this.requestSize = Integer.parseInt(fields[2]);
		this.totalReplySize = Integer.parseInt(fields[3]);
		this.simplexReplyDelay = Integer.parseInt(fields[4]);
		int replyRecords = (fields.length - 5) / 2;
		if (replyRecords > 0) {
			this.distinctReplySizes = new int[replyRecords];
			this.distinctReplyDelays = new int[replyRecords];
			for (int i=0; i<replyRecords; i++) {
				this.distinctReplySizes[i] = Integer.parseInt(fields[5+i*2]);
				this.distinctReplyDelays[i] = Integer.parseInt(fields[6+i*2]);
			}
		}
	}
	

	/**
	 * See class comment.
	 */
	public static Transaction readTransaction(BufferedReader trace) throws IOException {
		String line = trace.readLine();
		return new Transaction(line);
	}
	
	
	/** 
	 * delta between the p.o.t. (point of time) the first packet of the request  
	 * left the client and the p.o.t. the last packet of the reply was received  
	 * by the client in the original trace in ms. 0 if no reply contained.
	 * 
	 * Note: this variable corresponds to "T12-T1" in Fig.5 of [1]
	 * 
	 * Note: this variable is relevant for SIMPLEX_OPEN_LOOP-Mode only
	 * 
	 * Note: used on client-side only
	 * 
	 * how to use:
	 *   1. wait "sendDelay" ms
	 *   2. send request and at the same (simulation) time wait 
	 *      "simplexReplyDelay" ms
	 *   3. load the next transaction and repeat with 1.
	*/
	public int getSimplexReplyDelay() {
		return simplexReplyDelay;
	} 
	
	
	// TODO: public void reuse() ?
	// TODO: public static Transaction readTransaction(BufferedReader trace, Packet reusePacket) throws IOException {

	
	@Override
	public String toString() {
		return "transaction "+transactionId +": " +serialize();
	}
	
	
	public String serialize() {
		StringBuffer sb = new StringBuffer();
		serialize(sb);
		return sb.toString();
	}
	
	
	public StringBuffer serialize(StringBuffer bufferToAppend) {
		assert totalReplySize != Util.NOT_SET;
		bufferToAppend.append(Integer.toString(sendDelay));
		bufferToAppend.append(";");
		//bufferToAppend.append(Integer.toString(clientId));
		//bufferToAppend.append(";");
		bufferToAppend.append(Integer.toString(serverId));
		bufferToAppend.append(";");
		bufferToAppend.append(Integer.toString(requestSize));
		bufferToAppend.append(";");
		bufferToAppend.append(Integer.toString(totalReplySize));
		bufferToAppend.append(";");
		bufferToAppend.append(Integer.toString(simplexReplyDelay));
		if (distinctReplySizes != null && distinctReplySizes.length != 0)
			for (int i=0; i<distinctReplySizes.length; i++) {
				bufferToAppend.append(";");
				bufferToAppend.append(Integer.toString(distinctReplySizes[i]));
				bufferToAppend.append(";");
				bufferToAppend.append(Integer.toString(distinctReplyDelays[i]));
			}
		return bufferToAppend;
	}
	
	
	public void serialize(Writer destination) throws IOException {
		assert totalReplySize != Util.NOT_SET;
		destination.write(Integer.toString(sendDelay));
		destination.write(";");
		//destination.write(Integer.toString(clientId));
		//destination.write(";");
		destination.write(Integer.toString(serverId));
		destination.write(";");
		destination.write(Integer.toString(requestSize));
		destination.write(";");
		destination.write(Integer.toString(totalReplySize));
		destination.write(";");
		destination.write(Integer.toString(simplexReplyDelay));
		if (containsReplies())
			for (int i=0; i<distinctReplySizes.length; i++) {
				destination.write(";");
				destination.write(Integer.toString(distinctReplySizes[i]));
				destination.write(";");
				destination.write(Integer.toString(distinctReplyDelays[i]));
			}
	}
	
	
	
	/** 
	 * client shall start this transaction (i.e. send the request (see below)) 
	 * after RETURN ms. 
	 * Note: this value is referred to as "user think time" in many papers.
	 * Note: this variable corresponds to "T1-T0" in Fig.5 of [1] (or "T13-T12",
	 * if we assume that this is the second transaction of Fig.5 in [1])
	 * 
	 * Note: this variable is relevant for all modes
	 * Note: used on client-side only
	 */ 
	public int getSendDelay() {
		return sendDelay;
	}


	/**  id of the server who shall perform this transaction */
	public int getServerId() {
		return serverId;
	}
	
	
	/** unique id for this transaction */
	public int getTransactionId() {
		return transactionId;
	}


	/** 
	 * size of the request (i.e. the message from the client to the server) 
	 * in byte (0 if server shall send first message)
	 * 
	 * Note: this variable corresponds to "A_1" and "A_2" in Fig.3 of [1]
	 * Note: this variable is relevant for all modes
	 * Note: used on client-side only
	 */
	public int getRequestSize() {
		return requestSize;
	}
	
	
	public void setRequestSize(int newSize) {
		this.requestSize = newSize;
	}
	
	
	
	/** 
	 * size of all reply messages in total (0 if no message from server to 
	 * client shall be sent)
	 * 
	 * Note: this variable corresponds to "B_1 + B_2" in Fig.3 of [1]
	 * Note (informational): this variable is the sum of all individual sizes 
	 * stored in the "distinctReplySizes"-array below
	 * 
	 * Note: this variable is relevant for DUPLEX-Mode only
	 * Note: used on server/mix-side only
	 */
	public int getTotalReplySize() {
		return totalReplySize;
	}
	
	
	/**
	 * for statistics. set when messages is sent via load generator or 
	 * simulator; in ms; relative to 1.1.1970 UTC for load generator (use 
	 * with System.currentTimeMillis()); for simulator: relative to start of 
	 * simulation (Simulator.getNow()).
	 */
	public long getTimestampSend() {
		return timestampSend;
	}


	/**
	 * for statistics. set when messages is sent via load generator or 
	 * simulator; in ms; relative to 1.1.1970 UTC for load generator (use 
	 * with System.currentTimeMillis()); for simulator: relative to start of 
	 * simulation (Simulator.getNow()).
	 */
	public void setTimestampSend(long sendTime) {
		this.timestampSend = sendTime;
	}
	
	
	/** 
	 * array containing the sizes of each reply of this transaction. the size 
	 * of the first reply is found at index 0 of the array. array is null if 
	 * this transaction does not contain any replies. 
	 * 
	 * see comment for "distinctReplySizes" below on how to use this array
	 * 
	 * Note: this array corresponds to "B_1" and B_2" in Fig.3 of [1], i.e., 
	 * distinctReplySizes[0] = "B_1" and distinctReplySizes[1] = "B_2". 
	 * 
	 * Note: this variable is relevant for DUPLEX-Mode only
	 * Note: used on server/mix-side only
	 */
	public int[] getDistinctReplySizes() {
		return distinctReplySizes;
	}
	
	
	public void setDistinctReplySizes(int[] distinctReplySizes) {
		this.distinctReplySizes = distinctReplySizes;
	}
	
	
	public boolean containsReplies() {
		return !(distinctReplySizes == null || distinctReplySizes.length == 0 || distinctReplySizes[0] == 0);
	}
	
	
	/**
	 * array containing the delays (in ms) after which each reply of this 
	 * transaction shall be sent by the last mix (right after the last mix has 
	 * fully received the request of this transaction). 
	 * the delays are supposed to assure that the simulated server doesn't 
	 * reply faster than the original server did (based on observations 
	 * extracted from the trace file this transaction was created from 
	 * 
	 * Note: uses the same index as the "distinctReplySizes"-array above
	 * Note: distinctReplyDelays[0] corresponds to "T6-T1" in Fig.5 of [1] and 
	 *       distinctReplyDelays[1] corresponds to "T10-T1" in Fig.5 of [1]
	 * 
	 * Note: this variable is relevant for DUPLEX-Mode only
	 * Note: used on server/mix-side only
	 * 
	 * how to use:
	 *      1. wait until the request of this transaction is fully received 
	 *         at the last mix (= T1)
	 *      2. schedule each reply of this message for later sending (without 
	 *         waiting):
	 *           for (int i=0; i<distinctReplyDelays.length; i++)
	 *              scheduler.sendReplyIn(distinctReplyDelays[i],
	 *                                    distinctReplySizes[i]);
	 */
	public int[] getDistinctReplyDelays() {
		return distinctReplyDelays;
	}
	
	

	//public int[] getDistinctSimplexWithFeedbackReplyDelays() {
	//	return distinctSimplexWithFeedbackReplyDelays;
	//}
	
	int distinctSimplexWithFeedbackReplyDelayIndex = -1;
	
	/**
	 * returns the delays (in ms) after which the arrival of replies 
	 * should be simulated in SIMPLEX_WITH_FEEDBACK-Mode. 
	 * 
	 * Note: the value returned by this method after the first call corresponds 
	 *       to "T8-T1" in Fig.5 of [1] and the second to "T12-T8"
	 *       
	 * Note: this method is relevant for SIMPLEX_WITH_FEEDBACK-Mode only
	 * Note: used on clinet-side only
	 * 
	 * how to use:
	 *     1. wait "sendDelay" ms
	 *     2. send request and wait until it is fully sent, i.e., wait until 
	 *        the write()-method returns (note: we assume a blocking socket 
	 *        here)
	 *     3. schedule the arrival of the next reply (not all replies!):
	 *        scheduler.simulateReplyArrivalIn(getNextDistinctSimplexWithFeedbackReplyDelay());
	 *     4. on arrival of the reply (that was scheduled in 3.): 
	 *        schedule next reply, i.e. GOTO 3.:
	 *        (scheduler.simulateReplyArrivalIn(getNextDistinctSimplexWithFeedbackReplyDelay());)
	 *     Use hasMoreDistinctSimplexWithFeedbackReplyDelays() to check, 
	 *     whether further replies are present.
	 *              
	 */
	public int getNextDistinctSimplexWithFeedbackReplyDelay() {
		distinctSimplexWithFeedbackReplyDelayIndex++;
		return distinctSimplexWithFeedbackReplyDelays[distinctSimplexWithFeedbackReplyDelayIndex];
	}
	
	
	public boolean hasMoreDistinctSimplexWithFeedbackReplyDelays() {
		if (!containsReplies())
			return false;
		return distinctSimplexWithFeedbackReplyDelayIndex < (distinctSimplexWithFeedbackReplyDelays.length - 1);
	}
	
	
	/**
	 * (client-side) creates the payload object (request) to be sent via the
	 * anon socket. the request contains the header data needed by the
	 * server/last mix to reply to this request (finish the transaction)
	 */
	public byte[] createSendableTransaction(int clientId) {
		short replyFields = (!containsReplies()) ? 0
				: (short) distinctReplySizes.length;
		byte[] result = Util.concatArrays(new byte[][] {
				Util.intToByteArray(transactionId),
				Util.shortToByteArray(replyFields),
				Util.longToByteArray(timestampSend),
				Util.intToByteArray(clientId), Util.intToByteArray(serverId),
				Util.intToByteArray(requestSize), });
		for (int i = 0; i < replyFields; i++) {
			result = Util.concatArrays(result,
					Util.intToByteArray(distinctReplySizes[i]));
			result = Util.concatArrays(result,
					Util.intToByteArray(distinctReplyDelays[i]));
		}
		int payloadLength = Math.max(0, requestSize - (result.length + 4)); // additional 4 bytes for the header of "FragmentedMessage"
		if (payloadLength > 0) {
			byte[] payload = new byte[payloadLength];
			synchronized (random) {
				random.nextBytes(payload);
			}
			result = Util.concatArrays(result, payload);
		}
		return FragmentedMessage.toSendableMessage(result);
	}
	
}

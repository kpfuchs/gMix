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
import java.io.IOException;
import java.io.Writer;

import framework.core.util.Util;


/**
 * A Transaction consists of exactly one request and zero or more 
 * replies. This data structure can be used to replay Transactions in 
 * simulation or emulation setups. It contains data required on client and 
 * server-side to determine the size of and delay between requests and replies.
 */
public class Transaction {
	// TODO: update comments
	
	/** counter for the transactionId variable*/
	protected static int idCounter = 0;
	
	/** unique id for this transaction */
	protected int transactionId = Util.NOT_SET;
	
	/** 
	 * client shall start this transaction (i.e. send the request (see below)) 
	 * after "sendDelay" ms 
	 */
	protected int sendDelay = Util.NOT_SET;
	
	/** id of the client who shall perform this transaction */
	//protected int clientId = Util.NOT_SET;
	
	/** id of the server who shall perform this transaction */
	protected int serverId = Util.NOT_SET;
	
	/** 
	 * size of the request (i.e. the message from the client to the server) 
	 * in byte (0 if server shall send first message)
	 */
	protected int requestSize = Util.NOT_SET;
	
	/** 
	 * size of all reply messages in total (0 if no message from server to 
	 * client shall be sent)
	 */
	protected int totalReplySize = Util.NOT_SET;
	
	/** 
	 * delta between the p.o.t. (point of time) the last packet of the request 
	 * left the client and the p.o.t. the last packet of the reply was received 
	 * by the client in the original trace. use this value ONLY when replaying 
	 * traffic in a SIMPLEX simulation scenario WITH UNbounded communication 
	 * links (unlimited bandwidth; 0 latency) to determine the delay till the 
	 * next transaction. in a duplex simulation, use the "distinctReplyDelays" 
	 * array below instead.
	 * note that the value of this variable is NOT necessarily the sum of all 
	 * fields of "distinctReplyDelays", as distinct reply packets (in a trace 
	 * file) are interpreted as a single reply (that didn't fit into a single 
	 * packet) if the delay between two consecutive reply packets is small. if 
	 * a transaction is built out of many packets this can have a 
	 * none-neglectable influence on simplex simulations (thats the reason 
	 * for this variable).
	 * in ms.
	*/
	protected int simplexReplyDelay = Util.NOT_SET;
	
	/**
	 * if != null: server shall send multiple replies (message to client), 
	 * each of size distinctReplySizes[i] bytes and after at least 
	 * distinctReplyDelays[i] seconds
	 */
	protected int[] distinctReplySizes = null;
	
	/**
	 * Time deltas between the p.o.t. (point of time) the last packet of the 
	 * request left the client and the p.o.t. the first packet of the i-th 
	 * reply (i is the index of this array) was received by the client. use 
	 * this value when replaying traffic in DUPLEX mode on server-side, to 
	 * determine the delay till sending the next reply. the delays are 
	 * supposed to assure that the simulated server doesn't reply faster than 
	 * the original server did (based on observations extracted from the trace 
	 * file this transaction was created from or the simulation model used). 
	 * 
	 * example of usage:
	 * scenario: the server just (t_1=1) received a request with the reply 
	 * delays d_1 = 2, d_2 = 4 and d_3 = 2 ms. 
	 * He will wait 2 ms before sending the first reply.
	 * After sending (of the first reply) is finished, he will determine the 
	 * current time t_2=10 (to determine the time consumption for sending the 
	 * reply) and check whether t_2 - t_1 is larger than d_2.
	 * As  t_2 - t_1 = 9 is larger than  d_2 = 4, the server will send the next 
	 * reply immediately (if t_2 - t_1 was smaller than d_2, the server would 
	 * wait d_2 - (t_2 - t_1)  ms before sending the next reply).
	 * After sending of the next reply is finished, the server will again 
	 * determine the current time t_3=20 and check whether t_3 - t_1 is larger 
	 * than d_3...
	 * 
	 * You may want to think of the distinctReplyDelays as the "minimum 
	 * server-side delays of a reply from the p.o.t. the server received the 
	 * request".
	 */
	protected int[] distinctReplyDelays = null;

	/**
	 * for statistics. set when messages is sent via load generator or 
	 * simulator; in ms; offset from 1.1.1970 UTC for load generator (use 
	 * with System.currentTimeMillis()); for simulator: offset from start of 
	 * simulation (Simulator.getNow()).
	 */
	protected long timestampSend = Util.NOT_SET; // 
	
	
	public Object attachment;
	
	
	/**
	 * A Transaction consists of exactly one request and zero or more 
	 * replies. This data structure can be used to replay Transactions in 
	 * simulation or emulation setups. It contains data required on client and 
	 * server-side to determine the size of and delay between requests and replies.
	 */
	public Transaction() {
		
	}
	
	
	/**
	 * A Transaction consists of exactly one request and zero or more 
	 * replies. This data structure can be used to replay Transactions in 
	 * simulation or emulation setups. It contains data required on client and 
	 * server-side to determine the size of and delay between requests and replies.
	 */
	/*public Transaction(	float sendDelay,
								int clientId,
								int serverId,
								int requestSize,
								float replyDelay,
								int[] replySizes,
								float[] replyDelays
			) {
		this (	++idCounter,
				sendDelay,
				clientId,
				serverId,
				requestSize,
				replySizes,
				replyDelays
			);
	}*/
	
	
	/**
	 * A Transaction consists of exactly one request and zero or more 
	 * replies. This data structure can be used to replay Transactions in 
	 * simulation or emulation setups. It contains data required on client and 
	 * server-side to determine the size of and delay between requests and replies.
	 */
	public Transaction(	int transactionId,
								int sendDelay,
								//int clientId,
								int serverId,
								int requestSize,
								int[] replySizes,
								int[] replyDelays
								) {
		this.transactionId = transactionId;
		this.sendDelay = sendDelay;
		//this.clientId = clientId;
		this.serverId = serverId;
		this.requestSize = requestSize;
		this.distinctReplySizes = replySizes;
		this.distinctReplyDelays = replyDelays;
	}
	
	
	/**
	 * A Transaction consists of exactly one request and zero or more 
	 * replies. This data structure can be used to replay Transactions in 
	 * simulation or emulation setups. It contains data required on client and 
	 * server-side to determine the size of and delay between requests and replies.
	 */
	public Transaction(	int sendDelay,
								//int clientId,
								int serverId,
								int requestSize,
								int replySize,
								int replyDelay
								) {
		this (	++idCounter,
				sendDelay,
				//clientId,
				serverId,
				requestSize,
				new int[] {replySize},
				new int[] {replyDelay}
			);
	}
	
	
	/**
	 * A Transaction consists of exactly one request and zero or more 
	 * replies. This data structure can be used to replay Transactions in 
	 * simulation or emulation setups. It contains data required on client and 
	 * server-side to determine the size of and delay between requests and replies.
	 */
	/*public Transaction(	int transactionId,
								float sendDelay,
								int clientId,
								int serverId,
								int requestSize,
								int replySize,
								float replyDelay
								) {
		this(	transactionId,
				sendDelay,
				clientId,
				serverId,
				requestSize,
				new int[] {replySize},
				new float[] {replyDelay}
				);
	}*/
	
	
	/**
	 * A Transaction consists of exactly one request and zero or more 
	 * replies. This data structure can be used to replay Transactions in 
	 * simulation or emulation setups. It contains data required on client and 
	 * server-side to determine the size of and delay between requests and replies.
	 */
	/*public Transaction(	float sendDelay,
								int clientId,
								int serverId,
								int requestSize
								) {
		this (	++idCounter,
				sendDelay,
				clientId,
				serverId,
				requestSize,
				null,
				null
				);
	}*/
	
	
	/**
	 * A Transaction consists of exactly one request and zero or more 
	 * replies. This data structure can be used to replay Transactions in 
	 * simulation or emulation setups. It contains data required on client and 
	 * server-side to determine the size of and delay between requests and replies.
	 */
	public Transaction(	int transactionId,
								int sendDelay,
								//int clientId,
								int serverId,
								int requestSize
								) {
		this(	transactionId,
				sendDelay,
				//clientId,
				serverId,
				requestSize,
				null,
				null
				);
	}
	
	
	/**
	 * A Transaction consists of exactly one request and zero or more 
	 * replies. This data structure can be used to replay Transactions in 
	 * simulation or emulation setups. It contains data required on client and 
	 * server-side to determine the size of and delay between requests and replies.
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
	 * A Transaction consists of exactly one request and zero or more 
	 * replies. This data structure can be used to replay Transactions in 
	 * simulation or emulation setups. It contains data required on client and 
	 * server-side to determine the size of and delay between requests and replies.
	 */
	public static Transaction readTransaction(BufferedReader trace) throws IOException {
		String line = trace.readLine();
		return new Transaction(line);
	}
	
	
	/**
	 * use this method in SIMPLEX mode simulations (i.e. to replay the requests 
	 * of transactions only).
	 * this method returns the "totalReplyDelay" of this transaction, i.e. the 
	 * delta between the p.o.t. (point of time) the last packet of the request 
	 * left the client and the p.o.t. the last packet of the reply was received 
	 * by the client. use this value when replaying traffic in SIMPLEX mode to 
	 * determine the delay till the next transaction. 
	 * 
	 * note that there is no equivalent method for DUPLEX mode as the delay 
	 * will depend on the (simulated) properties of the reply channel. In 
	 * duplex mode, use the "distinctReplyDelays" array to delay replies on the 
	 * server side and wait on client side until all replies are received.
	 * then start the next transaction after 
	 * nextTransaction.getSendDelay() ms.
	 * 
	 * example (simplex):
	 * we want to determine the delay between two transactions 
	 * ("lastTransaction" and "nextTransaction") in simplex mode (assumption: 
	 * the "lastTransaction" was just sent and we want to determine the 
	 * delay till we should sent the "nextTransaction"):
	 * int delay = lastTransaction.getDelayTillNext() + 
	 * nextTransaction.getSendDelay();
	 * 
	 * determine the correct delay between 
	 * @return
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
	 * client shall start this transaction (i.e. send the request) after 
	 * "sendDelay" seconds.
	 */
	/*public void setSendDelay(int sendDelay) {
		this.sendDelay = sendDelay;
	}*/
	
	
	/** 
	 * client shall start this transaction (i.e. send the request) after 
	 * "sendDelay" ms.
	 */
	public int getSendDelay() {
		return sendDelay;
	}


	/** id of the client who shall perform this transaction */
	/*public int getClientId() {
		return clientId;
	}*/


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
	 * if != null || : server shall send multiple replies (messages to client), 
	 * each of size distinctReplySizes[i] bytes and after at least 
	 * distinctReplyDelays[i] seconds
	 */
	public int[] getDistinctReplySizes() {
		return distinctReplySizes;
	}
	
	
	/**
	 * if != null: server shall send multiple replies (message to client), 
	 * each of size distinctReplySizes[i] bytes and after at least 
	 * distinctReplyDelays[i] seconds
	 */
	public void setDistinctReplySizes(int[] distinctReplySizes) {
		this.distinctReplySizes = distinctReplySizes;
	}
	
	
	public boolean containsReplies() {
		return !(distinctReplySizes == null || distinctReplySizes.length == 0 || distinctReplySizes[0] == 0);
	}
	
	
	/**
	 * Time deltas between the p.o.t. (point of time) the last packet of the 
	 * request left the client and the p.o.t. the first packet of the i-th 
	 * reply (i is the index of this array) was received by the client. use 
	 * this value when replaying traffic in DUPLEX mode on server-side, to 
	 * determine the delay till sending the next reply. the delays are 
	 * supposed to assure that the simulated server doesn't reply faster than 
	 * the original server did (based on observations extracted from the trace 
	 * file this transaction was created from or the simulation model used). 
	 * 
	 * example of usage:
	 * scenario: the server just (t_1=1) received a request with the reply 
	 * delays d_1 = 2, d_2 = 4 and d_3 = 2 ms. 
	 * He will wait 2 seconds before sending the first reply.
	 * After sending (of the first reply) is finished, he will determine the 
	 * current time t_2=10 (to determine the time consumption for sending the 
	 * reply) and check whether t_2 - t_1 is larger than d_2.
	 * As  t_2 - t_1 = 9 is larger than  d_2 = 4, the server will send the next 
	 * reply immediately (if t_2 - t_1 was smaller than d_2, the server would 
	 * wait d_2 - (t_2 - t_1) ms before sending the next reply).
	 * After sending of the next reply is finished, the server will again 
	 * determine the current time t_3=20 and check whether t_3 - t_1 is larger 
	 * than d_3...
	 * 
	 * You may want to think of the distinctReplyDelays as the "minimum 
	 * server-side delays of a reply from the p.o.t. the server received the 
	 * request".
	 */
	public int[] getDistinctReplyDelays() {
		return distinctReplyDelays;
	}

	
}

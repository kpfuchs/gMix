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
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Arrays;

import framework.core.util.FragmentedMessage;
import framework.core.util.Util;


public class SendableTransaction extends Transaction {

	private static SecureRandom random = new SecureRandom();
	
	private int clientId;
	private long planedSendTime = Util.NOT_SET; // in nanosec (used to detect delays introduced by the scheduler)
	//private final static int BASIC_HEADER_LENGTH = 4 + 2 + 8 + 5*4 + 4; // transaction id + additionalReplyFields + send-timestamp + 5 standard fields (clientId, serverId ...) + header of "FragmentedMessage"
	private final static int REPLY_HEADER_LENGTH = 4 + 4 + 4; // transaction id + reply length + header of "FragmentedMessage"; (if reply length < 12: overhead; if reply length > 12: padding)
	private int replyCounter = 0;
	//private int headerLength = Util.NOT_SET;
	//private int payloadLength = Util.NOT_SET;
	private FragmentedMessage fragmentedMessage = new FragmentedMessage(); // data structure for to recreate transactions (requests/replies) from fragments
	
	
	public SendableTransaction(	
			int transactionId, 
			int sendDelay,
			int clientId,
			int serverId,
			int requestSize,
			int[] replySizes,
			int[] replyDelays
			) {
		super(	transactionId, 
				sendDelay, 
				//clientId, 
				serverId, 
				requestSize, 
				replySizes, 
				replyDelays
				);
		this.clientId = clientId;
	}


	public SendableTransaction(	
			int transactionId,
			int sendDelay,
			int clientId,
			int serverId,
			int requestSize,
			int replySize,
			int replyDelay
			) {
		super(	transactionId,
				sendDelay,
				//clientId,
				serverId,
				requestSize,
				new int[] {replySize},
				new int[] {replyDelay}
				);
		this.clientId = clientId;
	}
	

	public SendableTransaction(	
			int transactionId,
			int sendDelay,
			int clientId,
			int serverId,
			int requestSize
			) {
		super(	transactionId,
				sendDelay,
				//clientId,
				serverId,
				requestSize
				);
		this.clientId = clientId;
	}


	public SendableTransaction(String serializedTransaction) {
		super(serializedTransaction);
	}
	
	
	public SendableTransaction(InputStream inputStream) throws IOException {
		super();
		byte[] message = FragmentedMessage.forceReadMessage(inputStream);
		extractRequestHeaderRecords(message);
	}
	
	
	/**
	 * server-side
	 */
	public SendableTransaction() {
		super();
	}
	
	
	/**
	 * (client-side) creates the payload object (request) to be sent via the anon 
	 * socket. the request contains the header data needed by the server/last mix
	 * to reply to this request (finish the transaction)
	 */
	public byte[] createSendableTransaction() {
		short replyFields = (!containsReplies()) ? 0 : (short)distinctReplySizes.length;
		byte[] result = Util.concatArrays(
				new byte[][] {
						Util.intToByteArray(transactionId),
						Util.shortToByteArray(replyFields),
						Util.longToByteArray(timestampSend),
						Util.intToByteArray(clientId),
						Util.intToByteArray(serverId),
						Util.intToByteArray(requestSize),
					});
		for (int i=0; i<replyFields; i++) {
			result = Util.concatArrays(result, Util.intToByteArray(distinctReplySizes[i]));
			result = Util.concatArrays(result, Util.intToByteArray(distinctReplyDelays[i]));
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
	
	
	private void extractRequestHeaderRecords(byte[] message) {
		this.transactionId = Util.byteArrayToInt(Arrays.copyOfRange(message, 0, 4));
		short replyFields = Util.byteArrayToShort(Arrays.copyOfRange(message, 4, 6));
		this.timestampSend = Util.byteArrayToLong(Arrays.copyOfRange(message, 6, 14));
		this.clientId = Util.byteArrayToInt(Arrays.copyOfRange(message, 14, 18));
		this.serverId = Util.byteArrayToInt(Arrays.copyOfRange(message, 18, 22));
		this.requestSize = Util.byteArrayToInt(Arrays.copyOfRange(message, 22, 26));
		if (replyFields > 0) {
			super.distinctReplySizes = new int[replyFields];
			super.distinctReplyDelays = new int[replyFields];
			for (int i=0; i<replyFields; i++) {
				super.distinctReplySizes[i] = Util.byteArrayToInt(Arrays.copyOfRange(message, 26 + (i*8), 30 + (i*8)));
				super.distinctReplyDelays[i] = Util.byteArrayToInt(Arrays.copyOfRange(message, 30 + (i*8), 34 + (i*8)));
			}
		}
	}
	
	
	/**
	 * (server/mix-side) create payload for reply (data to be sent via the anon 
	 * channel)
	 * 
	 * @return
	 */
	public byte[] createSendableReply() {
		assert containsReplies();
		assert hasMoreReplies();
		replyCounter++;
		assert replyCounter <= distinctReplySizes.length;
		int replySize = distinctReplySizes[replyCounter - 1];
		if (replySize <= (REPLY_HEADER_LENGTH)) {
			byte[] result = Util.concatArrays(new byte[][] {
					Util.intToByteArray(transactionId),
					Util.intToByteArray(replySize)
				});
			return FragmentedMessage.toSendableMessage(result);
		} else {
			byte[] payload = new byte[replySize - REPLY_HEADER_LENGTH];
			synchronized (random) {
				random.nextBytes(payload);
			}
			payload = Util.concatArrays(new byte[][] {
					Util.intToByteArray(transactionId),
					Util.intToByteArray(replySize),
					payload
				});
			return FragmentedMessage.toSendableMessage(payload);
		}
	}
	
	

	public boolean hasMoreReplies() {
		return remainingReplies() != 0;
	}
	
	

	public int remainingReplies() {
		if (!containsReplies())
			return 0;
		return distinctReplySizes.length - replyCounter;
	}
	
	
	
	/**
	 * (client-side)
	 * @param message
	 */
	private void extractReplyHeaderRecords(byte[] message) {
		replyCounter++;
		assert replyCounter <= distinctReplySizes.length;
		int transactionId = Util.byteArrayToInt(Arrays.copyOfRange(message, 0, 4));
		assert transactionId == this.transactionId: ""+ transactionId +"!=" +this.transactionId;
		int replySize = Util.byteArrayToInt(Arrays.copyOfRange(message, 4, 8));
		int expectedReplySize = distinctReplySizes[replyCounter - 1];
		assert replySize == expectedReplySize: ""+ replySize +"!=" +expectedReplySize;
	}
	
	
	/**
	 * (server/mix-side) returns whether further chunks are needed or if the 
	 * message is already received completely
	 * see "addRequestChunk(payloadChunk)"
	 * @return
	 */
	public boolean needMoreRequestChunks() {
		return fragmentedMessage.isFullyReceived();
	}
	
	
	/**
	 * (server/mix-side) add a message chunk
	 * see "needMoreRequestChunks()"
	 * @return returns null if the complete payloadChunk was required or a 
	 * fraction of the payloadChunk otherwise
	 */
	public byte[] addRequestChunk(byte[] payloadChunk) {
		return fragmentedMessage.addFragment(payloadChunk);
	}
	
	
	/**
	 * (client-side) returns whether further chunks are needed or if the 
	 * message is already received completely
	 * see "addReplyChunk(byte[] payloadChunk)"
	 * @return
	 */
	public boolean needMoreReplyChunks() {
		return !hasMoreReplies() && fragmentedMessage.isFullyReceived();
	}
	
	
	
	/**
	 * (client-side) add a message chunk
	 * see "needMoreReplyChunks()"
	 * @return returns null if the complete payloadChunk was required or a 
	 * fraction of the payloadChunk otherwise
	 */
	public byte[] addReplyChunk(byte[] payloadChunk) {
		assert needMoreReplyChunks();
		byte[] overhead = fragmentedMessage.addFragment(payloadChunk);
		if (fragmentedMessage.isFullyReceived()) {
			extractReplyHeaderRecords(fragmentedMessage.getRawMessage());
			if (hasMoreReplies()) // create data structure for reading next reply
				this.fragmentedMessage = new FragmentedMessage();
			if (overhead != null) // assert that the method will only return bytes that do NOT belong to ANY reply of this transaction (not only the current transaction) 
				addReplyChunk(overhead);
		}
		return overhead;
	}
	
	
	/**
	 * (client-side) read all replies from inputStream that belong to this 
	 * transaction.
	 * blocks the calling thread until the all replies are received (for 
	 * none-blocking receival, the method addReplyChunk(byte[] payloadChunk) 
	 * can be used).
	 */
	public void readReplies(InputStream inputStream) throws IOException {
		while (hasMoreReplies())
			readSingleReply(inputStream);
	}
	
	
	
	/**
	 * (client-side) read reply from inputStream
	 * blocks the calling thread until the whole reply is received (for 
	 * none-blocking receival, the method addReplyChunk(byte[] payloadChunk) 
	 * can be used).
	 */
	public void readSingleReply(InputStream inputStream) throws IOException {
		assert containsReplies();
		assert hasMoreReplies();
		byte[] message = FragmentedMessage.forceReadMessage(inputStream);
		extractReplyHeaderRecords(message);
	}
	
	
	public long getPlanedSendTime() {
		return planedSendTime;
	}


	public void setPlanedSendTime(long planedSendTime) {
		this.planedSendTime = planedSendTime;
	}
	
	
	public int getClientId() {
		return this.clientId;
	}

}

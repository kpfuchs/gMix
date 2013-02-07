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
package evaluation.loadGenerator.applicationLevelTraffic.requestReply;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

import framework.core.util.Util;


public class ApplicationLevelMessage {

	private final static int REQUEST_HEADER_SIZE = 32;
	private final static int REPLY_HEADER_SIZE = 8;
	private static SecureRandom random = new SecureRandom();
	private static int idCounter = 0;
	
	private float sendDelay = Util.NOT_SET; // in sec
	private int clientId = Util.NOT_SET;
	private int serverId = Util.NOT_SET;
	private int requestSize = Util.NOT_SET; // in byte
	private int replySize = Util.NOT_SET; // in byte
	private float replyDelay = Util.NOT_SET; // in sec
	private int transactionId = Util.NOT_SET;
	private long planedSendTime = Util.NOT_SET; // in nanosec
	private long sendTime = Util.NOT_SET; // in nanosec
	private long absoluteSendTime = Util.NOT_SET; // in ms; relative to 1.1.1970 UTC (use with System.currentTimeMillis())
	
	private boolean chunkModeOn = false;
	private boolean receivedAllChunks = false;
	private ByteBuffer headerCache = null;
	private ByteBuffer messageCache = null;
	
	public boolean isNotifyEvent = false;
	
	
	/**
	 * (client-side) create ApplicationLevelMessage from 
	 * trace file
	 * 
	 * @param traceFile
	 * @throws EndOfFileReachedException
	 */
	public ApplicationLevelMessage(
			BufferedReader traceFile,
			int transactionId
			) throws EndOfFileReachedException {
		String line;
		try {
			line = traceFile.readLine();
		} catch (Exception e) {
			throw new EndOfFileReachedException();
		}
		initThroughTraceRecordAsString(line, transactionId);
	}
	
	
	public ApplicationLevelMessage(String traceRecord) {
		initThroughTraceRecordAsString(traceRecord);
	}
	
	
	/**
	 * (client-side) create ApplicationLevelMessage from 
	 * the parameters specified (duplex mode)
	 * 
	 * @param sendDelay
	 * @param clientId
	 * @param serverId
	 * @param requestSize
	 * @param replySize
	 * @param replyDelay
	 */
	public ApplicationLevelMessage(
			float sendDelay,
			int clientId,
			int serverId,
			int requestSize,
			int replySize,
			float replyDelay
			) {
		
		this.sendDelay = sendDelay;
		this.clientId = clientId;
		this.serverId = serverId;
		this.requestSize = requestSize;
		if (this.requestSize < REQUEST_HEADER_SIZE) {
			//System.out.println(
			//		"warning: the request size specifed is too " +
			//		"small and will be adjusted");
			this.requestSize = REQUEST_HEADER_SIZE;
		}
		this.replySize = replySize;
		if (this.replySize < REPLY_HEADER_SIZE) {
			//System.out.println(
			//		"warning: the reply size specifed is too " +
			//		"small and will be adjusted");
			this.replySize = REPLY_HEADER_SIZE;
		}
		this.replyDelay = replyDelay;
		synchronized (random) {
			this.transactionId = idCounter++;
		}
	}

	
	/**
	 * (client-side) create ApplicationLevelMessage from 
	 * the parameters specified (simplex mode)
	 * 
	 * @param sendDelay
	 * @param clientId
	 * @param serverId
	 * @param requestSize
	 */
	public ApplicationLevelMessage(
			float sendDelay,
			int clientId,
			int serverId,
			int requestSize
			) {
		
		this.sendDelay = sendDelay;
		this.clientId = clientId;
		this.serverId = serverId;
		this.requestSize = requestSize;
		if (this.requestSize < REQUEST_HEADER_SIZE) {
			//System.out.println(
			//		"warning: the request size specifed is too " +
			//		"small and will be adjusted");
			this.requestSize = REQUEST_HEADER_SIZE;
		}
		synchronized (random) {
			this.transactionId = idCounter++;
		}
	}
	
	
	/**
	 * (server/mix-side) create ApplicationLevelMessage 
	 * from InputStream
	 * 
	 * @param inputStream
	 * @throws IOException
	 */
	public ApplicationLevelMessage(
			InputStream inputStream
			) throws IOException {
		// read header:
		byte[] header = Util.forceRead(inputStream, REQUEST_HEADER_SIZE);
		extractRequestHeaderRecords(header);
		// read rest of message:
		if (requestSize - REQUEST_HEADER_SIZE > 0)
			Util.forceRead(inputStream, requestSize - REQUEST_HEADER_SIZE);
	}
	
	
	
	/**
	 * (server/mix-side) create ApplicationLevelMessage 
	 * and add message chunks later via "addChunk(byte[] payloadChunk)"
	 * see "addChunk(byte[] payloadChunk)"
	 * see "needMore()"
	 * use this constructor to prevent the calling thread to block until all
	 * mix messages of the corresponding request are received
	 */
	public ApplicationLevelMessage() {
		this.chunkModeOn = true;
		this.headerCache = ByteBuffer.allocate(REQUEST_HEADER_SIZE);
	}
	
	
	public void initThroughTraceRecordAsString(String traceRecord, int transactionId) {
		String[] columns = traceRecord.split("(,|;|\\s)");
		if (columns.length != 6)
			System.err.println("unrecognized trace file format"); 
		this.sendDelay = Float.parseFloat(columns[0]);
		this.clientId = Integer.parseInt(columns[1]);
		this.serverId = Integer.parseInt(columns[2]);
		this.requestSize = Integer.parseInt(columns[3]);
		if (this.requestSize < REQUEST_HEADER_SIZE) {
			//System.out.println(
			//		"warning: the request size specifed is too " +
			//		"small and will be adjusted");
			this.requestSize = REQUEST_HEADER_SIZE;
		}
		this.replySize = Integer.parseInt(columns[4]);
		if (this.replySize < REPLY_HEADER_SIZE) {
			//System.out.println(
			//		"warning: the reply size specifed is too " +
			//		"small and will be adjusted");
			this.replySize = REPLY_HEADER_SIZE;
		}
		this.replyDelay = Float.parseFloat(columns[5]);
		this.transactionId = transactionId;
	}
	
	
	public void initThroughTraceRecordAsString(String traceRecord) {
		synchronized (random) {
			this.transactionId = idCounter++;
		}
		initThroughTraceRecordAsString(traceRecord, transactionId);
	}
	
		
	/**
	 * (server/mix-side) returns whether further chunks are needed or if the 
	 * message is already received completely
	 * see "addChunk(byte[] payloadChunk)"
	 * see "ApplicationLevelMessage()"
	 * @return
	 */
	public boolean needMoreRequestChunks() {
		if (!chunkModeOn)
			throw new RuntimeException("only available if chunkMode is on");
		return !receivedAllChunks;
	}
	
	
	/**
	 * (server/mix-side) add a message chunk
	 * see "ApplicationLevelMessage()"
	 * see "needMore()"
	 * @return returns null if the complete payloadChunk was required or a 
	 * fraction of the payloadChunk otherwise
	 */
	public byte[] addRequestChunk(byte[] payloadChunk) {
		if (!chunkModeOn)
			throw new RuntimeException("cannot add chunks if constructor " +
					"\"ApplicationLevelTraceEntry_RequestReply_v_001(byte[] " +
					"payloadChunk)\" wasn't used to create this object"
					);
		// try to read header if not yet done:
		if (headerCache != null) {
			if (headerCache.hasRemaining()) { 
				if (payloadChunk.length < headerCache.remaining()) { // not enough data to read header completely
					headerCache.put(payloadChunk);
					return null;
				} else if (payloadChunk.length == headerCache.remaining()) { // exactly enough data to read header completely
					headerCache.put(payloadChunk);
					extractRequestHeaderRecords(headerCache.array());
					if (requestSize == REQUEST_HEADER_SIZE) {
						assert headerCache.position() == requestSize;
						receivedAllChunks = true; 
					}
					return null;
				} else if (payloadChunk.length > headerCache.remaining()) { // more data available than needed to read header completely
					byte[][] splitted = Util.split(headerCache.remaining(), payloadChunk);
					headerCache.put(splitted[0]);
					extractRequestHeaderRecords(headerCache.array());
					payloadChunk = splitted[1];
					if (requestSize == REQUEST_HEADER_SIZE)
						receivedAllChunks = true;
				}
			}
		}
		
		// try to read rest of message:
		if (messageCache != null && messageCache.hasRemaining()) { // request not yet fully received
			if (payloadChunk.length < messageCache.remaining()) { // not enough data to read message completely
				messageCache.put(payloadChunk);
				//System.out.println("adding chunk; transactionId,a: " +transactionId +", " +messageCache.position() +" of " +requestSize +" bytes received (" +payloadChunk.length +" bytes are new)"); // TODO 
				return null;
			} else if (payloadChunk.length == messageCache.remaining()) { // exactly enough data to read message completely
				messageCache.put(payloadChunk);
				assert messageCache.position() == requestSize;
				receivedAllChunks = true;
				//System.out.println("request received completely: id: " +transactionId +", size: " +messageCache.position() +"bytes, overhad: 0 bytes"); // TODO 
				return null;
			} else if (payloadChunk.length > messageCache.remaining()) { // more data available than needed to read message completely
				byte[][] splitted = Util.split(messageCache.remaining(), payloadChunk);
				messageCache.put(splitted[0]);
				assert messageCache.position() == requestSize;
				receivedAllChunks = true;
				//System.out.println("request received completely: " +transactionId +", size: " +messageCache.position() +"bytes, overhad: " +splitted[1].length +" bytes"); // TODO 
				return splitted[1];
			}
		}
		return null;
	}
	
	
	private void extractRequestHeaderRecords(byte[] byteHeader) {
		if (byteHeader.length != REQUEST_HEADER_SIZE)
			throw new RuntimeException("bypassed array has wrong length"); 
		this.requestSize = Util.byteArrayToInt(Arrays.copyOfRange(byteHeader, 0, 4));
		this.clientId = Util.byteArrayToInt(Arrays.copyOfRange(byteHeader, 4, 8));
		this.transactionId = Util.byteArrayToInt(Arrays.copyOfRange(byteHeader, 8, 12));
		this.serverId = Util.byteArrayToInt(Arrays.copyOfRange(byteHeader, 12, 16));
		this.absoluteSendTime = Util.byteArrayToLong(Arrays.copyOfRange(byteHeader, 16, 24));
		this.replySize = Util.byteArrayToInt(Arrays.copyOfRange(byteHeader, 24, 28));
		this.replyDelay = Util.byteArrayToFloat(Arrays.copyOfRange(byteHeader, 28, 32));
		if (requestSize - REQUEST_HEADER_SIZE > 0) {
			this.messageCache = ByteBuffer.allocate(requestSize);
			this.messageCache.put(byteHeader);
		} else 
			this.messageCache = null;
		this.headerCache = null;
		//System.out.println("extracting header; transactionId: " +transactionId +", requestSize: " +requestSize); // TODO 
	}
	
	
	/**
	 * (client-side) creates the payload object to be sent via the anon 
	 * socket. the payload contains the header data needed by the server/mix
	 * to reply to this request
	 * see "ApplicationLevelTraceEntry_RequestReply_v_001()"
	 * see "needMore()"
	 * @return returns null if the complete payloadChunk was required or a 
	 * fraction of the payloadChunk otherwise
	 */
	public byte[] createPayloadForRequest() {
		byte[] payload = new byte[requestSize - REQUEST_HEADER_SIZE];
		synchronized (random) {
			random.nextBytes(payload);
		}
		headerCache = ByteBuffer.allocate(REPLY_HEADER_SIZE);
		receivedAllChunks = false;
		return Util.concatArrays(
				new byte[][] {
					Util.intToByteArray(requestSize),
					Util.intToByteArray(clientId),
					Util.intToByteArray(transactionId),
					Util.intToByteArray(serverId),
					Util.longToByteArray(absoluteSendTime),
					Util.intToByteArray(replySize),
					Util.floatToByteArray(replyDelay),
					payload
				});
	}
	
	
	/**
	 * (server/mix-side) create payload for reply (data to be sent via the anon 
	 * channel)
	 * 
	 * @return
	 */
	public byte[] createPayloadForReply() {
		assert replySize != Util.NOT_SET;
		assert replySize >= REPLY_HEADER_SIZE;
		if (replySize == REPLY_HEADER_SIZE) {
			return Util.concatArrays(new byte[][] {
					Util.intToByteArray(transactionId),
					Util.intToByteArray(replySize)
			});
		} else {
			byte[] payload = new byte[replySize - REPLY_HEADER_SIZE];
			synchronized (random) {
				random.nextBytes(payload);
			}
			return Util.concatArrays(new byte[][] {
					Util.intToByteArray(transactionId),
					Util.intToByteArray(replySize),
					payload
			});
		}
	}
	
	
	private void extractReplyHeaderRecords(byte[] header) {
		if (header.length != REPLY_HEADER_SIZE)
			throw new RuntimeException("bypassed array has wrong length: " +header.length +"!=" +REPLY_HEADER_SIZE); 
		int transactionId = Util.byteArrayToInt(Arrays.copyOfRange(header, 0, 4));
		assert transactionId == this.transactionId: ""+ transactionId +"!=" +this.transactionId;
		int replySize = Util.byteArrayToInt(Arrays.copyOfRange(header, 4, 8));
		assert replySize == this.replySize;
		if ((replySize - REPLY_HEADER_SIZE) > 0) {
			this.messageCache = ByteBuffer.allocate(replySize);
			this.messageCache.put(header);
		} else 
			this.messageCache = null;
		this.headerCache = null;
	}
	
	
	/**
	 * (client-side) read reply from inputStream
	 * blocks the calling thread until the whole reply is received (for 
	 * none-blocking receival, the method addReplyChunk(byte[] payloadChunk) 
	 * can be used).
	 */
	public void readReply(InputStream inputStream) throws IOException {
		assert replySize != Util.NOT_SET;
		byte[] replyHeader = Util.forceRead(inputStream, REPLY_HEADER_SIZE);
		extractReplyHeaderRecords(replyHeader);
		/*byte[] payload = */Util.forceRead(inputStream, replySize - REPLY_HEADER_SIZE);
	}
	
	
	/**
	 * (client-side) returns whether further chunks are needed or if the 
	 * message is already received completely
	 * see "addReplyChunk(byte[] payloadChunk)"
	 * @return
	 */
	public boolean needMoreReplyChunks() {
		return !receivedAllChunks;
	}
	
	
	/**
	 * (client-side) add a message chunk
	 * see "needMoreReplyChunks()"
	 * @return returns null if the complete payloadChunk was required or a 
	 * fraction of the payloadChunk otherwise
	 */
	public byte[] addReplyChunk(byte[] payloadChunk) {
		// try to read header if not yet done:
		if (headerCache != null) {
			if (headerCache.hasRemaining()) { 
				if (payloadChunk.length < headerCache.remaining()) { // not enough data to read header completely
					headerCache.put(payloadChunk);
					return null;
				} else if (payloadChunk.length == headerCache.remaining()) { // exactly enough data to read header completely
					headerCache.put(payloadChunk);
					extractReplyHeaderRecords(headerCache.array());
					if (replySize == REPLY_HEADER_SIZE) {
						assert headerCache.position() == replySize;
						receivedAllChunks = true; 
					}
					return null;
				} else if (payloadChunk.length > headerCache.remaining()) { // more data available than needed to read header completely
					byte[][] splitted = Util.split(headerCache.remaining(), payloadChunk);
					headerCache.put(splitted[0]);
					extractReplyHeaderRecords(headerCache.array());
					payloadChunk = splitted[1];
					if (replySize == REPLY_HEADER_SIZE)
						receivedAllChunks = true;
				}
			}
		}
		// try to read rest of message:
		if (messageCache != null && messageCache.hasRemaining()) { // request not yet fully received
			if (payloadChunk.length < messageCache.remaining()) { // not enough data to read message completely
				messageCache.put(payloadChunk);
				//System.err.println("added reply chunk; transactionId,a: " +transactionId +", " +messageCache.position() +" of " +messageCache.capacity() +" bytes received (" +payloadChunk.length +" bytes are new)"); // TODO 
				return null;
			} else if (payloadChunk.length == messageCache.remaining()) { // exactly enough data to read message completely
				messageCache.put(payloadChunk);
				//System.err.println("added reply chunk; transactionId,b: " +transactionId +", " +messageCache.position() +" of " +messageCache.capacity() +" bytes received (" +payloadChunk.length +" bytes are new)"); // TODO 
				assert messageCache.position() == replySize;
				receivedAllChunks = true;
				//System.err.println("reply received completely: id: " +transactionId +", size: " +messageCache.position() +"bytes, overhad: 0 bytes"); // TODO 
				return null;
			} else if (payloadChunk.length > messageCache.remaining()) { // more data available than needed to read message completely
				byte[][] splitted = Util.split(messageCache.remaining(), payloadChunk);
				messageCache.put(splitted[0]);
				//System.err.println("added reply chunk; transactionId,c: " +transactionId +", " +messageCache.position() +" of " +messageCache.capacity() +" bytes received (" +splitted[0].length +" bytes are new)"); // TODO 
				assert messageCache.position() == replySize;
				receivedAllChunks = true;
				//System.err.println("reply received completely: " +transactionId +", size: " +messageCache.position() +"bytes, overhad: " +splitted[1].length +" bytes"); // TODO 
				return splitted[1];
			}
		}
		return null;
	}
	
	
	public float getSendDelay() {
		return sendDelay;
	}
	
	
	public int getSendDelayInMilliSec() {
		return (int) (sendDelay*1000f);
	}
	
	
	public int getSendDelayInMicroSec() {
		return (int) (sendDelay*1000f*1000f);
	}
	
	
	public long getSendDelayInNanoSec() {
		return (long) (sendDelay*1000f*1000f*1000f);
	}


	public int getClientId() {
		return clientId;
	}


	public int getServerId() {
		return serverId;
	}
	
	
	public int getTransactionId() {
		if (transactionId == Util.NOT_SET)
			System.err.println("no transactionId set"); 
		return transactionId;
	}


	public int getRequestSize() {
		return requestSize;
	}


	public int getReplySize() {
		if (replySize == Util.NOT_SET)
			System.err.println("no reply size set"); 
		return replySize;
	}


	public float getReplyDelay() {
		if (replyDelay == Util.NOT_SET)
			System.err.println("no reply delay set"); 
		return replyDelay;
	}

	
	public int getReplyDelayInMilliSec() {
		if (replyDelay == Util.NOT_SET)
			System.err.println("no reply delay set"); 
		return (int) (replyDelay*1000f);
	}
	
	
	public int getReplyDelayInMicroSec() {
		if (replyDelay == Util.NOT_SET)
			System.err.println("no reply delay set"); 
		return (int) (replyDelay*1000f*1000f);
	}
	
	
	public long getReplyDelayInNanoSec() {
		if (replyDelay == Util.NOT_SET)
			System.err.println("no reply delay set"); 
		return (long) (replyDelay*1000f*1000f*1000f);
	}


	public long getSendTime() {
		return sendTime;
	}


	public void setSendTime(long sendTime) {
		this.sendTime = sendTime;
	}
	
	
	public long getPlanedSendTime() {
		return planedSendTime;
	}


	public void setPlanedSendTime(long planedSendTime) {
		this.planedSendTime = planedSendTime;
	}
	
	
	public long getAbsoluteSendTime() {
		return absoluteSendTime;
	}


	public void setAbsoluteSendTime(long sendTime) {
		this.absoluteSendTime = sendTime;
	}


	@Override
	public String toString() {
		return serialize();
	}
	
	
	public String serialize() {
		StringBuffer sb = new StringBuffer();
		serialize(sb);
		return sb.toString();
	}
	
	public StringBuffer serialize(StringBuffer bufferToAppend) {
		bufferToAppend.append(sendDelay);
		bufferToAppend.append(";");
		bufferToAppend.append(clientId);
		bufferToAppend.append(";");
		bufferToAppend.append(serverId);
		bufferToAppend.append(";");
		bufferToAppend.append(requestSize);
		bufferToAppend.append(";");
		bufferToAppend.append(replySize);
		bufferToAppend.append(";");
		bufferToAppend.append(replyDelay);
		return bufferToAppend;
	}
	
	
	public void serialize(Writer destination) throws IOException {
		destination.write(Float.toString(sendDelay));
		destination.write(";");
		destination.write(Integer.toString(clientId));
		destination.write(";");
		destination.write(Integer.toString(serverId));
		destination.write(";");
		destination.write(Integer.toString(requestSize));
		destination.write(";");
		destination.write(Integer.toString(replySize));
		destination.write(";");
		destination.write(Float.toString(replyDelay));
	}
	
}

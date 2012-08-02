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
package framework.core.socket.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import framework.core.AnonNode;
import framework.core.controller.Layer3OutputStrategyMixController;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.userDatabase.User;


public class BasicOutputStreamMix extends OutputStream implements Callable<BasicOutputStreamMix> {

	private boolean isClosed = false;
	private StreamAnonSocketMixImpl socket;
	private User user;
	private AnonNode owner;

	private boolean sendImmediately;
	private int timeToWaitForFurtherData; // in microseconds
	public static ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1); // TODO: shut down on exit in AnonNode
	private ScheduledFuture<BasicOutputStreamMix> currentTimer;
	private ByteBuffer payloadForNextMessage;
	private Object synchronizer = new Object();
	private ByteBuffer timerByteBufferReference; // used to check if ByteBuffer was already sent
	private Layer3OutputStrategyMixController layer3;
	
	
	public BasicOutputStreamMix(
			AnonNode owner,
			StreamAnonSocketMixImpl socket,
			User user
			) {
		this.owner = owner;
		this.layer3 = owner.getOutputStrategyLayerControllerMix();
		this.user = user;
		this.socket = socket;
		this.timeToWaitForFurtherData = owner.TIME_TO_WAIT_FOR_FURTHER_DATA;
		if (timeToWaitForFurtherData == 0)
			sendImmediately = true;
	}


	
	@Override
	public void close() throws IOException {
		this.isClosed = true;
		this.socket = null;
	}
	
	
	@Override
	public void write(byte[] b) throws IOException {
		ByteBuffer toWrite = ByteBuffer.wrap(b);
		write(toWrite);
	}
	
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		ByteBuffer toWrite = ByteBuffer.wrap(b, off, len);
		write(toWrite);
	}
	
	
	@Override
	public void write(int b) throws IOException {
		if (isClosed) 
			throw new IOException("OutputStream closed!");
		if (!socket.isConnected())	
			throw new IOException("not connected");
		write(new byte[] {(byte)b});
	}
	
	
	private void write(ByteBuffer toWrite) throws IOException {
		if (isClosed)
			throw new IOException("OutputStream closed");
		if (!socket.isConnected())	
			throw new IOException("not connected");
		
		if (sendImmediately) {
			while (toWrite.hasRemaining()) {
				int length = getMTU() > toWrite.remaining() ? toWrite.remaining() : getMTU();
				byte[] payload = new byte[length];
				toWrite.get(payload);
				sendMessage(payload);
			}
		} else {
			synchronized (synchronizer) {
				while (toWrite.hasRemaining()) { // send as many full packets as possible; store additional data (that doesn't fill a complete packet) for later sending
					if (payloadForNextMessage == null)
						payloadForNextMessage = ByteBuffer.allocate(getMTU());
					if (toWrite.remaining() >= payloadForNextMessage.remaining()) { // send new message now
						if (payloadForNextMessage.position() == 0) { // empty -> send directly (without "payloadForNextMessage"-ByteBuffer)
							byte[] payload = new byte[payloadForNextMessage.capacity()];
							toWrite.get(payload);
							sendMessage(payload);
						} else { // send together with data already stored
							if (currentTimer != null && !currentTimer.isDone()) // early timer cancel (not really needed, but prevents unnecessary TimerTask executions)
								currentTimer.cancel(false);
							byte[] newData = new byte[payloadForNextMessage.remaining()];
							toWrite.get(newData);
							payloadForNextMessage.put(newData);
							sendMessage(payloadForNextMessage.array());
							payloadForNextMessage = null;
						}
					} else { // store data for later sending
						payloadForNextMessage.put(toWrite);
					}
				}
				// set timeout if needed
				if (payloadForNextMessage != null && payloadForNextMessage.position() != 0 && (currentTimer == null || currentTimer.isDone())) {
					timerByteBufferReference = payloadForNextMessage;
					currentTimer = scheduler.schedule(this, timeToWaitForFurtherData, TimeUnit.MICROSECONDS);
				}
			}
		}
	}
	

	@Override
	public void flush() throws IOException {
		if (sendImmediately) {
			// don't do anything; data is always sent directly
		} else {
			sendNow();
		}
	}
	
	
	private void sendMessage(byte[] payload) {
		Reply reply = MixMessage.getInstanceReply(payload, user);
		owner.putInReplyInputQueue(reply);
	}
	
	
	private void sendNow() {
		synchronized (synchronizer) {
			if (payloadForNextMessage == null || payloadForNextMessage.position() == 0)
				return;
			if (currentTimer != null && !currentTimer.isDone())
				currentTimer.cancel(false);
			byte[] payload = new byte[payloadForNextMessage.position()];
			payloadForNextMessage.get(payload);
			payloadForNextMessage = null;
			sendMessage(payload);
		}
	}
	
	
	public int getMTU() {
		return layer3.getMaxSizeOfNextReply();
	}

	
	@Override
	public BasicOutputStreamMix call() {
		synchronized (synchronizer) {
			if (payloadForNextMessage != null && timerByteBufferReference == payloadForNextMessage) {
				sendNow();
			}
			return this;
		}
	}
}

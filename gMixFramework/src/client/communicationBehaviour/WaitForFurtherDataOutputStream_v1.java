/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2011  Karl-Peter Fuchs
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

package client.communicationBehaviour;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import recodingScheme.MessageCreator;

import message.Request;

import client.mixCommunicationHandler.MixCommunicationHandler;


public class WaitForFurtherDataOutputStream_v1 extends OutputStream {

	@SuppressWarnings("unused")
	private ClientCommunicationBehaviour owner;
	private MixCommunicationHandler mixCommunicationHandler;
	private MessageCreator recodingScheme;
	private boolean isClosed = false;
	private int timeToWaitForFurtherData; // in ns
	private ScheduledThreadPoolExecutor scheduler;
	private ScheduledFuture<TimeoutTask> currentTimer;
	private ByteBuffer payloadForNextMessage;
	
	
	protected WaitForFurtherDataOutputStream_v1(
			ClientCommunicationBehaviour owner, 
			MixCommunicationHandler mixCommunicationHandler, 
			MessageCreator recodingScheme
		) {

		this.owner = owner;
		this.mixCommunicationHandler = mixCommunicationHandler;
		this.recodingScheme = recodingScheme;
		this.timeToWaitForFurtherData = owner.getSettings().getPropertyAsInt("TIME_TO_WAIT_FOR_FURTHER_DATA");
		this.scheduler = new ScheduledThreadPoolExecutor(1);
	}


	@Override
	public void close() throws IOException {
		this.isClosed = true;
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
	
	
	private void write(ByteBuffer toWrite) throws IOException {
		if (isClosed) {
			throw new IOException("OutputStream closed!");
		} else {
			synchronized (payloadForNextMessage) {
				if (payloadForNextMessage == null)
					payloadForNextMessage = ByteBuffer.allocate(recodingScheme.getMaxPayloadForNextMessage());
				
				if (payloadForNextMessage.remaining() > toWrite.remaining()) {
					payloadForNextMessage.put(toWrite);
					if (currentTimer != null && currentTimer.isDone())
						currentTimer = scheduler.schedule(new TimeoutTask(payloadForNextMessage), timeToWaitForFurtherData, TimeUnit.NANOSECONDS);
				} else {
					while (toWrite.hasRemaining()) { // send as many full packets as possible; store additional data (that doesn't fill a complete packet) for later sending (+set timeout for sending)
						if (payloadForNextMessage == null)
							payloadForNextMessage = ByteBuffer.allocate(recodingScheme.getMaxPayloadForNextMessage());
						if (toWrite.remaining() >= payloadForNextMessage.remaining()) { // send new message now
							byte[] data = new byte[payloadForNextMessage.remaining()];
							toWrite.get(data);
							payloadForNextMessage.put(data);
						} else { // store data for later sending; set timeout if not already done
							payloadForNextMessage.put(toWrite);
							if (currentTimer != null && currentTimer.isDone())
								currentTimer = scheduler.schedule(new TimeoutTask(payloadForNextMessage), timeToWaitForFurtherData, TimeUnit.NANOSECONDS);
						}
					}
					 // set timeout if needed
					if (payloadForNextMessage != null && payloadForNextMessage.hasRemaining() && currentTimer != null && currentTimer.isDone())
						currentTimer = scheduler.schedule(new TimeoutTask(payloadForNextMessage), timeToWaitForFurtherData, TimeUnit.NANOSECONDS);
				}
			}
		}
		
	}
	
	
	private int sendNow() throws IOException {
		if (!payloadForNextMessage.hasRemaining())
			return 0;
		if (currentTimer != null && !currentTimer.isDone())
			currentTimer.cancel(false);
		byte[] payload = new byte[payloadForNextMessage.remaining()];
		payloadForNextMessage.get(payload);
		Request request = recodingScheme.createMessage(payload);
		mixCommunicationHandler.sendMessage(request);
		payloadForNextMessage = null;
		return payload.length;
	}
	
	
	@Override
	public void write(int b) throws IOException {
		if (isClosed) {
			throw new IOException("OutputStream closed!");
		} else {
			synchronized (payloadForNextMessage) {
				if (payloadForNextMessage == null) {
					payloadForNextMessage = ByteBuffer.allocate(recodingScheme.getMaxPayloadForNextMessage());
					payloadForNextMessage.put((byte)b);
					if (currentTimer != null && currentTimer.isDone())
						currentTimer = scheduler.schedule(new TimeoutTask(payloadForNextMessage), timeToWaitForFurtherData, TimeUnit.NANOSECONDS);
				} else if (payloadForNextMessage.remaining() == 1) {
					payloadForNextMessage.put((byte)b);
					sendNow();
				} else if (payloadForNextMessage.remaining() == 0) {
					sendNow();
					write(b);
				}
			}
		}
	}


	
	@Override
	public void flush() throws IOException {
		synchronized (payloadForNextMessage) {
			sendNow();
		}
	}

	
	private class TimeoutTask implements Callable<TimeoutTask> {

		private ByteBuffer associatedByteBuffer;
		
		protected TimeoutTask(ByteBuffer associatedByteBuffer) {
			this.associatedByteBuffer = associatedByteBuffer;
		}



		@Override
		public TimeoutTask call() throws Exception {
			synchronized (payloadForNextMessage) {
				if (associatedByteBuffer == payloadForNextMessage) {
					try {
						sendNow();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return this;
			}
		}
		
	}
	
}

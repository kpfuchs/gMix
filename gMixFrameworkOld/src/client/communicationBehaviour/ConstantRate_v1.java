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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import recodingScheme.MessageCreator;

import message.Reply;
import message.Request;
import framework.Warning;


public class ConstantRate_v1 extends ClientCommunicationBehaviour {

	private BasicInputStream_v1 streamFromMixToClient;
	private ConstantRateOutputStream streamFromClientToMix;
	private MessageCreator recodingScheme;
	private ByteBuffer payloadForNextMessage;
	private Object payloadForNextMessageSync = new Object();
	private volatile boolean waitingForReply = false;
	private volatile boolean userDataQueued = false;
	private volatile boolean sending = false;
	private Reply reply;
	private int sendPeriod; // in ns
	private ReplyThread replyThread;
	private TimeoutTask timeoutTask;
	private ScheduledThreadPoolExecutor scheduler;
	private ScheduledFuture<?> currentTimer;
	
	
	@Override
	public void constructor() {
		this.streamFromMixToClient = new BasicInputStream_v1(client.getRecodingSchemeWrapper());
		this.streamFromClientToMix = new ConstantRateOutputStream();
		this.recodingScheme = client.getRecodingSchemeWrapper();
		this.sendPeriod = settings.getPropertyAsInt("SEND_PERIOD");
		this.replyThread = new ReplyThread();
		
	}

	
	@Override
	public void initialize() {
	}

	
	@Override
	public void begin() {
		payloadForNextMessage = ByteBuffer.allocate(recodingScheme.getMaxPayloadForNextMessage());
	}

	
	@Override
	public void connect() throws IOException {
		mixCommunicationHandler.connect();
		synchronized (payloadForNextMessageSync) {
			payloadForNextMessage = ByteBuffer.allocate(recodingScheme.getMaxPayloadForNextMessage());
		}
		timeoutTask = new TimeoutTask(); 
		scheduler = new ScheduledThreadPoolExecutor(1);
		currentTimer = scheduler.scheduleAtFixedRate(timeoutTask, sendPeriod, sendPeriod, TimeUnit.MILLISECONDS);
	}

	
	@Override
	public void disconnect() throws IOException {
		currentTimer.cancel(false);
		timeoutTask.stopSending();
		scheduler.shutdownNow();
		replyThread.stopExecution();
		mixCommunicationHandler.disconnect();
	}

	
	@Override
	public InputStream getInputStream() {
		return streamFromMixToClient;
	}

	
	@Override
	public OutputStream getOutputStream() {
		return streamFromClientToMix;
	}
	
	
	@Override
	public void incomingReply(Reply reply) {
		streamFromMixToClient.addReply(reply);
	}
	
	
	private void sendMessage() {
		synchronized (payloadForNextMessageSync) {
			Request request;
			if (payloadForNextMessage == null || payloadForNextMessage.position() == 0) // send dummy
				request = recodingScheme.createMessage(null);
			else {
				request = recodingScheme.createMessage(payloadForNextMessage.array());
				payloadForNextMessage = ByteBuffer.allocate(recodingScheme.getMaxPayloadForNextMessage());
			}
			try {
				mixCommunicationHandler.sendMessage(request);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (userDataQueued)
				payloadForNextMessageSync.notifyAll();
		}
	}
	
	
	private class TimeoutTask implements Runnable {

		private boolean stopped = false;
		private Warning warning = new Warning(client +" cannont send messages fast enough (ConstantRate_v1.java)", 2000);
		
		
		public void stopSending() {
			synchronized (payloadForNextMessageSync) {
				stopped = true;
			}
		}
		
		
		@Override
		public void run() {
			if (sending)
				warning.warn();
			sending = true;
			synchronized (payloadForNextMessageSync) {
				if (!stopped) {
					sendMessage();
					sending = false;
				}
			}
		}
		
	}
	
	
	private class ReplyThread extends Thread {
		
		private volatile boolean stopped = false;
		
		
		@Override
		public void run() {
			
			while (true) {
				synchronized(replyThread) {
					waitingForReply = true;
					while (waitingForReply) {
						try {
							replyThread.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
							continue;	
						}	
					}
					streamFromMixToClient.addReply(reply);
				}
				synchronized (replyThread) {
					if (stopped)
						break;
				}
			}

		}

		public void stopExecution() {
			synchronized (replyThread) {
				stopped = true;
			}
		}
		
	}
	
	
	private class ConstantRateOutputStream extends OutputStream {

		private boolean isClosed = false;
		
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
				synchronized (payloadForNextMessageSync) {
					if (payloadForNextMessage.remaining() > toWrite.remaining())
						payloadForNextMessage.put(toWrite);
					else { // block till all data is written
						while (toWrite.hasRemaining()) {
							if (payloadForNextMessage.hasRemaining()) {
								int length = recodingScheme.getMaxPayloadForNextMessage() > toWrite.remaining() ? toWrite.remaining() : recodingScheme.getMaxPayloadForNextMessage();
								byte[] payload = new byte[length];
								toWrite.get(payload);
								payloadForNextMessage.put(payload);
							}
								
							
							if (toWrite.hasRemaining()) { // wait till next message can be sent
								userDataQueued = true;
								while (!payloadForNextMessage.hasRemaining()) {
									try {
										payloadForNextMessageSync.wait();
									} catch (InterruptedException e) {
										e.printStackTrace();
										continue;	
									}	
								}
								userDataQueued = false;
							}
						}
					}
				}
			}
		}
		
		
		@Override
		public void write(int b) throws IOException {
			if (isClosed) {
				throw new IOException("OutputStream closed!");
			} else {
				synchronized (payloadForNextMessageSync) {
					if (payloadForNextMessage.hasRemaining())
						payloadForNextMessage.put((byte)b);
					else
						write(new byte[] {(byte)b});
				}
			}
		}


		@Override
		public void flush() throws IOException {
			// don't do anything; mix protocol decides
		}

	}

}

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
import java.util.concurrent.atomic.AtomicBoolean;

import recodingScheme.MessageCreator;

import message.Reply;
import message.Request;


public class WaitForReply_v1 extends ClientCommunicationBehaviour {

	private BasicInputStream_v1 streamFromMixToClient;
	private WaitForReplyOutputStream streamFromClientToMix;
	private MessageCreator recodingScheme;
	private ByteBuffer payloadForNextMessage;
	private volatile boolean waitingForReply = false;
	private volatile boolean userDataQueued = false;
	private volatile AtomicBoolean disconnectRequested;
	private Reply reply;
	private int delay; // in ns
	private RequestReplyThread requestReplyThread;

	
	@Override
	public void constructor() {
		this.recodingScheme = client.getRecodingSchemeWrapper();
		this.streamFromMixToClient = new BasicInputStream_v1(recodingScheme);
		this.streamFromClientToMix = new WaitForReplyOutputStream();
		this.delay = settings.getPropertyAsInt("DELAY");
		this.requestReplyThread = new RequestReplyThread();
	}

	
	@Override
	public void initialize() {
	}

	
	@Override
	public void begin() {

	}

	
	@Override
	public void connect() throws IOException {
		mixCommunicationHandler.connect();
		synchronized (disconnectRequested) {
			disconnectRequested = new AtomicBoolean(false);
		}
		synchronized (payloadForNextMessage) {
			payloadForNextMessage = ByteBuffer.allocate(recodingScheme.getMaxPayloadForNextMessage());
		}
		try {
			Thread.sleep(0, delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		requestReplyThread.start();
	}

	
	@Override
	public void disconnect() throws IOException {
		synchronized (disconnectRequested) {
			disconnectRequested.set(true);
			while(disconnectRequested.get() == true) {
				try {
					disconnectRequested.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;	
				}	
			}
			mixCommunicationHandler.disconnect();
		}
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
		synchronized(requestReplyThread) {
			this.reply = reply;
			if (waitingForReply) {
				waitingForReply = false;
				requestReplyThread.notifyAll();
			}
		}
	}
	
	
	
	private class RequestReplyThread extends Thread {
		
		@Override
		public void run() {
			
			while (true) {
				
				// send request
				synchronized (payloadForNextMessage) {
					Request request;
					if (payloadForNextMessage == null || payloadForNextMessage.position() == 0)
						request = recodingScheme.createMessage(null);
					else {
						request = recodingScheme.createMessage(payloadForNextMessage.array());
						payloadForNextMessage = ByteBuffer.allocate(recodingScheme.getMaxPayloadForNextMessage());
					}
					try {
						mixCommunicationHandler.sendMessage(request);
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
					if (userDataQueued)
						payloadForNextMessage.notifyAll();
				}
				
				// wait for reply
				synchronized(requestReplyThread) {
					waitingForReply = true;
					while (waitingForReply) {
						try {
							requestReplyThread.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
							continue;	
						}	
					}
					streamFromMixToClient.addReply(reply);
				}
				
				// discconnect if requested
				synchronized (disconnectRequested) {
					if (disconnectRequested.get() == true) {
						disconnectRequested.set(false);
						disconnectRequested.notifyAll();
						break;
					}
				}
				
				// wait "delay"
				try {
					Thread.sleep(0, delay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}

		}
		
	}

	
	private class WaitForReplyOutputStream extends OutputStream {

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
				synchronized (payloadForNextMessage) {
					if (payloadForNextMessage.remaining() > toWrite.remaining())
						payloadForNextMessage.put(toWrite);
					else { // block till all data is written
						while (toWrite.hasRemaining()) {
							int length = recodingScheme.getMaxPayloadForNextMessage() > toWrite.remaining() ? toWrite.remaining() : recodingScheme.getMaxPayloadForNextMessage();
							byte[] payload = new byte[length];
							toWrite.get(payload);
							payloadForNextMessage.put(payload);
							if (toWrite.hasRemaining()) { // wait till next message can be sent
								userDataQueued = true;
								while (!payloadForNextMessage.hasRemaining()) {
									try {
										payloadForNextMessage.wait();
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
			// TODO
			if (isClosed) {
				throw new IOException("OutputStream closed!");
			} else {
				synchronized (payloadForNextMessage) {
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

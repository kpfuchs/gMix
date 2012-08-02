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
import java.io.InputStream;
import java.nio.ByteBuffer;

import framework.core.AnonNode;


public class BasicInputStreamMix extends InputStream {
	
	private boolean isClosed = false;
	private ByteBuffer buffer= null;
	private StreamAnonSocketMixImpl socket;
	
	
	public BasicInputStreamMix(
			AnonNode owner,
			StreamAnonSocketMixImpl socket
			) {
		this.socket = socket;
		String requestBufferSize = owner.LAYER_4_MIX_INPUT_STREAM_REQUEST_BUFFER_SIZE;
		if (requestBufferSize.equalsIgnoreCase("AUTO")) {
			this.buffer = ByteBuffer.allocate((int)Math.ceil(((double)owner.SOCKET_MIX_BACKEND_QUEUE_SIZE * (double)owner.getOutputStrategyLayerControllerMix().getMaxSizeOfNextRequest() * 1.5)));
		} else {
			this.buffer = ByteBuffer.allocate(Integer.parseInt(requestBufferSize));
		}
		this.buffer.flip();
	}

	
	// will block till "len" bytes are read
	private synchronized int forceRead(byte[] b, int off, int len) throws IOException {
		if (isClosed)
			throw new IOException("InputStream closed");
		if (buffer.remaining() >= len) { // enough data in buffer
			buffer.get(b, off, len);
			tryFillBuffer();
			return len;
		} else { // not enough data in buffer
			int transfered = 0;
			while (true) {
				forceFillBuffer();
				if (buffer.remaining() >= len-transfered) { // enough data
					buffer.get(b, off, len-transfered);
					return len;
				} else { // not enough data
					int nowAvailable = buffer.remaining();
					buffer.get(b, off, nowAvailable);
					off += nowAvailable;
					transfered += nowAvailable;
				}
			}
		}
	}
	
	
	private synchronized void tryFillBuffer() {
		if (socket.availableRequests() > 0) {
			buffer.compact();
			while (socket.availableRequests() > 0 && buffer.remaining() >= socket.sizeOfNextRequest())
				buffer.put(socket.getNextRequest().getByteMessage());
			buffer.flip();	
		}
	}
	
	private synchronized void forceFillBuffer() {
		buffer.compact();
		byte[] data = socket.getNextRequest().getByteMessage();
		if (data.length > buffer.remaining()) { // resize buffer
			System.out.println("resizing buffer (" +buffer.capacity() +"->" +(buffer.position() +data.length) +")"); 
			ByteBuffer old = buffer;
			buffer = ByteBuffer.allocate(buffer.position() +data.length);
			buffer.put(old);
		}
		buffer.put(data);
		while (socket.availableRequests() > 0 && buffer.remaining() >= socket.sizeOfNextRequest()) 
			buffer.put(socket.getNextRequest().getByteMessage());
		buffer.flip();
	}
	
	
	// "Reads the next byte of data from the input stream."
	@Override
	public synchronized int read() throws IOException {
		if (buffer.hasRemaining())
			return buffer.get();
		else {
			byte[] result = new byte[1];
			read(result);
			return result[0];
		}
	}
	
	
	@Override
	public synchronized int read(byte[] b) throws IOException {
		return forceRead(b, 0, b.length);
	}
	
	
	@Override
	public synchronized int read(byte[] b, int off, int len) throws IOException {
		return forceRead(b, off, len);
	}

	
	@Override
	public synchronized int available() throws IOException {
		tryFillBuffer();
		return buffer.remaining();
	}
	
	
	@Override
	public synchronized void close() throws IOException {
		if (this.isClosed) {
			throw new IOException("InputStream already closed!");
		} else {
			this.isClosed = true;
		}	
	}
	

}

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
import framework.core.interfaces.Layer3OutputStrategyClient;
import framework.core.util.Util;


public class BasicInputStreamClient extends InputStream {

	private Layer3OutputStrategyClient layer3;
	private boolean isClosed = false;
	private ByteBuffer remaining = null;
	
	
	// TODO: add limit for read-method (if read(numBytes) is called with numBytes > availableRAM an OutOfMemoryException will occur) 
	public BasicInputStreamClient(
			AnonNode owner,
			Layer3OutputStrategyClient layer3
			) {
		this.layer3 = layer3;
		String replyBufferSize = owner.LAYER_4_CLIENT_INPUT_STREAM_REPLY_BUFFER_SIZE;
		if (replyBufferSize.equalsIgnoreCase("AUTO")) {
			this.remaining = ByteBuffer.allocate(layer3.getMaxSizeOfNextReply() * 2);
			this.remaining.flip();
		} else {
			this.remaining = ByteBuffer.allocate(Integer.parseInt(replyBufferSize));
			this.remaining.flip();
		}
	}
	

	// will block till "len" bytes are read
	private synchronized byte[] forceRead(byte[] b, int off, int len) throws IOException {
		if (isClosed)
			throw new IOException("InputStream closed");
		ByteBuffer result = ByteBuffer.wrap(b, off, len);
		if (remaining.hasRemaining() && result.remaining() <= remaining.remaining()) { // enough data in buffer
			byte[] msg = new byte[result.remaining()];
			remaining.get(msg);
			result.put(msg);
			return result.array();
		} else { // not enough data in buffer
			if (remaining.hasRemaining()) {
				byte[] msg = new byte[remaining.remaining()];
				remaining.get(msg);
				result.put(msg);
			}
			while (result.hasRemaining()) { // receive as many messages as needed
				byte[] newData = layer3.receiveReply().getByteMessage();
				if (newData.length > result.remaining()) { // received more data than needed; return result and store rest of data for later use
					byte[][] chunks = Util.split(result.remaining(), newData);
					result.put(chunks[0]);
					remaining.clear();
					remaining.put(chunks[1]);
					remaining.flip();
					return result.array();
				} else if (newData.length == result.remaining()) { // received exactly enough data
					result.put(newData);
					return result.array();
				} else { // received less data than needed
					result.put(newData);
				}
			}
		}
		throw new RuntimeException("implementation error; see while loop above (should always terminate with return...)"); 
	}
	
	
	// "Reads the next byte of data from the input stream."
	@Override
	public synchronized int read() throws IOException {
		System.out.println("read()"); 
		if (remaining.hasRemaining())
			return remaining.get();
		else {
			byte[] result = new byte[1];
			read(result);
			return result[0];
		}
	}
	
	
	@Override
	public int read(byte[] b) throws IOException {
		return forceRead(b, 0, b.length).length;
	}
	
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		forceRead(b, off, len);
		return len;
	}

	
	@Override
	public synchronized int available() throws IOException {
		return remaining.remaining() + layer3.availableReplyPayload();
	}
	
	
	@Override
	public void close() throws IOException {
		if (this.isClosed) {
			throw new IOException("InputStream already closed!");
		} else {
			this.isClosed = true;
		}	
	}
	
}

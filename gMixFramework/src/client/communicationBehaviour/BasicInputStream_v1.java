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
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import recodingScheme.MessageCreator;

import message.Reply;


public class BasicInputStream_v1 extends InputStream {

	private MessageCreator recodingScheme;
	private LinkedBlockingQueue<Reply> replyQueue;
	private ByteBuffer replyBuffer;
	private boolean isClosed = false;
	private final static int _100kbyte = 100*1000;
	private final byte[] buf = new byte[_100kbyte]; // used to skip data
	// TODO: wert einführen, der angibt nach wie vielen gelesenen bytes die read-methoden spätestens etwas zurückgeben sollen (return). sinn: read(16 gbyte) soll nicht speicher volllaufen lassen  
	
	
	protected BasicInputStream_v1(MessageCreator recodingScheme) {
		this.recodingScheme = recodingScheme;
		this.replyQueue = new LinkedBlockingQueue<Reply>();
	}
	
	
	protected void addReply(Reply reply) {
		replyQueue.add(reply);
	}
	
	
	// will block till "len" bytes are read
	private byte[] forceRead(byte[] b, int off, int len) throws IOException {
		
		if (isClosed)
			throw new IOException("InputStream closed!");
			
		ByteBuffer result = ByteBuffer.wrap(b, off, len);
		
		while (result.hasRemaining()) {
			
			// wait for data in replyBuffer if necessarry
			while (replyBuffer == null || !replyBuffer.hasRemaining()) {
				try {
					Reply nextReply = replyQueue.take();
					byte[] payload = recodingScheme.extractPayload(nextReply);
					if (payload == null || payload.length == 0) // dummy
						continue;
					replyBuffer = ByteBuffer.wrap(payload);
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}
			}
			
			if (result.remaining() > replyBuffer.remaining()) {
				result.put(replyBuffer);
			} else {
				byte[] data = new byte[result.remaining()];
				replyBuffer.get(data);
				result.put(data);
			}
			
		}
		
		return result.array();

	}
	
	
	// "Reads the next byte of data from the input stream."
	@Override
	public int read() throws IOException {
		if (replyBuffer.hasRemaining())
			return replyBuffer.get();
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
		return forceRead(b, off, len).length;
	}

	
	@Override
	public long skip(long n) throws IOException {
		long leftToSkip = n;
		while (leftToSkip > 0) {
			byte[] skipMe = leftToSkip > _100kbyte ? this.buf : new byte[(int)leftToSkip];
			read(skipMe);
			leftToSkip -= skipMe.length;
		}
		return n;
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

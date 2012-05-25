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

import recodingScheme.MessageCreator;

import message.Request;

import client.mixCommunicationHandler.MixCommunicationHandler;


public class SendImmediatelyOutputStream_v1 extends OutputStream {
	
	@SuppressWarnings("unused")
	private ClientCommunicationBehaviour owner;
	private MixCommunicationHandler mixCommunicationHandler;
	private MessageCreator recodingScheme;
	private boolean isClosed = false;
	
	
	protected SendImmediatelyOutputStream_v1(
			ClientCommunicationBehaviour owner, 
			MixCommunicationHandler mixCommunicationHandler, 
			MessageCreator recodingScheme
			) {
		
		this.owner = owner;
		this.mixCommunicationHandler = mixCommunicationHandler;
		this.recodingScheme = recodingScheme;
		
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
			while (toWrite.hasRemaining()) {
				int length = recodingScheme.getMaxPayloadForNextMessage() > toWrite.remaining() ? toWrite.remaining() : recodingScheme.getMaxPayloadForNextMessage();
				byte[] payload = new byte[length];
				toWrite.get(payload);
				Request request = recodingScheme.createMessage(payload);
				mixCommunicationHandler.sendMessage(request);
			}
		}
	}
	
	
	@Override
	public void write(int b) throws IOException {
		if (isClosed) {
			throw new IOException("OutputStream closed!");
		} else {
			Request request = recodingScheme.createMessage(new byte[] {(byte)b});
			mixCommunicationHandler.sendMessage(request);
		}
	}


	@Override
	public void flush() throws IOException {
		// don't do anything; data is always sent directly
	}

}

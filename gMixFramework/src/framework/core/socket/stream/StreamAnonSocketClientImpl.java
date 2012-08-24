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
import java.io.OutputStream;

import framework.core.AnonNode;
import framework.core.socket.socketInterfaces.AdaptiveAnonSocket;
import framework.core.socket.socketInterfaces.StreamAnonSocket;



public class StreamAnonSocketClientImpl extends AdaptiveAnonSocket implements StreamAnonSocket {

	private boolean isConnected = false;
	private BasicOutputStreamClient outputStream = null;
	private BasicInputStreamClient inputStream = null;
	
		
	public StreamAnonSocketClientImpl(
			AnonNode owner,
			CommunicationMode communicationMode,
			boolean isFreeRoute
			) {
		super(	owner, 
				communicationMode, 
				true, 
				true,
				true, 
				isFreeRoute
				);
	}


	@Override
	public void connect(int destinationPort) throws IOException {
		layer3.connect();
		
		this.destinationPort = destinationPort;
		this.outputStream = new BasicOutputStreamClient(this, layer3);
		if (isDuplex)
			this.inputStream =  new BasicInputStreamClient(owner, layer3);
		this.isConnected = true;
	}


	@Override
	public void connect(int destinationPseudonym, int destinationPort) throws IOException {
		if (!isFreeRoute)
			throw new RuntimeException("this is a fixed route socket; you cannot specify a destination address; use \"connect(destinationPort)\" instead"); 
		layer3.connect(destinationPseudonym);
		
		this.destinationPseudonym = destinationPseudonym;
		this.destinationPort = destinationPort;
		this.outputStream = new BasicOutputStreamClient(this, layer3);
		if (isDuplex)
			this.inputStream =  new BasicInputStreamClient(owner, layer3);
		this.isConnected = true;
	}


	@Override
	public void disconnect() throws IOException {
		layer3.disconnect();
		this.isConnected = false;
		this.outputStream.close();
		this.outputStream = null;
		if (isDuplex) {
			this.inputStream.close();
			this.inputStream = null;
		}
	}


	@Override
	public boolean isConnected() {
		return this.isConnected;
	}


	@Override
	public OutputStream getOutputStream() throws IOException {
		if (!isConnected)
			throw new IOException("not connected"); 
		return this.outputStream;
	}


	@Override
	public InputStream getInputStream() throws IOException {
		if (!isDuplex)
			throw new RuntimeException("this is a simplex socket"); 
		if (!isConnected)
			throw new IOException("not connected"); 
		return this.inputStream;
	}


	@Override
	public int getMTU() {
		return outputStream.getMaxSizeForNextMessageSend();
	}


	@Override
	public AdaptiveAnonSocket getImplementation() {
		return this;
	}

}

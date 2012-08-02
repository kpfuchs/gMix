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
import framework.core.socket.socketInterfaces.StreamAnonSocketMix;
import framework.core.userDatabase.User;


public class StreamAnonSocketMixImpl extends AdaptiveAnonSocket implements StreamAnonSocketMix {

	private boolean isConnected = false;
	private StreamAnonServerSocketImpl serverSocket;
	private BasicInputStreamMix inputStream = null;
	private BasicOutputStreamMix outputStream = null;
	private User user;
	
	
	public StreamAnonSocketMixImpl(
			StreamAnonServerSocketImpl serverSocket,
			User user,
			AnonNode owner,
			int endToEndPseudonym,
			CommunicationMode communicationMode,
			boolean isFreeRoute
			) {
		super(	owner, 
				endToEndPseudonym,
				communicationMode, 
				true, 
				true,
				true, 
				isFreeRoute
				);
		this.serverSocket = serverSocket;
		this.user = user;
		this.isConnected = true;
		if (isDuplex && !owner.LAYER_1_LINKS_MESSAGES)
			throw new RuntimeException("currently not supported"); // TODO: requires same user-object for each message (maybe this is a general problem and can't be solved without layer1 linkage) 
		this.inputStream = new BasicInputStreamMix(owner, this);
		if (isDuplex)
			this.outputStream = new BasicOutputStreamMix(owner, this, user);
	}

	
	@Override
	public void disconnect() throws IOException {
		if (!isConnected)
			throw new IOException("not connected");
		serverSocket.disconnect(endToEndPseudonym);
		isConnected = false;
	}

	
	@Override
	public boolean isConnected() {
		return isConnected;
	}

	
	@Override
	public OutputStream getOutputStream() {
		if (!isDuplex)
			throw new RuntimeException("this is a simplex socket"); 
		return this.outputStream;
	}

	
	@Override
	public InputStream getInputStream() {
		return this.inputStream;
	}


	@Override
	public User getUser() {
		return user;
	}


	@Override
	public AdaptiveAnonSocket getImplementation() {
		return this;
	}

}

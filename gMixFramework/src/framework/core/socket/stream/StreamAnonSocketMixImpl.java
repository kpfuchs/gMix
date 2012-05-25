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

}

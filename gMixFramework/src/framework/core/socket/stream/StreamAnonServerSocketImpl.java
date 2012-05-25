package framework.core.socket.stream;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import framework.core.AnonNode;
import framework.core.message.Request;
import framework.core.socket.socketInterfaces.AdaptiveAnonServerSocket;
import framework.core.socket.socketInterfaces.StreamAnonServerSocket;
import framework.core.socket.socketInterfaces.StreamAnonSocketMix;
import framework.core.util.Util;


public class StreamAnonServerSocketImpl extends AdaptiveAnonServerSocket implements StreamAnonServerSocket {

	private ConcurrentHashMap<Integer, StreamAnonSocketMixImpl> sockets;
	private LinkedBlockingQueue<StreamAnonSocketMixImpl> newConncetions;
	
	
	public StreamAnonServerSocketImpl(
			AnonNode owner,
			int bindPseudonym,
			int bindPort,
			CommunicationMode communicationMode,
			boolean isFreeRoute
			) {
		
		super(	owner,
				bindPseudonym, 
				bindPort, 
				communicationMode, 
				true,
				true, 
				true, 
				isFreeRoute);
		if (communicationMode == CommunicationMode.DUPLEX && !owner.IS_DUPLEX)
			throw new RuntimeException("the current plug-in config does not suport duplex sockets");
		if (communicationMode == CommunicationMode.SIMPLEX_SENDER)
			throw new RuntimeException("this is a simplex socket (server backend); the server backend can only be \"CommunicationMode.SIMPLEX_RECEIVER\"");
		this.sockets = new  ConcurrentHashMap<Integer, StreamAnonSocketMixImpl>((int) (owner.EXPECTED_NUMBER_OF_USERS * 1.2));
		this.newConncetions = new LinkedBlockingQueue<StreamAnonSocketMixImpl>();
	}

	
	
	@Override
	public StreamAnonSocketMix accept() {
		try {
			return newConncetions.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return accept();
		}
	}

	
	private void putInNewConnectionQueue(StreamAnonSocketMixImpl socket) {
		try {
			newConncetions.put(socket);
		} catch (InterruptedException e) {
			e.printStackTrace();
			putInNewConnectionQueue(socket);
		}
	}
	
	
	@Override
	public void incomingRequest(Request request) {
		int endToEndPseudonym;
		if (owner.LAYER_1_LINKS_MESSAGES) { // use user-id as pseudonym
			endToEndPseudonym = request.getOwner().getIdentifier();
		} else { // extract pseudonym
			endToEndPseudonym = Util.byteArrayToInt(Arrays.copyOf(request.getByteMessage(), 4));
			request.setByteMessage(Arrays.copyOfRange(request.getByteMessage(), 4, request.getByteMessage().length));
		}
		StreamAnonSocketMixImpl socket = sockets.get(endToEndPseudonym);
		if (socket == null) {
			socket = new StreamAnonSocketMixImpl(
					this,
					request.getOwner(),
					owner, 
					endToEndPseudonym,
					communicationMode,
					isFreeRoute);
			sockets.put(endToEndPseudonym, socket);
			putInNewConnectionQueue(socket);
		}
		socket.newIncomingMessage(request);
	}
	

	public void disconnect(int endToEndPseudonym) {
		sockets.remove(endToEndPseudonym);
	}

}
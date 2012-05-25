package framework.core.socket.datagram;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import framework.core.AnonNode;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.socket.socketInterfaces.AdaptiveAnonSocket;
import framework.core.socket.socketInterfaces.AnonMessage;
import framework.core.socket.socketInterfaces.DatagramAnonSocket;
import framework.core.util.Util;


public class DatagramAnonSocketClientImpl extends AdaptiveAnonSocket implements DatagramAnonSocket {

	private HashMap<Integer, Integer> pseudonymToDestAddress = null;
	
	
	public DatagramAnonSocketClientImpl(
			AnonNode owner,
			CommunicationMode communicationMode,
			boolean isReliable, 
			boolean isOrderPreserving, 
			boolean isFreeRoute
			) {
		super(	owner, 
				communicationMode, 
				false, 
				isReliable,
				isOrderPreserving, 
				isFreeRoute
				);
		if (isDuplex)
			this.pseudonymToDestAddress = new HashMap<Integer, Integer>(100);
	}

	
	@Override
	public void sendMessage(int destPort, byte[] payload) {
		if (!isFreeRoute)
			throw new RuntimeException("no destination address specified; use \"sendMessage(destinationPseudonym, destPort, payload\" instead"); 

		payload = Util.concatArrays(Util.shortToByteArray(destinationPort), payload); // add destination port (= which layer 5 service/ServerSocket shall be addressed)
		Request request = MixMessage.getInstanceRequest(payload);
		layer3.sendMessage(request);
	}


	@Override
	public void sendMessage(int destinationPseudonym, int destinationPort, byte[] payload) {
		if (!isFreeRoute)
			throw new RuntimeException("this is a fixed route socket; you cannot specify a destination address; use \"sendMessage(destPort, payload\" instead"); 
		
		if (isDuplex) { // add a pseudonym so the receiver's reply can be identified (the receiver will include this pseudonym in his reply)
			int endToEndPseudonym = new Random().nextInt(); // TODO
			pseudonymToDestAddress.put(endToEndPseudonym, destinationPseudonym);
			payload = Util.concatArrays(Util.intToByteArray(endToEndPseudonym), payload);
		}
		payload = Util.concatArrays(Util.shortToByteArray(destinationPort), payload); // add destination port (= which layer 5 service/ServerSocket shall be addressed)
		Request request = MixMessage.getInstanceRequest(payload);
		request.destinationPseudonym = destinationPseudonym;
		layer3.sendMessage(request);
	}


	@Override
	public AnonMessage receiveMessage() {
		if (!isDuplex)
			throw new RuntimeException("this is a simplex socket"); 
		
		Reply reply = layer3.receiveReply();
		AnonMessage result = new AnonMessage(reply.getByteMessage());
		
		int sourcePort = Util.byteArrayToShort(Arrays.copyOf(reply.getByteMessage(), 2));
		result.setByteMessage(Arrays.copyOfRange(reply.getByteMessage(), 2, reply.getByteMessage().length));
		result.setSourcePort(sourcePort);
				
		if (!isFreeRoute) {
			int endToEndPseudonym = Util.byteArrayToInt(Arrays.copyOf(result.getByteMessage(), 4));
			result.setByteMessage(Arrays.copyOfRange(result.getByteMessage(), 4, result.getByteMessage().length));
			Integer sourcePseudonym = pseudonymToDestAddress.remove(endToEndPseudonym);
			if (sourcePseudonym == null) {
				System.err.println("received reply with unknown id");
				return receiveMessage();
			}
			result.setSourcePseudonym(sourcePseudonym);
		}
		return result;
	}

	
	@Override
	public int getMaxSizeForNextMessageSend() {
		return layer3.getMaxSizeOfNextRequest() -6; // -2 for port; -4 for pseudonym; see sendMessage()
	}

	
	@Override
	public int getMaxSizeForNextMessageReceive() {
		if (!isDuplex)
			throw new RuntimeException("this socket is simplex only"); 
		int maxSize = layer3.getMaxSizeOfNextReply() -2; // -2 for port
		if (isFreeRoute)
			maxSize -= 4; // pseudonym
		return maxSize;
	}
}

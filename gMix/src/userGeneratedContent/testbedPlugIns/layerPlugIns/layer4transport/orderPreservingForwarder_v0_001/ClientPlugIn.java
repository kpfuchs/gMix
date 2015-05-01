/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
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
 *******************************************************************************/
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer4transport.orderPreservingForwarder_v0_001;

import java.util.PriorityQueue;

import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer1NetworkClient;
import staticContent.framework.interfaces.Layer2RecodingSchemeClient;
import staticContent.framework.interfaces.Layer3OutputStrategyClient;
import staticContent.framework.interfaces.Layer4TransportClient;
import staticContent.framework.util.Util;


public class ClientPlugIn extends Implementation implements Layer4TransportClient {

	private Layer3OutputStrategyClient layer3;
	private PriorityQueue<QueueElement<byte[]>> queuedReplies;
	private int nextSequenceNumberSend = -1;
	private int nextSequenceNumberReceive = 0;
	private int dstPseudonym = Util.NOT_SET;
	
	
	@Override
	public void constructor() {
		queuedReplies = new PriorityQueue<QueueElement<byte[]>>();
		System.out.println("loaded " +this +" on " +anonNode.PUBLIC_PSEUDONYM); 
	}


	@Override
	public void initialize() {
		
	}


	@Override
	public void begin() {
		
	}


	@Override
	public void setReferences(Layer1NetworkClient layer1,
			Layer2RecodingSchemeClient layer2,
			Layer3OutputStrategyClient layer3, 
			Layer4TransportClient layer4) {
		assert layer4 == this;
		this.layer3 = layer3;
	}


	@Override
	public byte[] receive() {
		if (nextSequenceNumberReceive == Integer.MAX_VALUE)
			displayErrorMessage();
		if (queuedReplies.peek() != null && queuedReplies.peek().getSequenceNumber() == nextSequenceNumberReceive) { // message already here
			//System.out.println("" +this +": returning " +nextSequenceNumberReceive); // TODO: remove
			nextSequenceNumberReceive++;
			return queuedReplies.remove().getMessage();
		} else { // message not yet here
			while (true) { // wait till the message arrives
				if (nextSequenceNumberReceive == Integer.MAX_VALUE)
					displayErrorMessage();
				byte[] reply = layer3.receive();
				assert reply != null && reply.length > 4;
				byte[][] splitted = Util.split(4, reply);
				QueueElement<byte[]> replyElement = new QueueElement<byte[]>(Util.byteArrayToInt(splitted[0]), splitted[1]);
				if (replyElement.getSequenceNumber() == nextSequenceNumberReceive) { // we are waiting for this message
					//System.out.println("" +this +": waiting for " +nextSequenceNumberReceive +", received: " +replyElement.getSequenceNumber()); // TODO: remove
					//System.out.println("" +this +": received (reply (client)): " +Util.toHex(replyElement.getMessage().getByteMessage())); // TODO: remove
					//System.out.println("" +this +": returning " +nextSequenceNumberReceive); // TODO: remove
					nextSequenceNumberReceive++;
					return replyElement.getMessage();
				} else { // we are not waiting for this message
					queuedReplies.add(replyElement);
					//System.out.println("" +this +": waiting for " +nextSequenceNumberReceive +", received: " +replyElement.getSequenceNumber()); // TODO: remove
				}
			}
		}
	}


	@Override
	public void write(byte[] data) {
		if (data == null || data.length == 0)
			throw new RuntimeException("write(null) and write(byte[0]) are not allowed");
		if (nextSequenceNumberSend == Integer.MAX_VALUE)
			displayErrorMessage();
		nextSequenceNumberSend++;
		byte[] sequenceNumberHeader = Util.intToByteArray(nextSequenceNumberSend);
		byte[] message = Util.concatArrays(sequenceNumberHeader, data);
		if (this.dstPseudonym != Util.NOT_SET)
			layer3.write(message, this.dstPseudonym);
		else
			layer3.write(message);
	}


	@Override
	public void write(byte[] data, int destPseudonym) {
		this.dstPseudonym = destPseudonym;
		write(data);
	}


	@Override
	public void connect() {
		layer3.connect();
	}


	@Override
	public void connect(int destPseudonym) {
		layer3.connect(destPseudonym);
	}


	@Override
	public void disconnect() {
		layer3.disconnect();
	}


	@Override
	public int getMaxSizeOfNextWrite() {
		return layer3.getMaxSizeOfNextWrite() - 4; // seq.-header is 4 bytes long (an int value)
	}


	@Override
	public int getMaxSizeOfNextReply() {
		return layer3.getMaxSizeOfNextReceive() - 4; // seq.-header is 4 bytes long (an int value)
	}


	@Override
	public int availableReplies() {
		return layer3.availableReplies();
	}


	@Override
	public int availableReplyPayload() {
		int sizePerMessage = getMaxSizeOfNextReply();
		int messages = layer3.availableReplies();
		return messages * sizePerMessage;
	}
	
	
	private void displayErrorMessage() {
		throw new RuntimeException(
				"Sorry, the orderPreservingForwarder_v0_001-plugin supports " +
				"a maximum of " +Integer.MAX_VALUE +" * MAX_PAYLOAD bytes " +
				"before its internal sequence number counter reaches its " +
				"limit. Given the current MAX_PAYLOAD of " 
				+anonNode.MAX_PAYLOAD +" bytes, the limit is " 
				+Util.humanReadableByteCount(((long)Integer.MAX_VALUE)*(long)anonNode.MAX_PAYLOAD, true) 
				+" and has now been reached. TODO: add overflow-handling ;)"
			); 
	}

}

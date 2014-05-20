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
package plugIns.layer4transport.orderPreservingForwarder_v0_001;

import java.util.PriorityQueue;

import framework.core.controller.Implementation;
import framework.core.controller.Layer3OutputStrategyMixController;
import framework.core.interfaces.Layer4TransportMix;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.userDatabase.User;
import framework.core.userDatabase.UserAttachment;
import framework.core.util.Util;


public class MixPlugIn extends Implementation implements Layer4TransportMix {

	private Layer3OutputStrategyMixController layer3controller;
	
	
	@Override
	public void constructor() {
		System.out.println("loaded " +this +" on " +anonNode.PUBLIC_PSEUDONYM); 
		this.layer3controller = anonNode.getOutputStrategyLayerControllerMix();
	}

	
	@Override
	public void initialize() {
		
	}

	
	@Override
	public void begin() {
		
	}

	
	@Override
	public void forwardRequest(Request request) {
		UserData userData = request.getOwner().getAttachment(this, UserData.class);
		if (userData == null)
			userData = new UserData(request.getOwner());
		if (userData.nextSequenceNumberReceive == Integer.MAX_VALUE)
			displayErrorMessage();
		byte[][] splitted = Util.split(4, request.getByteMessage());
		request.setByteMessage(splitted[1]);
		QueueElement<Request> requestElement = new QueueElement<Request>(Util.byteArrayToInt(splitted[0]), request);
		if (requestElement.getSequenceNumber() == userData.nextSequenceNumberReceive) { // we are waiting for this message
			//System.out.println("" +this +": waiting for " +userData.nextSequenceNumberReceive +" for " +userData.getOwner() +", received: " +requestElement.getSequenceNumber()); // TODO: remove
			userData.nextSequenceNumberReceive++;
			anonNode.forwardToLayer5(requestElement.getMessage());
			int numberOfQueuedRequests = userData.queuedRequests.size();
			for (int i=0; i<numberOfQueuedRequests; i++) {
				anonNode.forwardToLayer5(userData.queuedRequests.remove().getMessage());
				userData.nextSequenceNumberReceive++;
			}
		} else { // we are not waiting for this message
			//System.out.println("" +this +": waiting for " +userData.nextSequenceNumberReceive +" for " +userData.getOwner() +", received: " +requestElement.getSequenceNumber()); // TODO: remove
			userData.queuedRequests.add(requestElement);
		}
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
	
	private MixPlugIn getThis() {
		return this;
	}
	
	
	private class UserData extends UserAttachment {

		public PriorityQueue<QueueElement<Request>> queuedRequests;
		public int nextSequenceNumberSend = -1;
		public int nextSequenceNumberReceive = 0;
		
		public UserData(User owner) {
			super(owner, getThis());
			queuedRequests = new PriorityQueue<QueueElement<Request>>();
		}
		
	}


	@Override
	public void write(User user, byte[] data) {
		anonNode.write(user, data);
	}


	@Override
	public Reply addLayer4Header(Reply reply) {
		// TODO: get port-header from layer 4
		UserData userData = reply.getOwner().getAttachment(this, UserData.class);
		assert userData != null;
		if (userData.nextSequenceNumberSend == Integer.MAX_VALUE)
			displayErrorMessage();
		userData.nextSequenceNumberSend++;
		byte[] sequenceNumberHeader = Util.intToByteArray(userData.nextSequenceNumberSend);
		reply.setByteMessage(Util.concatArrays(sequenceNumberHeader, reply.getByteMessage()));
		//System.out.println("" +this +": sending " +userData.nextSequenceNumberSend); // TODO: remove
		//System.out.println("" +this +": sending (reply (mix)): " +Util.toHex(reply.getByteMessage())); // TODO: remove
		return reply;
	}


	@Override
	public int getSizeOfLayer4Header() {
		return 4; // seq.-header is 4 bytes long (an int value)
	}


	@Override
	public int getMaxSizeOfNextWrite() {
		return layer3controller.getMaxSizeOfNextWrite() - getSizeOfLayer4Header();
	}


	@Override
	public int getMaxSizeOfNextRead() {
		return layer3controller.getMaxSizeOfNextRead() - getSizeOfLayer4Header();
	}
	
}

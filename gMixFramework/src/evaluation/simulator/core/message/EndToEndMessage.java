/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2013  Karl-Peter Fuchs
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
package evaluation.simulator.core.message;

import evaluation.simulator.Simulator;
import evaluation.traceParser.engine.dataStructure.Transaction;


public class EndToEndMessage {

	public int receiverId;
	public Transaction payload;
	public int multiplexChannelId;
	public boolean closeConnection;
	public TransportMessage transportMessage;
	
	
	public static int multiplexChannelIdCounter = 0;

	
	public EndToEndMessage(int receiverId, Transaction payload, boolean closeConnection) {
		this(receiverId, -1, payload, closeConnection);
	}
	
	
	public EndToEndMessage(int receiverId, int multiplexChannelId, Transaction payload, boolean closeConnection) {
		this.receiverId = receiverId;
		this.multiplexChannelId = multiplexChannelId;
		this.payload = payload;
		this.closeConnection = closeConnection;
	}
	
	
	public EndToEndMessage createReplyForThisMessage(Transaction payload, int replySize) {
		EndToEndMessage reply = new EndToEndMessage(receiverId, multiplexChannelId, payload, closeConnection);
		assert transportMessage != null;
		MixMessage associatedMixMessage = transportMessage.getAssociatedMixMessage();
		reply.transportMessage = new TransportMessage(
				false, 
				Simulator.getSimulator().getDistantProxy(), 
				transportMessage.getOwner(), 
				transportMessage.getCreationTime(), // used for RTT calculation
				transportMessage.getOwner(), 
				replySize, 
				reply
				);
		reply.transportMessage.setAssociatedMixMessage(associatedMixMessage);
		return reply;
	}


	public Transaction getPayload() {
		return payload;
	}
	
}

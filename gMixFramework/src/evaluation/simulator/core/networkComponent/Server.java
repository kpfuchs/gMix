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
package evaluation.simulator.core.networkComponent;

import evaluation.simulator.core.message.EndToEndMessage;
import evaluation.traceParser.engine.dataStructure.ExtendedTransaction;
import evaluation.traceParser.engine.dataStructure.Transaction;


public class Server extends AbstractServer {

	
	protected Server(DistantProxy distantProxy) {
		super(distantProxy);
	}

	
	@Override
	public void incomingMessage(EndToEndMessage message) {
		//System.out.println("Server: received message " +message.payload.getTransactionId());
		if (SIMULATE_REPLY_CHANNEL) {
			if (message.getPayload().containsReplies()) { // create and schedule replies:
				if (UNLIMITED_BANDWIDTH) {
					ExtendedTransaction at = (ExtendedTransaction)message.getPayload();
					int[] replySizes = at.getDistinctReplySizes();
					for (int i=0; i<replySizes.length; i++) {
						EndToEndMessage reply = message.createReplyForThisMessage(at, replySizes[i]);
						long replyDelay = at.endsOfReplies[i] - at.startOfRequest; // delay as observed in original trace
						assert reply.transportMessage.hasNextFragment();
						//System.out.println("server: scheduling reply for t=" +(Simulator.getNow() + sumOfDelays) +" (transactionId: " +at.getTransactionId() +")"); 
						sendReplyIn(replyDelay, reply);
					}
				} else {
					Transaction at = message.getPayload();
					int[] replyDelays = at.getDistinctReplyDelays();
					int[] replySizes = at.getDistinctReplySizes();
					int sumOfDelays = 0;
					for (int i=0; i<replySizes.length; i++) {
						sumOfDelays += replyDelays[i];
						EndToEndMessage reply = message.createReplyForThisMessage(at, replySizes[i]);
						assert reply.transportMessage.hasNextFragment();
						//System.out.println("server: scheduling reply for t=" +(Simulator.getNow() + sumOfDelays) +" (transactionId: " +at.getTransactionId() +")"); 
						sendReplyIn(sumOfDelays, reply);
					} 
				}
			} else { // no reply
				return;
			}
		}
	}


}

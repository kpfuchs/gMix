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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer3outputStrategy.adaptiveMix_v0_001;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Vector;

import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer2RecodingSchemeMix;
import staticContent.framework.interfaces.Layer3OutputStrategyMix;
import staticContent.framework.message.MixMessage;
import staticContent.framework.message.Reply;
import staticContent.framework.message.Request;
import staticContent.framework.routing.RoutingMode;
import staticContent.framework.userDatabase.User;
import staticContent.framework.userDatabase.UserAttachment;
import staticContent.framework.util.Util;


public class MixPlugIn extends Implementation implements Layer3OutputStrategyMix {

	private Vector<UserData> connectedUsers;
	private int CHECK_INTERVAL;
	private Object synchronizer = new Object();
	private Layer2RecodingSchemeMix recodingScheme;
	
	
	@Override
	public void constructor() {
		if (anonNode.ROUTING_MODE != RoutingMode.GLOBAL_ROUTING)
			throw new RuntimeException("this plugin requires cascade (GLOBAL_ROUTING) routing mode"); 
		if (!anonNode.IS_DUPLEX)
			throw new RuntimeException("this plugin requires duplex mode");
	}

	
	@Override
	public void initialize() {
		if (anonNode.IS_LAST_MIX) {
			this.connectedUsers = new Vector<UserData>(anonNode.EXPECTED_NUMBER_OF_USERS*2);
			this.CHECK_INTERVAL = settings.getPropertyAsInt("CHECK_INTERVAL");
			this.recodingScheme = anonNode.getRecodingLayerControllerMix();
		}
	}

	
	@Override
	public void begin() {
		if (anonNode.IS_LAST_MIX) {
			new DecisionThread().start();
		}
	}

	
	@Override
	public void addRequest(Request request) {
		if (anonNode.IS_LAST_MIX) {
			if (request.getOwner().getAttachment(this, UserData.class) == null) {
				synchronized (synchronizer) {
					UserData userData = new UserData(request.getOwner(), this);
					connectedUsers.add(userData);
				}
			}
		}
		anonNode.putOutRequest(request); // TODO: do not just forward everything directly...
	}


	@Override
	public void write(User user, byte[] data) {
		synchronized (synchronizer) {
			// just store all replies and send the maximum number of reply-bytes available (for a single user) every x ms to all users...
			// note: we will create mix messages etc in the "DecisionThread"
			UserData userData = user.getAttachment(this, UserData.class);
			userData.addReplyBlock(data);
		}
	}
	
	
	@Override
	public void addReply(Reply reply) {
		if (reply.wasCreatedOnThisMix()) { // last mix/we have created this message in the "DecisionThread" earlier (now its encrypted) -> just forward it to the next mix
			anonNode.putOutReply(reply);
		} else { // not the last mix
			anonNode.putOutReply(reply); // TODO: wait until a message has arrived for every client. then, forward all messages in random order (note: requires knowledge of the number of messages that were sent by the last mix unless we want to work with timeouts... use a header? -> see comment below)
		}
	}

	
	@Override
	public int getMaxSizeOfNextWrite() {
		return super.recodingLayerMix.getMaxSizeOfNextReply();
	}


	@Override
	public int getMaxSizeOfNextRead() {
		return super.recodingLayerMix.getMaxSizeOfNextRequest();
	}
	
	
	protected class DecisionThread extends Thread {
		
		@Override
		public void run() {
			while (true) {
				sleep(CHECK_INTERVAL);
				synchronized (synchronizer) {
					int[] availableDataPerUser = new int[connectedUsers.size()];
					int maxData = 0;
					int minData = Integer.MAX_VALUE;
					for (int i=0; i<availableDataPerUser.length; i++) {
						availableDataPerUser[i] = connectedUsers.get(i).getTotalSizeOfAllReplyBlocks();
						if (availableDataPerUser[i] > maxData)
							maxData = availableDataPerUser[i];
						if (minData > availableDataPerUser[i])
							minData = availableDataPerUser[i];
					}
					// strategy: always send maximum data (generate dummies for all others):
					// TODO implement "real" strategy
					if (maxData > 0) { // TODO: not tested... (calculations might be wrong)
						int numberOfRepliesPerUser = (int)Math.ceil((double)maxData/(double)(getMaxSizeOfNextWrite()-transportLayerMix.getSizeOfLayer4Header()));
						Vector<Reply> outputBatch = new Vector<Reply>(numberOfRepliesPerUser * connectedUsers.size());
						for (int i=0; i<availableDataPerUser.length; i++) { // TODO: ggf. hier rekursiven layer3-header hinzufügen, der anderen mixen mitteilt, wie viele nachrichten zu diesem "schub" gehören (siehe kommentar bei addReply()-methode)
							Reply[] replies = connectedUsers.get(i).generateReplies(connectedUsers.get(i).getOwner(), availableDataPerUser[i]);
							for (int j=0; j<replies.length; j++)
								outputBatch.add(replies[j]); 
							int numberOfDummies = numberOfRepliesPerUser - replies.length;
							for (int j=0; j<numberOfDummies; j++)
								outputBatch.add(recodingScheme.generateDummyReply(connectedUsers.get(i).getOwner()));
						} 
						Collections.shuffle(outputBatch); // TODO: probably keep order of a single users' messages...
						System.out.println("putting out " +outputBatch.size() +" messages"); 
						for (Reply reply:outputBatch) { // TODO: ineffizient, weil das senden durchaus lange dauern kann (wenn der rückkanal am limit arbeitet = "trägheit des reply kanals") -> evtl. vorher eine liste der user-objekte zufällig anordnen, die einheitliche nachrichtengröße festlegen und dann (in reihenfolge der liste) immer eine mix-nachricht (bzw. die mix-nachrchten) für einen nutzer generieren und anschliessend direkt versenden (und das für jeden listeneintrag/nutzer nacheinander durchführen). vorteil: während nachrichten für andere nutzer verschickt werden/wurden, können über layer 4 neue daten eintreffen, die noch mit in die mix-nachrichten der nutzer aufgenommen wurden können (java "synchronized" beachten!), deren nachricht noch nicht abgeschickt wurde. ABER: überlegen, ob das ein sicherheitsproblem sein kann (erste einschätzung: bei sync. kanälen, bei denen der letzte mix alle nachrichten des selben (pseudonymen) nutzers verketten kann wohl eher nicht...); evtl auch sinnvoll: tcp-pufferauslastung oder warteschlangenlänge auf layer 1 berücksichtigen (= entschiedung erst treffen, wenn etwas platz frei ist (warteschlange/puffer sollte aber natürlich auch nie ganz leer laufen...))
							if (reply != null)
								anonNode.forwardToLayer2(reply);
						}
					}
				} 
			} 
		}
		
		
		private void sleep(int duration) {
			try {
				Thread.sleep(duration);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
	protected class UserData extends UserAttachment {

		private Vector<byte[]> replyQueue; // TODO: ich hab das gleich mal als vector gelassen, weil du vermutlich auch timestamps etc mit speichern willst... sonst wäre hier sicher ein ByteBuffer oder ByteArrayOutputStream besser geeignet. wenn du die beiden datenstrukturen noch nicht kennst, solltest du sie dir aber trotzdem mal anschauen - könnte (an anderen stellen) noch hilfreich werden
		private int totalSize = 0;
		
		
		public UserData(User owner, Object callingInstance) {
			super(owner, callingInstance);
			this.replyQueue = new Vector<byte[]>(100);
		}
		
		
		public int getNumberOfReplyBlocks() {
			return replyQueue.size();
		}
		
		
		// in byte
		public int getTotalSizeOfAllReplyBlocks() {
			return totalSize;
		}
		
		
		public byte[] getNextReplyBlock() {
			if (replyQueue.size() == 0) {
				return null;
			} else {
				byte[] r = replyQueue.remove(0);
				totalSize -= r.length;
				return r;
			}
		}
		
		
		public void addReplyBlock(byte[] replyBlock) {
			replyQueue.add(replyBlock);
			totalSize += replyBlock.length;
		}
		
		
		public byte[] getReplyBlock(int index) {
			return replyQueue.get(index);
		}
		
		
		// TODO: not tested...
		public byte[] getBytes(int numberOfBytes) {
			if (numberOfBytes > totalSize)
				throw new RuntimeException("can't return more than getTotalSizeOfReplies() bytes"); 
			ByteArrayOutputStream result = new ByteArrayOutputStream(numberOfBytes);
			try {
				int remaining = numberOfBytes;
				while (remaining > 0) {
					int lengthOfNextBlock = replyQueue.firstElement().length;
					if (lengthOfNextBlock <= remaining) {
						result.write(replyQueue.remove(0));
						totalSize -= lengthOfNextBlock;
						remaining -= lengthOfNextBlock;
					} else { // lengthOfNextBlock > remaining
						byte[][] splitted = Util.split(remaining, replyQueue.remove(0));
						result.write(splitted[0]);
						replyQueue.add(0, splitted[1]);
						totalSize -= splitted[0].length;
						remaining = 0;
					}
				} 
			} catch (IOException e) {
				throw new RuntimeException("seriously: how can this happen with a local array?"); 
			}
			return result.toByteArray();
		}
		
		// TODO: not tested...
		public Reply[] generateReplies(User user, int numberOfPayloadBytes) {
			if (numberOfPayloadBytes > totalSize)
				throw new RuntimeException("can't return more than getTotalSizeOfReplies() bytes"); 
			int maxPayloadSize = recodingLayerMix.getMaxSizeOfNextReply() - transportLayerMix.getSizeOfLayer4Header();
			int numberOfMessages = (int)Math.ceil((double)numberOfPayloadBytes/(double)maxPayloadSize);
			int sizeOfLastMessage = numberOfPayloadBytes % maxPayloadSize;
			Reply[] result = new Reply[numberOfMessages];
			for (int i=0; i<numberOfMessages; i++) {
				int payloadSize = (i != numberOfMessages-1) ? maxPayloadSize : sizeOfLastMessage;
				if (payloadSize == 0)
					continue;
				byte[] payload = getBytes(payloadSize);
				result[i] = MixMessage.getInstanceReply(payload, user); 
				result[i].isFirstReplyHop = true;
				transportLayerMix.addLayer4Header(result[i]);
			}
			return result;
		}
		
	} 

}

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
package plugIns.layer3outputStrategy.proportionalMethod_v0_001;

import java.security.SecureRandom;
import java.util.Vector;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer3OutputStrategyMix;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.userDatabase.User;
import framework.core.userDatabase.UserAttachment;


// see IEEE ICC 2012 "Source Anonymity in Fair Scheduling: A Case for the Proportional Method" (Abhishek Mishra, Parv Venkitasubramaniam)
public class MixPlugIn extends Implementation implements Layer3OutputStrategyMix {

	private static SecureRandom secureRandom = new SecureRandom();
	private SimplexPmPool requestPool;
	private SimplexPmPool replyPool;
	private int POOL_SIZE;
	private int DEFAULT_QUEUE_SIZE;
	private Object attachmentKey = new Object();
	
	
	@Override
	public void constructor() {
		this.POOL_SIZE = settings.getPropertyAsInt("PROPORTIONAL_METHOD_POOL_SIZE");
		this.DEFAULT_QUEUE_SIZE = settings.getPropertyAsInt("PROPORTIONAL_METHOD_DEFAULT_QUEUE_SIZE");
		this.requestPool = new SimplexPmPool(true);
		this.replyPool = new SimplexPmPool(false);
	}
	

	
	@Override
	public void initialize() {
		// no need to do anything
	}

	
	@Override
	public void begin() {
		// no need to do anything
	}

	
	private class UserData extends UserAttachment {
		
		protected Vector<Request> requestQueue;
		protected Vector<Reply> replyQueue;
		
		
		private UserData(User owner) {
			super(owner, attachmentKey);
			this.requestQueue = new Vector<Request>(DEFAULT_QUEUE_SIZE);
			if (anonNode.IS_DUPLEX)
				this.replyQueue = new Vector<Reply>(DEFAULT_QUEUE_SIZE);
		}
		
	}
	
	
	@Override
	public void addRequest(Request request) {
		UserData userData = request.getOwner().getAttachment(attachmentKey, UserData.class);
		if (userData == null) // first message of this user
			userData = new UserData(request.getOwner());
		userData.requestQueue.add(request);
		requestPool.addMessage(userData);
	}

	
	@Override
	public void addReply(Reply reply) {
		UserData userData = reply.getOwner().getAttachment(attachmentKey, UserData.class);
		assert userData != null;
		userData.replyQueue.add(reply);
		replyPool.addMessage(userData);
	}

	
	public class SimplexPmPool {
		
		private boolean isRequestPool;
		private UserData[] collectedMessages;
		private int nextFreeSlot = 0;
		
		
		public SimplexPmPool(boolean isRequestPool) {
			this.collectedMessages = new UserData[POOL_SIZE];
			this.isRequestPool = isRequestPool;
		}
		
		
		public synchronized void addMessage(UserData userData) {
			if (nextFreeSlot < POOL_SIZE) {
				collectedMessages[nextFreeSlot++] = userData;
			} else {
				int chosen = secureRandom.nextInt(POOL_SIZE+1);
				if (chosen == POOL_SIZE) {
					putOutMessage(userData);
				} else {
					putOutMessage(collectedMessages[chosen]);
					collectedMessages[chosen] = userData;
				}
			}
		}
		
		
		private void putOutMessage(UserData userData) {
			if (isRequestPool)
				anonNode.putOutRequest(userData.requestQueue.remove(0));
			else
				anonNode.putOutReply(userData.replyQueue.remove(0));
		}
	}

	
	@Override
	public int getMaxSizeOfNextReply() {
		return super.recodingLayerMix.getMaxSizeOfNextReply();
	}


	@Override
	public int getMaxSizeOfNextRequest() {
		return super.recodingLayerMix.getMaxSizeOfNextRequest();
	}
	
}

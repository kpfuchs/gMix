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
package plugIns.layer3outputStrategy.basicBatch_v0_001;

import java.util.Arrays;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer3OutputStrategyMix;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;


public class MixPlugIn extends Implementation implements Layer3OutputStrategyMix {

	private SimplexBatch requestBatch;
	private SimplexBatch replyBatch;
	private int BATCH_SIZE;
	
	
	@Override
	public void constructor() {
		this.BATCH_SIZE = settings.getPropertyAsInt("BASIC_BATCH_BATCH_SIZE");
		this.requestBatch = new SimplexBatch(true);
		this.replyBatch = new SimplexBatch(false);
	}

	
	@Override
	public void initialize() {
		// no need to do anything
	}

	
	@Override
	public void begin() {
		// no need to do anything
	}

	
	@Override
	public void addRequest(Request request) {
		requestBatch.addMessage(request);
	}


	@Override
	public void addReply(Reply reply) {
		replyBatch.addMessage((MixMessage)reply);
	}
	
	
	public class SimplexBatch {
		
		private boolean isRequestBatch;
		private MixMessage[] collectedMessages;
		private int nextFreeSlot = -1;
		
		
		public SimplexBatch(boolean isRequestBatch) {
			this.isRequestBatch = isRequestBatch;
			this.collectedMessages = isRequestBatch ? new Request[BATCH_SIZE]: new Reply[BATCH_SIZE];	
		}
		
		
		public synchronized void addMessage(MixMessage mixMessage) {
			nextFreeSlot++;
			if (nextFreeSlot == BATCH_SIZE) {
				Arrays.sort(collectedMessages);
				if (isRequestBatch)
					anonNode.putOutRequests((Request[])collectedMessages);
				else
					anonNode.putOutReplies((Reply[])collectedMessages);
				this.collectedMessages = isRequestBatch ? new Request[BATCH_SIZE]: new Reply[BATCH_SIZE];
				nextFreeSlot = 0;
			}
			collectedMessages[nextFreeSlot] = mixMessage;
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

/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2011  Karl-Peter Fuchs
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

package outputStrategy;

import java.util.Arrays;

import message.MixMessage;
import message.Reply;
import message.Request;
import framework.Implementation;


public class BasicBatch_v1 extends Implementation implements OutputStrategy {

	private SimplexBatch requestBatch;
	private SimplexBatch replyBatch;

	
	@Override
	public void constructor() {
		int batchSize = settings.getPropertyAsInt("BasicBatch_v1_BATCH_SIZE");
		this.requestBatch = new SimplexBatch(batchSize, true);
		this.replyBatch = new SimplexBatch(batchSize, false);
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
		requestBatch.addMessage((MixMessage)request);
	}


	@Override
	public void addReply(Reply reply) {
		replyBatch.addMessage((MixMessage)reply);
	}
	
	
	public class SimplexBatch {
		
		private boolean isRequestBatch;
		private int batchSize;
		private MixMessage[] collectedMessages;
		private int nextFreeSlot = 0;
		
		
		public SimplexBatch(int batchSize, boolean isRequestBatch) {
			
			this.batchSize = batchSize;	
			this.isRequestBatch = isRequestBatch;
			this.collectedMessages = new MixMessage[batchSize];
				
		}
		
		
		public synchronized void addMessage(MixMessage mixMessage) {
			
			collectedMessages[nextFreeSlot++] = mixMessage;
			
			if (nextFreeSlot == batchSize) {
				
				Arrays.sort(collectedMessages);
				
				for (MixMessage m: collectedMessages)
					if (isRequestBatch)
						controller.getInputOutputHandler().addRequest((Request)m);
					else
						controller.getInputOutputHandler().addReply((Reply)m);
				
				nextFreeSlot = 0;
				
			}
			
		}
		
	}

}
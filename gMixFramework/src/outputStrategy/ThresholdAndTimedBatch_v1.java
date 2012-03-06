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

import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import message.MixMessage;
import message.Reply;
import message.Request;
import framework.Implementation;


//Dingledine 2002: Timed Mix
//"fires (flushes all messages) every t seconds but only
//when at least n messages have accumulated in the mix.
public class ThresholdAndTimedBatch_v1 extends Implementation implements OutputStrategy {

	private SimplexThresholdAndTimedBatch requestBatch;
	private SimplexThresholdAndTimedBatch replyBatch;

	
	@Override
	public void constructor() {
		int sendingRate = settings.getPropertyAsInt("ThresholdAndTimedBatch_v1_SENDING_RATE");
		int batchSize = settings.getPropertyAsInt("ThresholdAndTimedBatch_v1_BATCH_SIZE");
		this.requestBatch = new SimplexThresholdAndTimedBatch(true, sendingRate, batchSize);
		this.replyBatch = new SimplexThresholdAndTimedBatch(false, sendingRate, batchSize);
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
		requestBatch.addMessage((MixMessage) request);
	}

	
	@Override
	public void addReply(Reply reply) {
		replyBatch.addMessage((MixMessage) reply);
	}

	
	public class SimplexThresholdAndTimedBatch {

		private boolean isRequestBatch;
		private Vector<MixMessage> collectedMessages;
		private int batchSize;
		private Timer timer = new Timer();
		
		
		public SimplexThresholdAndTimedBatch(boolean isRequestBatch, int sendingRate, int batchSize) {
			
			this.collectedMessages = new Vector<MixMessage>(batchSize);
			this.isRequestBatch = isRequestBatch;
			this.batchSize = batchSize;
			this.timer.scheduleAtFixedRate(new TimeoutTask(this), sendingRate, sendingRate);
			
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			
			synchronized (this) {
				
				collectedMessages.add(mixMessage);

			}

		}

		
		public void putOutMessages() {
			
			synchronized (this) {
				
				if (collectedMessages.size() <= batchSize)
					return;
				
				if (isRequestBatch)
					System.out.println("putting out " +collectedMessages.size() +" messages"); // TODO: remove
				
				Collections.sort(collectedMessages);
				
				if (isRequestBatch)
					for (MixMessage m:collectedMessages)
						controller.getInputOutputHandler().addRequest((Request)m);
				else
					for (MixMessage m:collectedMessages)
						controller.getInputOutputHandler().addReply((Reply)m);
				
				this.collectedMessages = new Vector<MixMessage>(batchSize);
	
			}
				
		}
	
		
		private final class TimeoutTask extends TimerTask {

			private SimplexThresholdAndTimedBatch linkedBatch;
			
			protected TimeoutTask(SimplexThresholdAndTimedBatch linkedBatch) {
				this.linkedBatch = linkedBatch;
			}
			
			@Override 
			public void run() {
				linkedBatch.putOutMessages();
			}
			
		}
			
	}

}

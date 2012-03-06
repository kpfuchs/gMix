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
//collects messages until "batchSize" messages are reached or a timeout occurs
//the timeout timer is started when the first message is added to the batch
//and gets canceled as the batch is put out (due to reaching the batch size)
//see also: "ThresholdOrTimedBatch.java"
public class BatchWithTimeout_v1 extends Implementation implements OutputStrategy {

	private SimplexBatchWithTimeout requestBatch;
	private SimplexBatchWithTimeout replyBatch;

	
	@Override
	public void constructor() {
		int timeout = settings.getPropertyAsInt("BatchWithTimeout_v1_TIMEOUT");
		int batchSize = settings.getPropertyAsInt("BatchWithTimeout_v1_BATCH_SIZE");
		this.requestBatch = new SimplexBatchWithTimeout(true, timeout, batchSize);
		this.replyBatch = new SimplexBatchWithTimeout(false, timeout, batchSize);
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

	
	public class SimplexBatchWithTimeout {

		private boolean isRequestBatch;
		private Vector<MixMessage> collectedMessages;
		private int timeout;
		private int batchSize;
		private volatile long lastOutput;
		private Timer timer;
		
		
		public SimplexBatchWithTimeout(boolean isRequestBatch, int timeout, int batchSize) {
			
			this.collectedMessages = new Vector<MixMessage>(batchSize);
			this.isRequestBatch = isRequestBatch;
			this.batchSize = batchSize;
			this.timeout = timeout;
			
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			
			synchronized (this) {
				
				collectedMessages.add(mixMessage);
				
				if (collectedMessages.size() == 1) {
					
					timer = new Timer();
					timer.schedule(new TimeoutTask(this), timeout);
					lastOutput = mix.getNetworkClock().getTime();
					
				} else if (collectedMessages.size() == batchSize)
					putOutMessages();
				
			}

		}

		
		public void putOutMessages() {
			
			synchronized (this) {
				
				Collections.sort(collectedMessages);
				
				if (isRequestBatch)
					for (MixMessage m:collectedMessages)
						controller.getInputOutputHandler().addRequest((Request)m);
				else
					for (MixMessage m:collectedMessages)
						controller.getInputOutputHandler().addReply((Reply)m);
				
				this.collectedMessages = new Vector<MixMessage>(batchSize);
				
				if (timer != null)
					timer.cancel();
				
				this.lastOutput = mix.getNetworkClock().getTime();
				
			}
				
		}
	
		
		private final class TimeoutTask extends TimerTask {

			private SimplexBatchWithTimeout linkedBatch;
			
			protected TimeoutTask(SimplexBatchWithTimeout linkedBatch) {
				this.linkedBatch = linkedBatch;
			}
			
			@Override 
			public void run() {
				if (mix.getNetworkClock().getTime() - lastOutput >= timeout) {
					linkedBatch.putOutMessages();
				}
			}
			
		}
			
	}

}

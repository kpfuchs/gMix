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
package plugIns.layer3outputStrategy.thresholdAndTimedBatch_v0_001;

import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer3OutputStrategyMix;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;


//Dingledine 2002: Timed Mix
//"fires (flushes all messages) every t seconds but only
//when at least n messages have accumulated in the mix."
public class MixPlugIn extends Implementation implements Layer3OutputStrategyMix {

	private SimplexThresholdAndTimedBatch requestBatch;
	private SimplexThresholdAndTimedBatch replyBatch;
	private int BATCH_SIZE;
	private int SENDING_RATE;

	
	@Override
	public void constructor() {
		this.BATCH_SIZE = settings.getPropertyAsInt("THRESHOLD_AND_TIMED_BATCH_BATCH_SIZE");
		this.SENDING_RATE = settings.getPropertyAsInt("THRESHOLD_AND_TIMED_BATCH_SENDING_RATE");
		this.requestBatch = new SimplexThresholdAndTimedBatch(true);
		this.replyBatch = new SimplexThresholdAndTimedBatch(false);
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
		replyBatch.addMessage(reply);
	}

	
	public class SimplexThresholdAndTimedBatch {

		private boolean isRequestBatch;
		private Vector<MixMessage> collectedMessages;
		private Timer timer = new Timer();
		
		
		public SimplexThresholdAndTimedBatch(boolean isRequestBatch) {
			this.collectedMessages = new Vector<MixMessage>(BATCH_SIZE);
			this.isRequestBatch = isRequestBatch;
			this.timer.scheduleAtFixedRate(new TimeoutTask(this), SENDING_RATE, SENDING_RATE);
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			synchronized (timer) {
				collectedMessages.add(mixMessage);
			}
		}

		
		public void putOutMessages() {
			synchronized (timer) {
				if (collectedMessages.size() <= BATCH_SIZE)
					return;
				if (isRequestBatch)
					System.out.println("putting out " +collectedMessages.size() +" messages"); // TODO: remove
				Collections.sort(collectedMessages);
				if (isRequestBatch)
					anonNode.putOutRequests(collectedMessages.toArray(new Request[0]));
				else
					anonNode.putOutReplies(collectedMessages.toArray(new Reply[0]));
				this.collectedMessages = new Vector<MixMessage>(BATCH_SIZE);
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

	
	@Override
	public int getMaxSizeOfNextReply() {
		return super.recodingLayerMix.getMaxSizeOfNextReply();
	}


	@Override
	public int getMaxSizeOfNextRequest() {
		return super.recodingLayerMix.getMaxSizeOfNextRequest();
	}
	
}

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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer3outputStrategy.thresholdOrTimedBatch_v0_001;

import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer3OutputStrategyMix;
import staticContent.framework.message.MixMessage;
import staticContent.framework.message.Reply;
import staticContent.framework.message.Request;
import staticContent.framework.userDatabase.User;


//Dingledine 2002: Timed Mix
//"The mix fires (flushes all messages) every t seconds or when n messages 
//accumulate in the mix."
//see also: "BatchWithTimeout.java"
public class MixPlugIn extends Implementation implements Layer3OutputStrategyMix {

	private SimplexThresholdOrTimedBatch requestBatch;
	private SimplexThresholdOrTimedBatch replyBatch;
	private int BATCH_SIZE;
	private int SENDING_RATE;

	
	@Override
	public void constructor() {
		this.BATCH_SIZE = settings.getPropertyAsInt("THRESHOLD_OR_TIMED_BATCH_BATCH_SIZE");
		this.SENDING_RATE = settings.getPropertyAsInt("THRESHOLD_OR_TIMED_BATCH_SENDING_RATE");
		this.requestBatch = new SimplexThresholdOrTimedBatch(true);
		this.replyBatch = new SimplexThresholdOrTimedBatch(false);
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

	
	public class SimplexThresholdOrTimedBatch {

		private boolean isRequestBatch;
		private Vector<MixMessage> collectedMessages;
		private Timer timer = new Timer();
		
		
		public SimplexThresholdOrTimedBatch(boolean isRequestBatch) {
			this.collectedMessages = new Vector<MixMessage>(BATCH_SIZE);
			this.isRequestBatch = isRequestBatch;
			this.timer.scheduleAtFixedRate(new TimeoutTask(this), SENDING_RATE, SENDING_RATE);
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			synchronized (this) {
				collectedMessages.add(mixMessage);
				if (collectedMessages.size() == BATCH_SIZE)
					putOutMessages();
				
			}
		}

		
		public void putOutMessages() {
			synchronized (this) {
				if (collectedMessages.size() > 0) {
					Collections.sort(collectedMessages);
					if (isRequestBatch) {
						for (MixMessage request: collectedMessages)
							anonNode.putOutRequest((Request) request);
					} else {
						for (MixMessage reply: collectedMessages)
							anonNode.putOutReply((Reply) reply);
					}
					this.collectedMessages = new Vector<MixMessage>(BATCH_SIZE);
				}
			}	
		}
	
		
		private final class TimeoutTask extends TimerTask {

			private SimplexThresholdOrTimedBatch linkedBatch;
			
			protected TimeoutTask(SimplexThresholdOrTimedBatch linkedBatch) {
				this.linkedBatch = linkedBatch;
			}
			
			@Override 
			public void run() {
				linkedBatch.putOutMessages();
			}
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


	@Override
	public void write(User user, byte[] data) {
		Reply reply = MixMessage.getInstanceReply(data, user); 
		reply.isFirstReplyHop = true;
		transportLayerMix.addLayer4Header(reply);
		anonNode.forwardToLayer2(reply);
	}
	
}

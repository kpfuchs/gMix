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

import framework.Implementation;

import message.MixMessage;
import message.Reply;
import message.Request;


public class SynchronousBatch_v1 extends Implementation implements OutputStrategy {

	private SimplexSynchronousBatch_v1 requestBatch;
	private SimplexSynchronousBatch_v1 replyBatch;

	
	@Override
	public void constructor() {
		int timeout = settings.getPropertyAsInt("SynchronousBatch_v1_TIMEOUT");
		this.requestBatch = new SimplexSynchronousBatch_v1(true, timeout);
		this.replyBatch = new SimplexSynchronousBatch_v1(false, timeout);
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

	
	public class SimplexSynchronousBatch_v1 {

		private boolean isRequestBatch;
		private Vector<MixMessage> collectedMessages;
		private int timeout;
		private volatile long lastOutput;
		private Timer timer;
		
		
		public SimplexSynchronousBatch_v1(boolean isRequestBatch, int timeout) {
			
			this.collectedMessages = new Vector<MixMessage>(userDatabase.getNumberOfUsers());
			this.isRequestBatch = isRequestBatch;
			this.timeout = timeout;
			
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			
			synchronized (this) {
				
				collectedMessages.add(mixMessage);
				
				if (collectedMessages.size() == 1) {
					
					timer = new Timer();
					timer.schedule(new TimeoutTask(this), timeout);
					lastOutput = mix.getNetworkClock().getTime();
					
				} else if (collectedMessages.size() >= userDatabase.getNumberOfUsers())
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
				
				this.collectedMessages = new Vector<MixMessage>(userDatabase.getNumberOfUsers());
				
				if (timer != null)
					timer.cancel();
				
				this.lastOutput = mix.getNetworkClock().getTime();
				
			}
				
		}
	
		
		private final class TimeoutTask extends TimerTask {

			private SimplexSynchronousBatch_v1 linkedBatch;
			
			protected TimeoutTask(SimplexSynchronousBatch_v1 linkedBatch) {
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

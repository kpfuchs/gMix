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
package plugIns.layer3outputStrategy.constantRate_v0_001;

import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer3OutputStrategyMix;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;


public class MixPlugIn extends Implementation implements Layer3OutputStrategyMix {

	private SimplexSynchronousBatch requestBatch;
	private SimplexSynchronousBatch replyBatch;
	private long TIMEOUT;
	
	
	@Override
	public void constructor() {
		this.TIMEOUT = settings.getPropertyAsInt("CONSTANT_RATE_BATCH_TIMEOUT");
		this.requestBatch = new SimplexSynchronousBatch(true);
		this.replyBatch = new SimplexSynchronousBatch(false);
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

	
	public class SimplexSynchronousBatch implements Callable<SimplexSynchronousBatch> {

		private boolean isRequestBatch;
		private Vector<MixMessage> collectedMessages;
		private volatile long lastOutput;
		private ScheduledThreadPoolExecutor scheduler;
		private ScheduledFuture<?> currentTimer;
		
		
		public SimplexSynchronousBatch(boolean isRequestBatch) {
			this.collectedMessages = new Vector<MixMessage>(userDatabase.getNumberOfUsers());
			this.isRequestBatch = isRequestBatch;
			this.scheduler = new ScheduledThreadPoolExecutor(1);
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			synchronized (this) {
				collectedMessages.add(mixMessage);
				if (collectedMessages.size() == 1) {
					currentTimer = scheduler.schedule(this, TIMEOUT, TimeUnit.MILLISECONDS);
					lastOutput = clock.getTime();
				} else if (collectedMessages.size() >= userDatabase.getNumberOfUsers())
					putOutMessages();
			}
		}

		
		public void putOutMessages() {
			synchronized (this) {
				Collections.sort(collectedMessages);
				if (isRequestBatch)
					anonNode.putOutRequests(collectedMessages.toArray(new Request[0]));
				else
					anonNode.putOutReplies(collectedMessages.toArray(new Reply[0]));
				this.collectedMessages = new Vector<MixMessage>(userDatabase.getNumberOfUsers());
				if (currentTimer != null)
					currentTimer.cancel(false);
				this.lastOutput = clock.getTime();
			}	
		}


		@Override
		public SimplexSynchronousBatch call() throws Exception {
			if (clock.getTime() - lastOutput >= TIMEOUT) {
				putOutMessages();
			}
			return this;
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
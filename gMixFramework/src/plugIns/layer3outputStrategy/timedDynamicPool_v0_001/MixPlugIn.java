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
package plugIns.layer3outputStrategy.timedDynamicPool_v0_001;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer3OutputStrategyMix;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;


//Cottrell 1995 ("Mixmaster & Remailer Attacks")
//"The mix fires every t seconds, provided there are n + f(min) messages in the 
//mix; however, instead of sending n messages (as in a timed- and-threshold
//constant-pool mix), the mix sends the greater of 1 and m * frac messages,
//and retains the rest in the pool, where m + fmin is the number of messages
//in the mix (m >=n). If n = 1, this is the mix that has been used in the 
//Mixmaster remailer system for years."
//implemented as described in "Generalising Mixes" (Diaz)
public class MixPlugIn extends Implementation implements Layer3OutputStrategyMix {

	private SecureRandom secureRandom;
	private SimplexTimedDynamicPool requestPool;
	private SimplexTimedDynamicPool replyPool;
	private int DEFAULT_POOL_SIZE;
	private int SENDING_RATE;
	private double MIN_MESSAGES;
	private double FRACTION;

	
	@Override
	public void constructor() {
		this.DEFAULT_POOL_SIZE = settings.getPropertyAsInt("TIMED_DYNAMIC_POOL_DEFAULT_POOL_SIZE");
		this.SENDING_RATE = settings.getPropertyAsInt("TIMED_DYNAMIC_POOL_SENDING_RATE");
		this.MIN_MESSAGES = settings.getPropertyAsDouble("TIMED_DYNAMIC_POOL_MIN_MESSAGES");
		this.FRACTION = settings.getPropertyAsDouble("TIMED_DYNAMIC_POOL_FRACTION");
		this.requestPool = new SimplexTimedDynamicPool(true);
		this.replyPool = new SimplexTimedDynamicPool(false);
		try {
			this.secureRandom = SecureRandom.getInstance(settings.getProperty("TIMED_DYNAMIC_POOL_PRNG_ALGORITHM"));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException("could not init secureRandom"); 
		}
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
		requestPool.addMessage((MixMessage) request);
	}

	
	@Override
	public void addReply(Reply reply) {
		replyPool.addMessage((MixMessage) reply);
	}

	
	public class SimplexTimedDynamicPool {

		private boolean isRequestPool;
		private Vector<MixMessage> collectedMessages;
		private boolean isFirstMessage = true;
		private Timer timer = new Timer();
		
		
		public SimplexTimedDynamicPool(boolean isRequestPool) {
			this.collectedMessages = new Vector<MixMessage>(DEFAULT_POOL_SIZE);
			this.isRequestPool = isRequestPool;
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			synchronized (collectedMessages) {
				if (isFirstMessage) {
					isFirstMessage = false;
					timer.scheduleAtFixedRate(new TimeoutTask(this), SENDING_RATE, SENDING_RATE);
				}
				collectedMessages.add(mixMessage);
			}
		}

		
		public void putOutMessages() {
			synchronized (collectedMessages) {
				if (collectedMessages.size() > MIN_MESSAGES) {
					int numberOfMessagesToPutOut = (int) Math.floor(FRACTION * (collectedMessages.size() - MIN_MESSAGES));
					if (isRequestPool) {
						Request[] requests = new Request[numberOfMessagesToPutOut];
						for (int i=0; i<numberOfMessagesToPutOut; i++) {
							int chosen = secureRandom.nextInt(collectedMessages.size());
							requests[i] = (Request)collectedMessages.remove(chosen);
						} 
						anonNode.putOutRequests(requests);
					} else {
						Reply[] replies = new Reply[numberOfMessagesToPutOut];
						for (int i=0; i<numberOfMessagesToPutOut; i++) {
							int chosen = secureRandom.nextInt(collectedMessages.size());
							replies[i] = (Reply)collectedMessages.remove(chosen);
						} 
						anonNode.putOutReplies(replies);
					}
				}
			}
		}
	
		
		private final class TimeoutTask extends TimerTask {

			private SimplexTimedDynamicPool linkedPool;
			
			protected TimeoutTask(SimplexTimedDynamicPool linkedPool) {
				this.linkedPool = linkedPool;
			}
			
			@Override 
			public void run() {
				linkedPool.putOutMessages();
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

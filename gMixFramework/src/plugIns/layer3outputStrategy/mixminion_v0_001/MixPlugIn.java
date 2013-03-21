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
package plugIns.layer3outputStrategy.mixminion_v0_001;

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


/**
 * Java port of the output strategy used in Mixminion (29.08.2012 - see 
 * "https://github.com/mixminion/mixminion/blob/master/lib/mixminion/server/
 * ServerQueue.py")
 * Based on Cottrell 1995 ("Mixmaster & Remailer Attacks")
 * 
 * Note: A slightly different implementation (as described in 
 * http://mixminion.net/minion-spec.txt) can be found in the 
 * timedDynamicPool_v0_001 plug-in.
 * 
 */
public class MixPlugIn extends Implementation implements Layer3OutputStrategyMix {

	private SimplexTimedDynamicPool requestPool;
	private SimplexTimedDynamicPool replyPool;
	private int DEFAULT_POOL_SIZE;
	private int INTERVAL;
	private int MIN_POOL;
	private double SEND_RATE;
	private int MIN_SEND;
	

	@Override
	public void constructor() {
		this.DEFAULT_POOL_SIZE = settings.getPropertyAsInt("MIXMINION_DEFAULT_POOL_SIZE");
		this.INTERVAL = settings.getPropertyAsInt("MIXMINION_INTERVAL");
		this.MIN_POOL = settings.getPropertyAsInt("MIXMINION_MIN_POOL");
		this.SEND_RATE = settings.getPropertyAsDouble("MIXMINION_SEND_RATE");
		this.MIN_SEND = settings.getPropertyAsInt("MIXMINION_MIN_SEND");
		this.requestPool = new SimplexTimedDynamicPool(true);
		this.replyPool = new SimplexTimedDynamicPool(false);
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
		requestPool.addMessage(request);
	}

	
	@Override
	public void addReply(Reply reply) {
		replyPool.addMessage(reply);
	}


	public class SimplexTimedDynamicPool {

		private SecureRandom secureRandom;
		private boolean isRequestPool;
		private Vector<MixMessage> collectedMessages;
		private boolean isFirstMessage = true;
		private Timer timer = new Timer();
		
		
		public SimplexTimedDynamicPool(boolean isRequestPool) {
			this.collectedMessages = new Vector<MixMessage>(DEFAULT_POOL_SIZE);
			this.isRequestPool = isRequestPool;
			try {
				this.secureRandom = SecureRandom.getInstance(settings.getProperty("MIXMINION_PRNG_ALGORITHM"));
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				throw new RuntimeException("could not init secureRandom"); 
			}
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			synchronized (collectedMessages) {
				if (isFirstMessage) { // "1. Wait for MIX_INTERVAL seconds."
					isFirstMessage = false; 
					timer.scheduleAtFixedRate(
							new TimeoutTask(this), 
							INTERVAL,
							INTERVAL
							); // "3. Repeat indefinitely."
				}
				collectedMessages.add(mixMessage);
			}
		}

		
		public void putOutMessages() {
			synchronized (collectedMessages) {
				int sendable;
				int pool = collectedMessages.size();
				if (pool >= (MIN_POOL + MIN_SEND)) {
					sendable = pool - MIN_SEND;
					sendable = Math.min(sendable, Math.max(1, (int)Math.round((double)pool * SEND_RATE)));
				} else {
					sendable = 0;
				}
				if (sendable > 0) {
					if (isRequestPool) {
						for (int i=0; i<sendable; i++) {
							int chosen = secureRandom.nextInt(collectedMessages.size());
							anonNode.putOutRequest((Request)collectedMessages.remove(chosen));
						} 
					} else {
						for (int i=0; i<sendable; i++) {
							int chosen = secureRandom.nextInt(collectedMessages.size());
							anonNode.putOutReply((Reply)collectedMessages.remove(chosen));
						} 
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

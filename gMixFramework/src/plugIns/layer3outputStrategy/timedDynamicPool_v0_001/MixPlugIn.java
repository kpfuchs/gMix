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


/**
 * Output strategy used in Mixminion according to Mixminion spec (see 
 * below). Based on Cottrell 1995 ("Mixmaster & Remailer Attacks")
 * Note that the actual (29.08.2012) Mixminion implementation is slightly 
 * different (see "https://github.com/mixminion/mixminion/blob/master/lib/
 * mixminion/server/ServerQueue.py"). A Java port of the actual (29.08.2012) 
 * Mixminion implementation can be found in mixminion_v0_001 plug-in.
 * 
 * implemented as described in:
 * http://mixminion.net/minion-spec.txt ($Id: minion-spec.txt,v
 * 1.31 2007/04/21 16:57:47 arma Exp $ ): 
 * 
 * "A.1. Appendix: A suggested pooling rule
 * 
 * In order to allow room for future experimentation, we do not require
 * a single batching rule. Nonetheless, we describe a recommended rule
 * (as used in Mixmaster) which is somewhat resistant to flooding
 * attacks. Implementors are strongly encouraged to use this algorithm,
 * or another equally robust against active and passive attacks. (Be
 * sure to read \cite{batching-taxonomy}. [XXXX Ref])
 * 
 * PROCEDURE: Choose sets of messages to transmit ("Cottrell-style batching")
 * 
 * Inputs: 
 *    Q (a queue of messages)
 *    N (the number of messages in the queue).
 *    MIX_INTERVAL (algorithm parameter; time to wait between batches of
 *      messages. Should be around XXXXX. Must be >= 0.)
 *    POOL_SIZE (algorithm parameter; minimum size of pool. Should be at least
 *      XXXXXXXX. Must be >= 0.)
 *    MAX_REPLACEMENT_RATE (algorithm parameter; largest allowable rate 
 *      for messages to be removed from the pool. Should be between XXXX 
 *      and XXXX. Must have 0.0 < MAX_REPLACEMENT_RATE <= 1.0)
 * 
 * Outputs: (A set of messages sent to the network).
 * 1. Wait for MIX_INTERVAL seconds.
 * 2. If N > POOL_SIZE, then let 'max_send' =
 * FLOOR(N*MAX_REPLACEMENT_RATE). [If 'max_send' < 0, let max_send = 1.]
 * Choose Min(N-POOL_SIZE, max_send) messages from Q. Transmit the
 * selected messages.
 * 3. Repeat indefinitely."
 */
public class MixPlugIn extends Implementation implements Layer3OutputStrategyMix {

	private SecureRandom secureRandom;
	private SimplexTimedDynamicPool requestPool;
	private SimplexTimedDynamicPool replyPool;
	private int DEFAULT_POOL_SIZE;
	private int MIX_INTERVAL;
	private int POOL_SIZE;
	private double MAX_REPLACEMENT_RATE;

	
	@Override
	public void constructor() {
		this.DEFAULT_POOL_SIZE = settings.getPropertyAsInt("TIMED_DYNAMIC_POOL_DEFAULT_POOL_SIZE");
		this.MIX_INTERVAL = settings.getPropertyAsInt("TIMED_DYNAMIC_POOL_MIX_INTERVAL");
		this.POOL_SIZE = settings.getPropertyAsInt("TIMED_DYNAMIC_POOL_POOL_SIZE");
		this.MAX_REPLACEMENT_RATE = settings.getPropertyAsDouble("TIMED_DYNAMIC_POOL_MAX_REPLACEMENT_RATE");
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
				if (isFirstMessage) { // "1. Wait for MIX_INTERVAL seconds."
					isFirstMessage = false; 
					timer.scheduleAtFixedRate(
							new TimeoutTask(this), 
							MIX_INTERVAL, 
							MIX_INTERVAL
							); // "3. Repeat indefinitely."
				}
				collectedMessages.add(mixMessage);
			}
		}

		/*
		 * from http://mixminion.net/minion-spec.txt ($Id: minion-spec.txt,v
		 * 1.31 2007/04/21 16:57:47 arma Exp $ ): 
		 * 
		 * "Inputs: 
		 *    Q (a queue of messages)
		 *    N (the number of messages in the queue).
		 *    MIX_INTERVAL (algorithm parameter; time to wait between batches of
		 *      messages. Should be around XXXXX. Must be >= 0.)
		 *    POOL_SIZE (algorithm parameter; minimum size of pool. Should be at
		 *      least XXXXXXXX. Must be >= 0.)
		 *    MAX_REPLACEMENT_RATE (algorithm parameter; largest allowable rate 
		 *      for messages to be removed from the pool. Should be between XXXX 
		 *      and XXXX. Must have 0.0 < MAX_REPLACEMENT_RATE <= 1.0)
		 * 
		 * Outputs: (A set of messages sent to the network).
		 * 1. Wait for MIX_INTERVAL seconds.
		 * 2. If N > POOL_SIZE, then let 'max_send' =
		 * FLOOR(N*MAX_REPLACEMENT_RATE). [If 'max_send' < 0, let max_send = 1.]
		 * Choose Min(N-POOL_SIZE, max_send) messages from Q. Transmit the
		 * selected messages.
		 * 3. Repeat indefinitely."
		 * 
		 * note that "Q" is named "collectedMessages" in this class
		 * note that "max_send" is named "numberOfMessagesToSend" in this class
		 */
		public void putOutMessages() {
			synchronized (collectedMessages) {
				int N = collectedMessages.size();
				if (N > POOL_SIZE) { // "2. If N > POOL_SIZE, then ..."
					int numberOfMessagesToSend = (int) Math.floor(N * MAX_REPLACEMENT_RATE);
					if (numberOfMessagesToSend < 0)
						numberOfMessagesToSend = 1;
					numberOfMessagesToSend = Math.min(N - POOL_SIZE, numberOfMessagesToSend);
					// choose and transmit messages:
					if (numberOfMessagesToSend > 0) {
						if (isRequestPool) {
							Request[] requests = new Request[numberOfMessagesToSend];
							for (int i=0; i<numberOfMessagesToSend; i++) {
								int chosen = secureRandom.nextInt(collectedMessages.size());
								requests[i] = (Request)collectedMessages.remove(chosen);
							} 
							anonNode.putOutRequests(requests);
						} else {
							Reply[] replies = new Reply[numberOfMessagesToSend];
							for (int i=0; i<numberOfMessagesToSend; i++) {
								int chosen = secureRandom.nextInt(collectedMessages.size());
								replies[i] = (Reply)collectedMessages.remove(chosen);
							} 
							anonNode.putOutReplies(replies);
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

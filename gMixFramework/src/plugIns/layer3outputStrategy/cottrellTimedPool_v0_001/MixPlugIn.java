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
package plugIns.layer3outputStrategy.cottrellTimedPool_v0_001;

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
//every "outputRate" ms, send x (= "numberOfMessagesInPool" - "minPoolSize") 
//randomly chosen messages (if x >= 1)
public class MixPlugIn extends Implementation implements Layer3OutputStrategyMix {

	private SimplexCottrellTimedPool requestPool;
	private SimplexCottrellTimedPool replyPool;
	private static SecureRandom secureRandom;
	private int SENDING_RATE;
	private int MIN_POOL_SIZE;
	

	@Override
	public void constructor() {
		try {
			MixPlugIn.secureRandom = SecureRandom.getInstance(settings.getProperty("COTTRELL_TIMED_POOL_PRNG_ALGORITHM"));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException("could not init secureRandom"); 
		}
		this.SENDING_RATE = settings.getPropertyAsInt("COTTRELL_TIMED_POOL_SENDING_RATE");
		this.MIN_POOL_SIZE = settings.getPropertyAsInt("COTTRELL_TIMED_POOL_MIN_POOL_SIZE"); 
		this.requestPool = new SimplexCottrellTimedPool(true);
		this.replyPool = new SimplexCottrellTimedPool(false);
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
		requestPool.addMessage((MixMessage)request);
	}

	
	@Override
	public void addReply(Reply reply) {
		replyPool.addMessage((MixMessage)reply);
	}

	
	public class SimplexCottrellTimedPool {

		private boolean isRequestPool;
		private Vector<MixMessage> collectedMessages;
		private boolean isFirstMessage = true;
		private Timer timer = new Timer();
		
		
		public SimplexCottrellTimedPool(boolean isRequestPool) {
			this.collectedMessages = new Vector<MixMessage>(settings.getPropertyAsInt("COTTRELL_TIMED_POOL_DEFAULT_POOL_SIZE"));
			this.isRequestPool = isRequestPool;
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			synchronized (this) {
				if (isFirstMessage) {
					isFirstMessage = false;
					timer.scheduleAtFixedRate(new TimeoutTask(this), SENDING_RATE, SENDING_RATE);
				}
				collectedMessages.add(mixMessage);
			}
		}

		
		public void putOutMessages() {
			synchronized (this) {
				int numberOfMessagesToPutOut = collectedMessages.size() - MIN_POOL_SIZE;
				if (numberOfMessagesToPutOut > 0) {
					for (int i=0; i<numberOfMessagesToPutOut; i++) {
						int chosen = secureRandom.nextInt(collectedMessages.size());
						if (isRequestPool)
							anonNode.putOutRequest((Request)collectedMessages.remove(chosen));
						else
							anonNode.putOutReply((Reply)collectedMessages.remove(chosen));
					}
				}
			}		
		}
	
		
		private final class TimeoutTask extends TimerTask {

			private SimplexCottrellTimedPool linkedPool;
			
			protected TimeoutTask(SimplexCottrellTimedPool linkedPool) {
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

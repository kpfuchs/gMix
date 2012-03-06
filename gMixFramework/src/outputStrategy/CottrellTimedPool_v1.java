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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import message.MixMessage;
import message.Reply;
import message.Request;
import framework.Implementation;


//Cottrell 1995 ("Mixmaster & Remailer Attacks")
//every "outputRate" ms, send x (= "numberOfMessagesInPool" - "minPoolSize") 
//randomly chosen messages (if x >= 1)
public class CottrellTimedPool_v1 extends Implementation implements OutputStrategy {

	private SimplexCottrellTimedPool requestPool;
	private SimplexCottrellTimedPool replyPool;
	private static SecureRandom secureRandom;
	
	
	@Override
	public void constructor() {
		
		try {
			CottrellTimedPool_v1.secureRandom = SecureRandom.getInstance(settings.getProperty("PRNG_ALGORITHM"));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException("could not init secureRandom"); 
		}
		
		int sendingRate = settings.getPropertyAsInt("CottrellTimedPool_v1_SENDING_RATE");
		int minPoolSize = settings.getPropertyAsInt("CottrellTimedPool_v1_MIN_POOL_SIZE");
		
		this.requestPool = new SimplexCottrellTimedPool(true, sendingRate, minPoolSize);
		this.replyPool = new SimplexCottrellTimedPool(false, sendingRate, minPoolSize);

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
		private int sendingRate;
		private int minPoolSize;
		private Timer timer = new Timer();
		
		
		public SimplexCottrellTimedPool(boolean isRequestPool, int sendingRate, int minPoolSize) {
			
			this.collectedMessages = new Vector<MixMessage>(100);	// TODO property file
			this.isRequestPool = isRequestPool;
			this.sendingRate = sendingRate;
			this.minPoolSize = minPoolSize;
			
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			
			synchronized (this) {
				
				if (isFirstMessage) {
					isFirstMessage = false;
					timer.scheduleAtFixedRate(new TimeoutTask(this), sendingRate, sendingRate);
				}
				
				collectedMessages.add(mixMessage);
				
			}

		}

		
		public void putOutMessages() {
			
			synchronized (this) {
				
				int numberOfMessagesToPutOut = collectedMessages.size() - minPoolSize;
				
				if (numberOfMessagesToPutOut > 0) {
					
					for (int i=0; i<numberOfMessagesToPutOut; i++) {
						
						int chosen = secureRandom.nextInt(collectedMessages.size());
						
						if (isRequestPool)
							controller.getInputOutputHandler().addRequest((Request)collectedMessages.remove(chosen));
						else
							controller.getInputOutputHandler().addReply((Reply)collectedMessages.remove(chosen));
						
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
	
}

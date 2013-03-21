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
package plugIns.layer3outputStrategy.cottrellRandomDelay_v0_001;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Timer;
import java.util.TimerTask;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer3OutputStrategyMix;
import framework.core.message.Reply;
import framework.core.message.Request;


//Cottrell 1995 ("Mixmaster & Remailer Attacks")
//delays incoming messages randomly
public class MixPlugIn extends Implementation implements Layer3OutputStrategyMix {

	private static SecureRandom secureRandom;
	private Timer requestTimer = new Timer();
	private Timer replyTimer = new Timer();
	private int maxDelay;
	
	
	@Override
	public void constructor() {
		try {
			MixPlugIn.secureRandom = SecureRandom.getInstance(settings.getProperty("COTTRELL_RANDOM_DELAY_PRNG_ALGORITHM"));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException("could not init secureRandom"); 
		}
		this.maxDelay = settings.getPropertyAsInt("COTTRELL_RANDOM_DELAY_MAX_DELAY");
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
		synchronized (requestTimer) {
			requestTimer.schedule(new SendMessageTask(request), getRandomDelay());
		}
	}

	
	@Override
	public void addReply(Reply reply) {
		synchronized (replyTimer) {
			replyTimer.schedule(new SendMessageTask(reply), getRandomDelay());
		}
	}

	
	private int getRandomDelay() {
		return secureRandom.nextInt(maxDelay+1);
	}
	
	
	private final class SendMessageTask extends TimerTask {

		private Request request;
		private Reply reply;
		private boolean isRequest;
		
		
		protected SendMessageTask(Request request) {
			this.request = request;
			this.isRequest = true;
		}
		
		
		protected SendMessageTask(Reply reply) {
			this.reply = reply;
			this.isRequest = false;
		}
		
		
		@Override 
		public void run() {
			if (isRequest) {
				synchronized (requestTimer) {
					anonNode.putOutRequest(request);
				}
			} else {
				synchronized (replyTimer) {
					anonNode.putOutReply(reply);
				}
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

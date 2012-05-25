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

import message.MixMessage;
import message.Reply;
import message.Request;
import framework.Implementation;


//Cottrell 1995 ("Mixmaster & Remailer Attacks")
//delays incoming messages randomly
public class CottrellRandomDelay_v1 extends Implementation implements OutputStrategy {

	private static SecureRandom secureRandom;
	private Timer timer = new Timer();
	private int maxDelay;
	
	
	@Override
	public void constructor() {
		try {
			CottrellRandomDelay_v1.secureRandom = SecureRandom.getInstance(settings.getProperty("PRNG_ALGORITHM"));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException("could not init secureRandom"); 
		}
		this.maxDelay = settings.getPropertyAsInt("CottrellRandomDelay_v1_MAX_DELAY");
	}
	

	@Override
	public void initialize() {
		
	}

	
	@Override
	public void begin() {
		// no need to do anything
	}
	
	
	private int getRandomDelay() {
		return secureRandom.nextInt(maxDelay+1);
	}
	
	
	@Override
	public void addRequest(Request request) {
		synchronized (timer) {
			timer.schedule(new SendMessageTask((MixMessage) request, true), getRandomDelay());
		}
	}

	
	@Override
	public void addReply(Reply reply) {
		synchronized (timer) {
			timer.schedule(new SendMessageTask((MixMessage) reply, false), getRandomDelay());
		}
	}

	
	private final class SendMessageTask extends TimerTask {

		private MixMessage relatedMessage;
		private boolean isRequest;
		
		protected SendMessageTask(MixMessage mixMessage, boolean isRequest) {
			this.relatedMessage = mixMessage;
			this.isRequest = isRequest;
		}
		
		@Override 
		public void run() {
			
			synchronized (timer) {
				
				if (isRequest)
					controller.getInputOutputHandler().addRequest((Request)relatedMessage);
				else
					controller.getInputOutputHandler().addReply((Reply)relatedMessage);
				
			}
			
		}
			
	}
	
}

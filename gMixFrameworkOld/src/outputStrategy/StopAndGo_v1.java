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


import java.util.Timer;
import java.util.TimerTask;

import networkClock.NetworkClockController;

import message.MixMessage;
import message.Reply;
import message.Request;
import message.StopAndGoReply;
import message.StopAndGoRequest;
import framework.Implementation;


// Kesdogan et. al. 1998: Stop-and-Go MIXes: Providing Probabilistic Anonymity in an Open System
// under development
public class StopAndGo_v1 extends Implementation implements OutputStrategy {

	private boolean useTimeStamps;
	private NetworkClockController nwclock;
	private Timer timer = new Timer();
	
	
	@Override
	public void constructor() {
		this.useTimeStamps = true; // TODO: property file!
	}
	

	@Override
	public void initialize() {
		nwclock = mix.getNetworkClock();
	}
	

	@Override
	public void begin() {
		// no need to do anything
	}

	
	@Override
	public void addRequest(Request req) {
		
		//req.headers[0]; // TODO
		assert req instanceof StopAndGoRequest;
		StopAndGoRequest request = (StopAndGoRequest)req;
		
		synchronized (timer) {
			
			long now = nwclock.getTime();
			
			if (useTimeStamps)
				if (now < request.getTsMin() || now > request.getTsMax())
					return;
			
			timer.schedule(new SendMessageTask((MixMessage)request, true), request.getDelay());
			
		}
	}

	
	@Override
	public void addReply(Reply rep) {
		
		assert rep instanceof StopAndGoReply;
		StopAndGoReply reply = (StopAndGoReply)rep;
		
		synchronized (timer) {
			
			long now = nwclock.getTime();
			
			if (useTimeStamps)
				if (now < reply.getTsMin() || now > reply.getTsMax())
					return;
			
			timer.schedule(new SendMessageTask((MixMessage)reply, false), reply.getDelay());
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

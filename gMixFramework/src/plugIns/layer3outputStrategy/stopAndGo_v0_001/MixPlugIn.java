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
package plugIns.layer3outputStrategy.stopAndGo_v0_001;

import java.util.Timer;
import java.util.TimerTask;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer3OutputStrategyMix;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;


//Kesdogan et. al. 1998: Stop-and-Go MIXes: Providing Probabilistic Anonymity in an Open System
public class MixPlugIn extends Implementation implements Layer3OutputStrategyMix {

	private boolean useTimeStamps;
	private Timer timer = new Timer();
	
	
	@Override
	public void constructor() {
		this.useTimeStamps = settings.getPropertyAsBoolean("STOP_AND_GO_USE_TIMESTAMPS");
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
		TimestampHeader header = new TimestampHeader(request.headers[0]);
		synchronized (timer) {
			long now = clock.getTime();
			if (useTimeStamps)
				if (now < header.tsMin || now > header.tsMax)
					return;
			timer.schedule(new SendMessageTask((MixMessage)request), header.delay);
		}
	}


	@Override
	public void addReply(Reply reply) {
		TimestampHeader header = new TimestampHeader(reply.headers[0]);
		synchronized (timer) {
			long now = clock.getTime();
			if (useTimeStamps)
				if (now < header.tsMin || now > header.tsMax)
					return;
			timer.schedule(new SendMessageTask((MixMessage)reply), header.delay);
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
	
	
	private final class SendMessageTask extends TimerTask {

		private MixMessage relatedMessage;
		
		protected SendMessageTask(MixMessage mixMessage) {
			assert mixMessage != null;
			this.relatedMessage = mixMessage;
		}
		
		@Override 
		public void run() {
			synchronized (timer) {
				if (relatedMessage instanceof Request)
					anonNode.putOutRequest((Request)relatedMessage);
				else
					anonNode.putOutReply((Reply)relatedMessage);
			}
		}
			
	}
	
}

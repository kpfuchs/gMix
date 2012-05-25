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

import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import message.MixMessage;
import message.Reply;
import message.Request;
import framework.Implementation;


//Dingledine 2002: Timed Mix
//"The mix fires (flushes all messages) every t seconds"
public class TimedBatch_v1 extends Implementation implements OutputStrategy {

	private SimplexTimedBatch requestBatch;
	private SimplexTimedBatch replyBatch;

	
	@Override
	public void constructor() {
		int sendingRate = settings.getPropertyAsInt("TimedBatch_v1_SENDING_RATE");
		this.requestBatch = new SimplexTimedBatch(true, sendingRate);
		this.replyBatch = new SimplexTimedBatch(false, sendingRate);
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
		requestBatch.addMessage((MixMessage) request);
	}

	
	@Override
	public void addReply(Reply reply) {
		replyBatch.addMessage((MixMessage) reply);
	}

	
	public class SimplexTimedBatch {

		private boolean isRequestPool;
		private Vector<MixMessage> collectedMessages;
		private boolean isFirstMessage = true;
		private int sendingRate;
		private Timer timer = new Timer();
		
		
		public SimplexTimedBatch(boolean isRequestPool, int sendingRate) {
			
			this.collectedMessages = new Vector<MixMessage>(100);	// TODO property file
			this.isRequestPool = isRequestPool;
			this.sendingRate = sendingRate;
			
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
				
				Collections.sort(collectedMessages);
				
				for (MixMessage m:collectedMessages)
					if (isRequestPool)
						controller.getInputOutputHandler().addRequest((Request)m);
					else
						controller.getInputOutputHandler().addReply((Reply)m);

				
				this.collectedMessages = new Vector<MixMessage>(100);	// TODO property file
	
			}
				
		}
	
		
		private final class TimeoutTask extends TimerTask {

			private SimplexTimedBatch linkedBatch;
			
			protected TimeoutTask(SimplexTimedBatch linkedBatch) {
				this.linkedBatch = linkedBatch;
			}
			
			@Override 
			public void run() {
				linkedBatch.putOutMessages();
			}
			
		}
			
	}
	
}

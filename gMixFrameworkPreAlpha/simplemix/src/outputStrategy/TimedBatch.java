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

import message.Message;
import message.Reply;
import message.Request;
import architectureInterface.OutputStrategyInterface;
import framework.Implementation;


//Dingledine 2002: Timed Mix
//"The mix fires (flushes all messages) every t seconds"
public class TimedBatch extends Implementation implements OutputStrategyInterface {

	private SimplexTimedBatch requestBatch;
	private SimplexTimedBatch replyBatch;

	
	@Override
	public void constructor() {
		int sendingRate = 1000; // TODO: property file!
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
		requestBatch.addMessage((Message) request);
	}

	
	@Override
	public void addReply(Reply reply) {
		replyBatch.addMessage((Message) reply);
	}

	
	public class SimplexTimedBatch {

		private boolean isRequestPool;
		private Vector<Message> collectedMessages;
		private boolean isFirstMessage = true;
		private int sendingRate;
		private Timer timer = new Timer();
		
		
		public SimplexTimedBatch(boolean isRequestPool, int sendingRate) {
			
			this.collectedMessages = new Vector<Message>(100);	// TODO property file
			this.isRequestPool = isRequestPool;
			this.sendingRate = sendingRate;
			
		}
		
		
		public void addMessage(Message mixMessage) {
			
			synchronized (collectedMessages) {
				
				if (isFirstMessage) {
					isFirstMessage = false;
					timer.scheduleAtFixedRate(new TimeoutTask(this), sendingRate, sendingRate);
				}
				
				collectedMessages.add(mixMessage);
				
			}

		}

		
		public void putOutMessages() {
			
			synchronized (collectedMessages) {
				
				Collections.sort(collectedMessages);
				
				for (Message m:collectedMessages)
					if (isRequestPool)
						controller.getInputOutputHandler().addRequest((Request)m);
					else
						controller.getInputOutputHandler().addReply((Reply)m);

				
				this.collectedMessages = new Vector<Message>(100);	// TODO property file
	
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

	
	@Override
	public String[] getCompatibleImplementations() {
		return (new String[] {	"outputStrategy.BasicBatch",
								"outputStrategy.BasicPool",
								"inputOutputHandler.BasicInputOutputHandler",
								"keyGenerator.BasicKeyGenerator",
								"messageProcessor.BasicMessageProcessor",
								"externalInformationPort.BasicExternalInformationPort",
								"networkClock.BasicSystemTimeClock",
								"userDatabase.BasicUserDatabase",
								"message.BasicMessage"	
			});
	}


	@Override
	public boolean usesPropertyFile() {
		return false;
	}
	
}

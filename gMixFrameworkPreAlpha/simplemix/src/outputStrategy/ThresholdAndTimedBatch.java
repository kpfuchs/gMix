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
import java.util.Vector;

import message.Message;
import message.Reply;
import message.Request;
import architectureInterface.OutputStrategyInterface;
import framework.Implementation;


//Dingledine 2002: Timed Mix
//"fires (flushes all messages) every t seconds but only
//when at least n messages have accumulated in the mix.
public class ThresholdAndTimedBatch extends Implementation implements OutputStrategyInterface {

	private SimplexThresholdAndTimedBatch requestBatch;
	private SimplexThresholdAndTimedBatch replyBatch;

	
	@Override
	public void constructor() {
		int sendingRate = 1000; // TODO: property file!
		int batchSize = 100; // TODO: property file!
		this.requestBatch = new SimplexThresholdAndTimedBatch(true, sendingRate, batchSize);
		this.replyBatch = new SimplexThresholdAndTimedBatch(false, sendingRate, batchSize);
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

	
	public class SimplexThresholdAndTimedBatch {

		private boolean isRequestBatch;
		private Vector<Message> collectedMessages;
		private int batchSize;
		private Timer timer = new Timer();
		
		
		public SimplexThresholdAndTimedBatch(boolean isRequestBatch, int sendingRate, int batchSize) {
			
			this.collectedMessages = new Vector<Message>(batchSize);
			this.isRequestBatch = isRequestBatch;
			this.batchSize = batchSize;
			this.timer.scheduleAtFixedRate(new TimeoutTask(this), sendingRate, sendingRate);
			
		}
		
		
		public void addMessage(Message mixMessage) {
			
			synchronized (collectedMessages) {
				
				collectedMessages.add(mixMessage);

			}

		}

		
		public void putOutMessages() {
			
			synchronized (collectedMessages) {
				
				if (collectedMessages.size() != batchSize)
					return;
					
				if (isRequestBatch)
					for (Message m:collectedMessages)
						controller.getInputOutputHandler().addRequest((Request)m);
				else
					for (Message m:collectedMessages)
						controller.getInputOutputHandler().addReply((Reply)m);
				
				this.collectedMessages = new Vector<Message>(batchSize);
	
			}
				
		}
	
		
		private final class TimeoutTask extends TimerTask {

			private SimplexThresholdAndTimedBatch linkedBatch;
			
			protected TimeoutTask(SimplexThresholdAndTimedBatch linkedBatch) {
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

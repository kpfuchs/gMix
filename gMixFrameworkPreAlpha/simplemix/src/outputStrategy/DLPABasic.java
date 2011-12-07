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

import networkClock.NetworkClockController;

import message.Message;
import message.Reply;
import message.Request;
import architectureInterface.OutputStrategyInterface;
import framework.Implementation;


//DLP Algorithm (2008: Wei Wang, Mehul Motani, Vikram Srinivasan: Dependent Link Padding Algorithms for Low Latency Anonymity Systems)
public class DLPABasic extends Implementation implements OutputStrategyInterface {

	private int maxRequestDelay = 1000; // TODO
	private int maxReplyDelay = 1000; // TODO
	public Vector<DLPAOutputSlot> requestOutputSlots = new Vector<DLPAOutputSlot>(); // TODO: private
	public Vector<DLPAOutputSlot> replyOutputSlots = new Vector<DLPAOutputSlot>(); // TODO: private
	private NetworkClockController nwclock;
	private Timer timer = new Timer();
	
	
	@Override
	public void constructor() {
		// no need to do anything
	}
	

	@Override
	public void initialize() {
		nwclock = mix.getNetworkClock();
	}
	

	@Override
	public void begin() {
		// no need to do anything
	}

	
	public DLPAOutputSlot getUnusedOutputSlot(Message message) {

		if (message instanceof Request) {
			
			synchronized(requestOutputSlots) {
				
				long latestOutputPossible = nwclock.getTime() + maxRequestDelay;

				for (DLPAOutputSlot outputSlot:requestOutputSlots) {
					if (outputSlot.getTimeOfOutput() <= latestOutputPossible && !outputSlot.isUsedBy(message.getIdentifier())) {
						return outputSlot;
					}
				}
				
				return null;
				
			}
			
		} else {
			
			synchronized(replyOutputSlots) {
				
				long latestOutputPossible = nwclock.getTime() + maxReplyDelay;
	
				for (DLPAOutputSlot outputSlot:replyOutputSlots) {
					if (outputSlot.getTimeOfOutput() <= latestOutputPossible && !outputSlot.isUsedBy(message.getIdentifier())) {
						return outputSlot;
					}
				}
				
				return null;
				
			}

		}
	
	}
	
	
	@Override
	public void addRequest(Request request) {
		
		synchronized(requestOutputSlots) {
			
			DLPAOutputSlot outputSlot = getUnusedOutputSlot((Message)request);
			
			if (outputSlot == null) { // generate new OutputSlot if necessary
				outputSlot = new DLPAOutputSlot(true, maxRequestDelay, mix);
				requestOutputSlots.add(outputSlot);
				timer.schedule(new DLPAPutOutSlotTask(outputSlot, true), maxRequestDelay);
			}
			
			outputSlot.addMessage((Message)request);
			
			if (outputSlot.getNumerOfMessagesContained() == 10) // TODO Property File
				outputSlot.putOutMessages();

		}
	
	}

	
	@Override
	public void addReply(Reply reply) {
		
		synchronized(replyOutputSlots) {
			
			DLPAOutputSlot outputSlot = getUnusedOutputSlot((Message)reply);
			
			if (outputSlot == null) { // generate new OutputSlot if necessary
				outputSlot = new DLPAOutputSlot(false, maxReplyDelay, mix);
				replyOutputSlots.add(outputSlot);
				timer.schedule(new DLPAPutOutSlotTask(outputSlot, false), maxReplyDelay);
			}
			
			outputSlot.addMessage((Message)reply);
			
			if (outputSlot.getNumerOfMessagesContained() == 10) // TODO Property File
				outputSlot.putOutMessages();
			
		}

	}

	
	private final class DLPAPutOutSlotTask extends TimerTask {

		private DLPAOutputSlot relatedSlot;
		private boolean isRequestSlot;
		
		
		protected DLPAPutOutSlotTask(DLPAOutputSlot relatedSlot, boolean isRequestSlot) {
			this.relatedSlot = relatedSlot;
			this.isRequestSlot = isRequestSlot;
		}
		
		
		@Override 
		public void run() {
			
			if (isRequestSlot) {
				
				synchronized(requestOutputSlots) {
					if (requestOutputSlots.contains(relatedSlot)) { // if not contained, slot was output during timeout (-> thread synchronization)
						relatedSlot.putOutMessages();
						requestOutputSlots.remove(relatedSlot);
					}
				}
				
			} else {
				
				synchronized(replyOutputSlots) {
					if (replyOutputSlots.contains(relatedSlot)) { // if not contained, slot was output during timeout (-> thread synchronization)
						relatedSlot.putOutMessages();
						replyOutputSlots.remove(relatedSlot);
					}
				}
				
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

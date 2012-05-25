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

import message.MixMessage;
import message.Reply;
import message.Request;
import framework.Implementation;


//DLP Algorithm (2008: Wei Wang, Mehul Motani, Vikram Srinivasan: Dependent Link Padding Algorithms for Low Latency Anonymity Systems)
public class DLPABasic_v1 extends Implementation implements OutputStrategy {

	private int maxRequestDelay;
	private int maxReplyDelay;
	public Vector<DLPABasic_v1_OutputSlot> requestOutputSlots = new Vector<DLPABasic_v1_OutputSlot>(); // TODO: private
	public Vector<DLPABasic_v1_OutputSlot> replyOutputSlots = new Vector<DLPABasic_v1_OutputSlot>(); // TODO: private
	private NetworkClockController nwclock;
	private Timer timer = new Timer();
	
	
	@Override
	public void constructor() {
		this.maxRequestDelay = settings.getPropertyAsInt("DLPABasic_v1_MAX_REQUEST_DELAY");
		this.maxReplyDelay = settings.getPropertyAsInt("DLPABasic_v1_MAX_REPLY_DELAY");
	}
	

	@Override
	public void initialize() {
		nwclock = mix.getNetworkClock();
	}
	

	@Override
	public void begin() {
		// no need to do anything
	}
	
	
	public DLPABasic_v1_OutputSlot getUnusedOutputSlot(MixMessage mixMessage) {

		if (mixMessage instanceof Request) {
			
			synchronized(requestOutputSlots) {
				
				long latestOutputPossible = nwclock.getTime() + maxRequestDelay;

				for (DLPABasic_v1_OutputSlot outputSlot:requestOutputSlots) {
					if (outputSlot.getTimeOfOutput() <= latestOutputPossible && !outputSlot.isUsedBy(mixMessage.getOwner())) {
						return outputSlot;
					}
				}
				
				return null;
				
			}
			
		} else {
			
			synchronized(replyOutputSlots) {
				
				long latestOutputPossible = nwclock.getTime() + maxReplyDelay;
	
				for (DLPABasic_v1_OutputSlot outputSlot:replyOutputSlots) {
					if (outputSlot.getTimeOfOutput() <= latestOutputPossible && !outputSlot.isUsedBy(mixMessage.getOwner())) {
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
			
			DLPABasic_v1_OutputSlot outputSlot = getUnusedOutputSlot((MixMessage)request);
			
			if (outputSlot == null) { // generate new OutputSlot if necessary
				outputSlot = new DLPABasic_v1_OutputSlot(true, nwclock.getTime() + maxRequestDelay, mix);
				requestOutputSlots.add(outputSlot);
				timer.schedule(new DLPAPutOutSlotTask(outputSlot, true), maxRequestDelay);
			}
			
			outputSlot.addMessage((MixMessage)request);
			
			if (outputSlot.getNumerOfMessagesContained() >= userDatabase.getNumberOfUsers()) // TODO Property File
				outputSlot.putOutMessages();

		}
	
	}

	
	@Override
	public void addReply(Reply reply) {
		
		synchronized(replyOutputSlots) {
			
			DLPABasic_v1_OutputSlot outputSlot = getUnusedOutputSlot((MixMessage)reply);
			
			if (outputSlot == null) { // generate new OutputSlot if necessary
				outputSlot = new DLPABasic_v1_OutputSlot(false, nwclock.getTime() + maxReplyDelay, mix);
				replyOutputSlots.add(outputSlot);
				timer.schedule(new DLPAPutOutSlotTask(outputSlot, false), maxReplyDelay);
			}
			
			outputSlot.addMessage((MixMessage)reply);
			
			if (outputSlot.getNumerOfMessagesContained() >= userDatabase.getNumberOfUsers()) // TODO Property File
				outputSlot.putOutMessages();
			
		}

	}

	
	private final class DLPAPutOutSlotTask extends TimerTask {

		private DLPABasic_v1_OutputSlot relatedSlot;
		private boolean isRequestSlot;
		
		
		protected DLPAPutOutSlotTask(DLPABasic_v1_OutputSlot relatedSlot, boolean isRequestSlot) {
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
	
}

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
package plugIns.layer3outputStrategy.dlpaBasic_v0_001;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer3OutputStrategyMix;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;


//DLP Algorithm (2008: Wei Wang, Mehul Motani, Vikram Srinivasan: Dependent Link Padding Algorithms for Low Latency Anonymity Systems)
public class MixPlugIn extends Implementation implements Layer3OutputStrategyMix {

	private int maxRequestDelay;
	private int maxReplyDelay;
	public Vector<OutputSlot> requestOutputSlots = new Vector<OutputSlot>(); // TODO: private
	public Vector<OutputSlot> replyOutputSlots = new Vector<OutputSlot>(); // TODO: private
	private Timer timer = new Timer();
	

	@Override
	public void constructor() {
		this.maxRequestDelay = settings.getPropertyAsInt("DLPA_BASIC_MAX_REQUEST_DELAY");
		this.maxReplyDelay = settings.getPropertyAsInt("DLPA_BASIC_MAX_REPLY_DELAY");
	}
	
	
	@Override
	public void initialize() {
		// no need to do anything
	}

	
	@Override
	public void begin() {
		// no need to do anything
	}

	
	public OutputSlot getUnusedOutputSlot(MixMessage mixMessage) {
		if (mixMessage instanceof Request) {
			synchronized(requestOutputSlots) {
				long latestOutputPossible = clock.getTime() + maxRequestDelay;
				for (OutputSlot outputSlot:requestOutputSlots) {
					if (outputSlot.getTimeOfOutput() <= latestOutputPossible && !outputSlot.isUsedBy(mixMessage.getOwner())) {
						return outputSlot;
					}
				}
				return null;
			}
		} else {
			synchronized(replyOutputSlots) {
				long latestOutputPossible = clock.getTime() + maxReplyDelay;
				for (OutputSlot outputSlot:replyOutputSlots) {
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
			OutputSlot outputSlot = getUnusedOutputSlot((MixMessage)request);
			if (outputSlot == null) { // generate new OutputSlot if necessary
				outputSlot = new OutputSlot(true, clock.getTime() + maxRequestDelay, anonNode);
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
			OutputSlot outputSlot = getUnusedOutputSlot((MixMessage)reply);
			if (outputSlot == null) { // generate new OutputSlot if necessary
				outputSlot = new OutputSlot(false, clock.getTime() + maxReplyDelay, anonNode);
				replyOutputSlots.add(outputSlot);
				timer.schedule(new DLPAPutOutSlotTask(outputSlot, false), maxReplyDelay);
			}
			outputSlot.addMessage((MixMessage)reply);
			if (outputSlot.getNumerOfMessagesContained() >= userDatabase.getNumberOfUsers())
				outputSlot.putOutMessages();
		}
	}

	
	private final class DLPAPutOutSlotTask extends TimerTask {

		private OutputSlot relatedSlot;
		private boolean isRequestSlot;
		
		
		protected DLPAPutOutSlotTask(OutputSlot relatedSlot, boolean isRequestSlot) {
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
	public int getMaxSizeOfNextReply() {
		return super.recodingLayerMix.getMaxSizeOfNextReply();
	}


	@Override
	public int getMaxSizeOfNextRequest() {
		return super.recodingLayerMix.getMaxSizeOfNextRequest();
	}
	
}

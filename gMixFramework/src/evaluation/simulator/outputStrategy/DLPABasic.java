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
package evaluation.simulator.outputStrategy;

import java.util.Vector;


import evaluation.simulator.communicationBehaviour.LastMixCommunicationBehaviour;
import evaluation.simulator.core.Simulator;
import evaluation.simulator.message.MixMessage;
import evaluation.simulator.message.TransportMessage;
import evaluation.simulator.networkComponent.IdGenerator;
import evaluation.simulator.networkComponent.Identifiable;
import evaluation.simulator.networkComponent.Mix;
import evaluation.simulator.statistics.Statistics;


// DLP Algorithm (2008: Wei Wang, Mehul Motani, Vikram Srinivasan: Dependent Link Padding Algorithms for Low Latency Anonymity Systems)
public class DLPABasic extends OutputStrategy implements Identifiable {

	private int maxRequestDelay;
	private int maxReplyDelay;
	public Vector<DLPAOutputSlot> requestOutputSlots = new Vector<DLPAOutputSlot>();
	public Vector<DLPAOutputSlot> replyOutputSlots = new Vector<DLPAOutputSlot>();
	public Statistics statistics;
	private int numericIdentifier;
	private LastMixCommunicationBehaviour lastMixCommunicationBehaviour;
	
	
	protected DLPABasic(Mix mix, Simulator simulator) {
		
		super(mix, simulator);
		this.maxRequestDelay = Simulator.settings.getPropertyAsInt("MAX_REQUEST_DELAY");
		this.maxReplyDelay = Simulator.settings.getPropertyAsInt("MAX_REPLY_DELAY");
		this.statistics = new Statistics(this);
		this.numericIdentifier = IdGenerator.getId();
		this.lastMixCommunicationBehaviour = LastMixCommunicationBehaviour.getInstance(mix, simulator, this);
		
	}

	
	/*@Override
	public void addRequest(MixMessage mixMessage) {

		
		DLPAOutputSlot outputSlot = getUnusedOutputSlot(mixMessage);
		
		if (outputSlot == null) { // generate new OutputSlot if necessary
			outputSlot = new DLPAOutputSlot(true, maxRequestDelay, simulator, mix, this);
			requestOutputSlots.add(outputSlot);
		}
		
		outputSlot.addMessage(mixMessage);
		
	}
*/
	
	public DLPAOutputSlot getUnusedOutputSlot(MixMessage mixMessage) {

		int maxDelay = mixMessage.isRequest() ? this.maxRequestDelay : this.maxReplyDelay;
		long latestOutputPossible = Simulator.getNow() + maxDelay;
		Vector<DLPAOutputSlot> outputSlots = mixMessage.isRequest() ? this.requestOutputSlots : this.replyOutputSlots;
		
		for (DLPAOutputSlot outputSlot:outputSlots) {
			if (outputSlot.getTimeOfOutput() <= latestOutputPossible && !outputSlot.isUsedBy(mixMessage.getOwner())) {
				
				return outputSlot;
			}
		}
		return null;
			
	}


	public void removeRequestOutputslot(DLPAOutputSlot dlpaOutputSlot) {
		requestOutputSlots.remove(dlpaOutputSlot);
	}
	
	
	public void removeReplyOutputslot(DLPAOutputSlot dlpaOutputSlot) {
		replyOutputSlots.remove(dlpaOutputSlot);
	}


	@Override
	public int getGlobalId() {
		return numericIdentifier;
	}


	@Override
	public void incomingRequest(MixMessage mixMessage) {
		
		DLPAOutputSlot outputSlot = getUnusedOutputSlot(mixMessage);
		
		if (outputSlot == null) { // generate new OutputSlot if necessary
			outputSlot = new DLPAOutputSlot(true, maxRequestDelay, simulator, mix, this);
			requestOutputSlots.add(outputSlot);
		}
		
		outputSlot.addMessage(mixMessage);
		
		if (outputSlot.getNumerOfMessagesContained() == simulator.getClients().size()) { // TODO
			outputSlot.putOutMessages();
			simulator.unscheduleEvent(outputSlot.getTimeoutEvent());
		}
		
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		
		DLPAOutputSlot outputSlot = getUnusedOutputSlot(mixMessage);
		
		if (outputSlot == null) { // generate new OutputSlot if necessary
			outputSlot = new DLPAOutputSlot(false, maxReplyDelay, simulator, mix, this);
			replyOutputSlots.add(outputSlot);
		}
		
		outputSlot.addMessage(mixMessage);
		
		if (outputSlot.getNumerOfMessagesContained() == simulator.getClients().size()) { // TODO
			outputSlot.putOutMessages();
			simulator.unscheduleEvent(outputSlot.getTimeoutEvent());
		}
		
	}


	@Override
	public void incomingReply(TransportMessage noneMixMessage) {
		
		lastMixCommunicationBehaviour.incomingDataFromDistantProxy(noneMixMessage);
		
	}

}

/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
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
 *******************************************************************************/
package userGeneratedContent.simulatorPlugIns.plugins.outputStrategy;

import java.util.Vector;

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.annotations.plugin.Plugin;
import staticContent.evaluation.simulator.annotations.property.IntSimulationProperty;
import staticContent.evaluation.simulator.core.message.MixMessage;
import staticContent.evaluation.simulator.core.networkComponent.AbstractClient;
import staticContent.evaluation.simulator.core.networkComponent.IdGenerator;
import staticContent.evaluation.simulator.core.networkComponent.Identifiable;
import staticContent.evaluation.simulator.core.networkComponent.Mix;
import staticContent.evaluation.simulator.core.statistics.Statistics;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.ClientSendStyle;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.MixSendStyle;
import userGeneratedContent.simulatorPlugIns.plugins.clientSendStyle.ClientSendStyleImpl;
import userGeneratedContent.simulatorPlugIns.plugins.mixSendStyle.MixSendStyleImpl;


// DLP Algorithm (2008: Wei Wang, Mehul Motani, Vikram Srinivasan: Dependent Link Padding Algorithms for Low Latency Anonymity Systems)
@Plugin(pluginKey = "DLPA_BASIC", pluginName = "DPLA Basic")
public class DLPABasic extends OutputStrategyImpl implements Identifiable {

	// Requirement
	@IntSimulationProperty( name="Maximum request delay (ms)", 
			key="MAX_DLPAB_REQUEST_DELAY",
			min = 0)
	private int maxRequestDelay;
	
	@IntSimulationProperty( name="Maximum reply delay (ms)", 
			key="MAX_DLPAB_REPLY_DELAY",
			min = 0)
	private int maxReplyDelay;
	
	public Vector<DLPAOutputSlot> requestOutputSlots = new Vector<DLPAOutputSlot>(100,100);
	public Vector<DLPAOutputSlot> replyOutputSlots = new Vector<DLPAOutputSlot>(100,100);
	public Statistics statistics;
	private int numericIdentifier;
	
	
	public DLPABasic(Mix mix, Simulator simulator) {
		super(mix, simulator);
		this.maxRequestDelay = Simulator.settings.getPropertyAsInt("MAX_DLPAB_REQUEST_DELAY");
		this.maxReplyDelay = Simulator.settings.getPropertyAsInt("MAX_DLPAB_REPLY_DELAY");
		this.statistics = new Statistics(this);
		this.numericIdentifier = IdGenerator.getId();
	}

	
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
		
		if (outputSlot.getNumerOfMessagesContained() == simulator.getNumberOfClients()) {
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
		
		if (outputSlot.getNumerOfMessagesContained() == simulator.getNumberOfClients()) {
			outputSlot.putOutMessages();
			simulator.unscheduleEvent(outputSlot.getTimeoutEvent());
		}
		
	}


	@Override
	public ClientSendStyleImpl getClientSendStyle(AbstractClient client) {
		return ClientSendStyle.getInstance(client);
	}


	@Override
	public MixSendStyleImpl getMixSendStyle() {
		return MixSendStyle.getInstance(mix, mix);
	}
	
}

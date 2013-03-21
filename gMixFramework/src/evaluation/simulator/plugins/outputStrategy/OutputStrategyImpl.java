/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2013  Karl-Peter Fuchs
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
package evaluation.simulator.plugins.outputStrategy;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.Mix;
import evaluation.simulator.plugins.clientSendStyle.ClientSendStyleImpl;
import evaluation.simulator.plugins.mixSendStyle.MixSendStyleImpl;
import evaluation.simulator.plugins.mixSendStyle.ReplyReceiver;


public abstract class OutputStrategyImpl implements ReplyReceiver {

	protected Simulator simulator;
	protected Mix mix;
	protected boolean simulateRequestChannel;
	protected boolean simulateReplyChannel;
	
	
	public OutputStrategyImpl(Mix mix, Simulator simulator) {
		this.mix = mix;
		this.simulator = simulator;
		if (Simulator.settings.getProperty("COMMUNICATION_MODE").equals("SIMPLEX_REPLY") || Simulator.settings.getProperty("COMMUNICATION_MODE").equals("DUPLEX"))
			simulateReplyChannel = true;
		else
			simulateReplyChannel = false;
		if (Simulator.settings.getProperty("COMMUNICATION_MODE").equals("SIMPLEX_REPLY"))
			simulateRequestChannel = false;
		else
			simulateRequestChannel = true;
	}
	
	
	/**
	 * request from client or previous mix
	 * must call mix.putOutRequest(NetworkMessage networkMessage)
	 */
	public abstract void incomingRequest(MixMessage mixMessage);
	
	
	/**
	 * reply from next mix
	 * must call mix.putOutReply(MixMessage mixMessage)
	 */
	public abstract void incomingReply(MixMessage mixMessage);

	
	/**
	 * reply from distant proxy
	 * must call mix.putOutReply(MixMessage mixMessage)
	 * @param client 
	 */
	
	
	public abstract ClientSendStyleImpl getClientSendStyle(AbstractClient client);
	public abstract MixSendStyleImpl getMixSendStyle();
	
	
	public Mix getOwner() {
		return mix;
	}
	
}
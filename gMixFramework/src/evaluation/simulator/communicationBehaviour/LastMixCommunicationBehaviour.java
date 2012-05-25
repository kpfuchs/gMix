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

package evaluation.simulator.communicationBehaviour;

import evaluation.simulator.core.Simulator;
import evaluation.simulator.message.NoneMixMessage;
import evaluation.simulator.networkComponent.NetworkNode;
import evaluation.simulator.statistics.Statistics;


public abstract class LastMixCommunicationBehaviour {

	protected NetworkNode owner;
	protected Simulator simulator;
	protected Statistics statistics;
	protected ReplyReceiver replyReceiver;


	public static LastMixCommunicationBehaviour getInstance(NetworkNode owner, Simulator simulator, ReplyReceiver replyReceiver) {
		
		String type = Simulator.settings.getProperty("LAST_MIX_COMMUNICATION_BEHAVIOUR");
		
		if (type.equals("REPLY_IMMEDIATELY"))
			return new LastMixReplyImmediately(owner, simulator, replyReceiver);
		else if (type.equals("WAIT_FOR_FURTHER_DATA_BEFORE_REPLY"))
			return new LastMixWaitForFurtherDataBeforeReply(owner, simulator, replyReceiver);
		else
			throw new RuntimeException("ERROR: Unknown or not supported communicationBehaviour! " +type);

	}
	
	
	protected LastMixCommunicationBehaviour(	NetworkNode owner, 
											Simulator simulator, 
											ReplyReceiver replyReceiver
											) {
		
		this.simulator = simulator;
		this.owner = owner;
		this.statistics = owner.getStatistics();
		this.replyReceiver = replyReceiver;
		
	}
	
	
	// must call replyReceiver.addReply(MixMessage mixMessage)
	public abstract void incomingDataFromDistantProxy(NoneMixMessage noneMixMessage);

}

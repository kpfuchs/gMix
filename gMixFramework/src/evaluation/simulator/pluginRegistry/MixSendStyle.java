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
package evaluation.simulator.pluginRegistry;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.networkComponent.NetworkNode;
import evaluation.simulator.plugins.mixSendStyle.LastMixReplyImmediately;
import evaluation.simulator.plugins.mixSendStyle.LastMixWaitForFurtherDataBeforeReply;
import evaluation.simulator.plugins.mixSendStyle.MixSendStyleImpl;
import evaluation.simulator.plugins.mixSendStyle.ReplyReceiver;


public enum MixSendStyle {

	SEND_IMMEDIATELY(),
	WAIT_FOR_FURTHER_DATA()
	;
	
	
	public static MixSendStyleImpl getInstance(NetworkNode owner, ReplyReceiver replyReceiver) {
		String desiredImpl = Simulator.settings.getProperty("MIX_SEND_STYLE");
		if (desiredImpl.equals("REPLY_IMMEDIATELY")) {
			return new LastMixReplyImmediately(owner, Simulator.getSimulator(), replyReceiver);
		} else if (desiredImpl.equals("WAIT_FOR_FURTHER_DATA_BEFORE_REPLY")) {
			return new LastMixWaitForFurtherDataBeforeReply(owner, Simulator.getSimulator(), replyReceiver);
		} else
			throw new RuntimeException("ERROR: no MixSendStyle with the name \"" +desiredImpl  
				+"\" available (Key \"MIX_SEND_STYLE\" in experiment config file. See " +
				"\"MixSendStyle.java\" for available MixSendStyles.");
	}
	
}

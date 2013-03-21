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
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.plugins.clientSendStyle.ClientBasicSynchronous;
import evaluation.simulator.plugins.clientSendStyle.ClientSendImmediately;
import evaluation.simulator.plugins.clientSendStyle.ClientSendStyleImpl;
import evaluation.simulator.plugins.clientSendStyle.ClientSendWithoutMixes;
import evaluation.simulator.plugins.clientSendStyle.ClientWaitForFurtherDataBeforeSend;
import evaluation.simulator.plugins.clientSendStyle.ClientWaitForReply;


public enum ClientSendStyle {

	SEND_IMMEDIATELY,
	WAIT_FOR_FURTHER_DATA,
	SEND_WITHOUT_MIXES,
	SEND_SYNCHRONOUS,
	WAIT_FOR_REPLY
	;
	
	
	public static ClientSendStyleImpl getInstance(AbstractClient owner) {
		boolean noMixes = !Topology.getTopology().containsAtLeastOneMix();
		boolean noRequestChannel = Simulator.settings.getProperty("COMMUNICATION_MODE").equals("SIMPLEX_REPLY");
		String desiredImpl = Simulator.settings.getProperty("CLIENT_SEND_STYLE");
		if (noMixes || noRequestChannel) {
			return new ClientSendWithoutMixes(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("SEND_IMMEDIATELY")) {
			return new ClientSendImmediately(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("WAIT_FOR_FURTHER_DATA")) {
			return new ClientWaitForFurtherDataBeforeSend(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("SEND_WITHOUT_MIXES")) {
			return new ClientSendWithoutMixes(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("SEND_SYNCHRONOUS")) {
			return new ClientBasicSynchronous(owner, Simulator.getSimulator());
		} else if (desiredImpl.equals("WAIT_FOR_REPLY")) {
			return new ClientWaitForReply(owner, Simulator.getSimulator());
		} else
			throw new RuntimeException("ERROR: no ClientSendStyle with the name \"" +desiredImpl  
				+"\" available (Key \"CLIENT_SEND_STYLE\" in experiment config file. See " +
				"\"ClientSendStyle.java\" for available ClientSendStyles.");
	}
	
}

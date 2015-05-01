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
package userGeneratedContent.simulatorPlugIns.plugins.clientSendStyle;

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.annotations.plugin.Plugin;
import staticContent.evaluation.simulator.core.message.NetworkMessage;
import staticContent.evaluation.simulator.core.message.TransportMessage;
import staticContent.evaluation.simulator.core.networkComponent.AbstractClient;

@Plugin(pluginKey = "SEND_WITHOUT_MIXES", pluginName = "Send Without Mixes")
public class ClientSendWithoutMixes extends ClientSendStyleImpl {
	
	public ClientSendWithoutMixes(AbstractClient owner, Simulator simulator) {

		super(owner, simulator);

	}

	@Override
	public void incomingDecryptedReply(NetworkMessage reply) {

	}

	@Override
	public void incomingRequestFromUser(TransportMessage request) {
		this.owner.sendRequest(request);

	}

	@Override
	public void messageReachedServer(TransportMessage request) {

	}

}

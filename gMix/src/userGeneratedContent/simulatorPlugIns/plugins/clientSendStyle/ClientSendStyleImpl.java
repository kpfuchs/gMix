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
import staticContent.evaluation.simulator.annotations.plugin.PluginSuperclass;
import staticContent.evaluation.simulator.core.event.Event;
import staticContent.evaluation.simulator.core.event.EventExecutor;
import staticContent.evaluation.simulator.core.message.NetworkMessage;
import staticContent.evaluation.simulator.core.message.TransportMessage;
import staticContent.evaluation.simulator.core.networkComponent.AbstractClient;
import staticContent.evaluation.simulator.core.statistics.Statistics;

@PluginSuperclass( layerName = "Mix Client", layerKey = "CLIENT_SEND_STYLE", position = 1)
public abstract class ClientSendStyleImpl implements EventExecutor {
	
	protected AbstractClient owner;

	protected boolean simulateReplyChannel;
	protected Simulator simulator;
	protected Statistics statistics;

	protected ClientSendStyleImpl(AbstractClient owner, Simulator simulator) {
		this.simulator = simulator;
		this.owner = owner;
		this.statistics = owner.getStatistics();
		this.simulateReplyChannel = Simulator.settings.getProperty(
				"COMMUNICATION_MODE").equals("SIMPLEX_REPLY")
				|| Simulator.settings.getProperty("COMMUNICATION_MODE").equals(
						"DUPLEX");
	}

	// overwrite in subclass if needed
	@Override
	public void executeEvent(Event e) {
		throw new RuntimeException("Not Implemented");

	}

	public AbstractClient getOwner() {
		return this.owner;
	}

	public abstract void incomingDecryptedReply(NetworkMessage reply);

	// must call owner.sendRequest(NetworkMessage);
	public abstract void incomingRequestFromUser(TransportMessage request);

	public abstract void messageReachedServer(TransportMessage request);

	public void setOwner(AbstractClient owner) {
		this.owner = owner;
	}

}

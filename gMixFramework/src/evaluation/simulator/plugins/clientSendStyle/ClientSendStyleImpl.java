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
package evaluation.simulator.plugins.clientSendStyle;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.event.EventExecutor;
import evaluation.simulator.core.message.NetworkMessage;
import evaluation.simulator.core.message.TransportMessage;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.statistics.Statistics;


public abstract class ClientSendStyleImpl implements EventExecutor {

	protected AbstractClient owner;
	protected Simulator simulator;
	protected Statistics statistics;
	protected boolean simulateReplyChannel; 
	
	
	protected ClientSendStyleImpl(AbstractClient owner, Simulator simulator) {
		this.simulator = simulator;
		this.owner = owner;
		this.statistics = owner.getStatistics();
		this.simulateReplyChannel = Simulator.settings.getProperty("COMMUNICATION_MODE").equals("SIMPLEX_REPLY") || Simulator.settings.getProperty("COMMUNICATION_MODE").equals("DUPLEX");
	}
	
	
	// must call owner.sendRequest(NetworkMessage);
	public abstract void incomingRequestFromUser(TransportMessage request);
	
	
	public abstract void messageReachedServer(TransportMessage request);
	
	
	public abstract void incomingDecryptedReply(NetworkMessage reply);

	
	
	public void setOwner(AbstractClient owner) {
		this.owner = owner;
	}


	public AbstractClient getOwner() {
		return owner;
	}
	
	
	// overwrite in subclass if needed
	public void executeEvent(Event e) {
		throw new RuntimeException("Not Implemented");
		
	}

}

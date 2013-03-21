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
package evaluation.simulator.plugins.topology;

import java.util.HashMap;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.DistantProxy;
import evaluation.simulator.core.networkComponent.Mix;
import evaluation.simulator.core.networkComponent.NetworkConnection;
import evaluation.simulator.pluginRegistry.DelayBox.TypeOfNode;
import evaluation.simulator.pluginRegistry.DelayBox;


public class NMixCascadeTopology extends TopologyScript {
	
	private int numberOfMixes;
	private HashMap<String, AbstractClient> clients;
	private HashMap<String, Mix> mixes;
	private HashMap<String, NetworkConnection> networkConnections;
	private DistantProxy distantProxy;
	
	
	public NMixCascadeTopology(int numberOfMixes) {
		this.numberOfMixes = numberOfMixes;
	}

	
	@Override
	public void constructor(AbstractClient[] clients) {
		this.clients = new HashMap<String, AbstractClient>(clients.length*2);
		this.mixes = new HashMap<String, Mix>(numberOfMixes*2);
		this.networkConnections = new HashMap<String, NetworkConnection>(numberOfMixes*2);
		this.distantProxy = DistantProxy.getInstance("DistantProxy", Simulator.getSimulator());
		this.distantProxy.setDelayBox(DelayBox.getInstance(TypeOfNode.DISTANT_PROXY));
		// create mixes:
		for (int i=1; i<=numberOfMixes; i++) {
			boolean isFirst = (i == 1);
			boolean isLast = (i == numberOfMixes);
			Mix mix = new Mix("Mix"+i, Simulator.getSimulator(), isFirst, isLast);
			mix.setDelayBox(DelayBox.getInstance(TypeOfNode.MIX));
			mixes.put("Mix:Mix1", mix);
		}
		// connect mixes:
		for (int i=1; i<=numberOfMixes; i++) {
			if (i == numberOfMixes) // last mix
				networkConnections.put("NetworkConnection:NetworkConnection" +i +":Mix" +i +"<->DistantProxy", new NetworkConnection(mixes.get("Mix:Mix" +i), distantProxy, Simulator.getSimulator()));
			else // not last mix
				networkConnections.put("NetworkConnection:NetworkConnection" +i +":Mix" +i +"<->Mix" +(i+1), new NetworkConnection(mixes.get("Mix:Mix" +i), mixes.get("Mix:Mix" +(i+1)), Simulator.getSimulator()));
		}
		for (int i=0; i<clients.length; i++) {
			this.clients.put("Client:Client" +i, clients[i]);
			this.networkConnections.put("NetworkConnection:NetworkConnection:Client"+i +"<->Mix1", new NetworkConnection(clients[i], mixes.get("Mix:Mix1"), Simulator.getSimulator()));
			if (clients[i].getDelayBox() == null)
				clients[i].setDelayBox(DelayBox.getInstance(TypeOfNode.CLIENT));
		}
	}
	


	@Override
	public HashMap<String, AbstractClient> getClients() {
		return this.clients;
	}
	
	
	@Override
	public HashMap<String, Mix> getMixes() {
		return this.mixes;
	}

	
	@Override
	public DistantProxy getDistantProxy() {
		return this.distantProxy;
	}

	
	@Override
	public HashMap<String, NetworkConnection> getNetworkConnections() {
		return this.networkConnections;
	}
	
}

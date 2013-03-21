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

import java.util.HashMap;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.DistantProxy;
import evaluation.simulator.core.networkComponent.Mix;
import evaluation.simulator.core.networkComponent.NetworkConnection;
import evaluation.simulator.plugins.topology.NMixCascadeTopology;
import evaluation.simulator.plugins.topology.NoMixTopology;
import evaluation.simulator.plugins.topology.TopologyScript;


public enum Topology {

	NO_MIXES(new NoMixTopology(), false),
	ONE_MIX(new NMixCascadeTopology(1), true),
	THREE_MIX_CASCADE(new NMixCascadeTopology(3), true),
	FIVE_MIX_CASCADE(new NMixCascadeTopology(5), true),
	;
	

	private TopologyScript topologyScript;
	private boolean containsAtLeastOneMix;
	
	
	private Topology(TopologyScript topologyScript, boolean containsAtLeastOneMix) {
		this.topologyScript = topologyScript;
		this.containsAtLeastOneMix = containsAtLeastOneMix;
	}
	

	public void constructor(AbstractClient[] clients) {
		this.topologyScript.constructor(clients);
	}
	
	
	public HashMap<String, AbstractClient> getClients() {
		return this.topologyScript.getClients();
	}
	
	
	public HashMap<String, Mix> getMixes() {
		return this.topologyScript.getMixes();
	}
	
	
	public DistantProxy getDistantProxy() {
		return this.topologyScript.getDistantProxy();
	}
	
	
	public HashMap<String, NetworkConnection> getNetworkConnections() {
		return this.topologyScript.getNetworkConnections();
	}
	
	
	public boolean containsAtLeastOneMix() {
		return this.containsAtLeastOneMix;
	}

	
	public static Topology getTopology() {
		String desiredTopology = Simulator.settings.getProperty("TOPOLOGY_SCRIPT");
		for (Topology t:Topology.values())
			if (t.name().equalsIgnoreCase(desiredTopology))
				return t;
		throw new RuntimeException("ERROR: no Topology with the name \"" +desiredTopology  
				+"\" available (Key \"TOPOLOGY_SCRIPT\" in experiment config file. See " +
				"\"Topology.java\" for available Topologies.");
	}
	
}

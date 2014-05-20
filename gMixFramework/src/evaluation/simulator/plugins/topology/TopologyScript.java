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
package evaluation.simulator.plugins.topology;

import java.util.HashMap;

import evaluation.simulator.annotations.plugin.PluginSuperclass;
import evaluation.simulator.annotations.property.IntSimulationProperty;
import evaluation.simulator.annotations.property.StringSimulationProperty;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.DistantProxy;
import evaluation.simulator.core.networkComponent.Mix;
import evaluation.simulator.core.networkComponent.NetworkConnection;

@PluginSuperclass( 
		layerName = "Topology", 
		layerKey = "TOPOLOGY_SCRIPT",
		fakePlugins = "NO_MIXES:No mixes,ONE_MIX:One mix,THREE_MIX_CASCADE:Three mix cascade,FIVE_MIX_CASCADE:Five mix cascade", 
		position = 6)
		// isStatic = true) // TODO: Implement is static functions
public abstract class TopologyScript {
	
	public abstract void constructor(AbstractClient[] clients);
	
	public abstract HashMap<String, AbstractClient> getClients();
	
	public abstract HashMap<String, Mix> getMixes();
	
	public abstract DistantProxy getDistantProxy();
	
	public abstract HashMap<String, NetworkConnection> getNetworkConnections();
	
}

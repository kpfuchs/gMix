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
package userGeneratedContent.simulatorPlugIns.pluginRegistry;

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.annotations.plugin.Plugin;
import staticContent.evaluation.simulator.annotations.property.IntSimulationProperty;
import userGeneratedContent.simulatorPlugIns.plugins.delayBox.BasicDelayBox;
import userGeneratedContent.simulatorPlugIns.plugins.delayBox.DelayBoxImpl;
import userGeneratedContent.simulatorPlugIns.plugins.delayBox.NoDelayDelayBox;

@Plugin(pluginKey = "BASIC_DELAY_BOX", pluginName = "Basic delay", pluginLayerKey="TYPE_OF_DELAY_BOX")
public enum DelayBox {

	NO_DELAY_BOX,
	BASIC_DELAY_BOX
	;
		
	public static enum TypeOfNode {CLIENT, MIX, DISTANT_PROXY};
	public static int UNLIMITD_BANDWIDTH = -1;
	
	@IntSimulationProperty( name="Client send bandwidth (mbit/s)", 
			key="BASIC_DELAY_BOX_DEFAULT_CLIENT_BANDWIDTH_SEND", 
			enableUnlimited = true,
			min = 0)
	private static int bandwidthSend_client;
	
	@IntSimulationProperty( name="Client receive bandwidth (mbit/s)", 
			key="BASIC_DELAY_BOX_DEFAULT_CLIENT_BANDWIDTH_RECEIVE", 
			enableUnlimited = true,
			min = 0)
	private static int bandwidthReceive_client;
	
	@IntSimulationProperty( name="Client latency (ms)", 
			key="BASIC_DELAY_BOX_DEFAULT_CLIENT_LATENCY",
			min = 0)
	private static int latency_client;
	
	@IntSimulationProperty( name="Mix send bandwidth (mbit/s)", 
			key="BASIC_DELAY_BOX_DEFAULT_MIX_BANDWIDTH_SEND", 
			enableUnlimited = true,
			min = 0)
	private static int bandwidthSend_mix;
	
	@IntSimulationProperty( name="Mix receive banwidth (mbit/s)", 
			key="BASIC_DELAY_BOX_DEFAULT_MIX_BANDWIDTH_RECEIVE",
			enableUnlimited = true,
			min =0)
	private static int bandwidthReceive_mix;
	
	@IntSimulationProperty( name="Mix latency (ms)", 
			key="BASIC_DELAY_BOX_DEFAULT_MIX_LATENCY",
			min = 0)
	private static int latency_mix;
	
	@IntSimulationProperty( name="Distant proxy send bandwidth (mbit/s)", 
			key="BASIC_DELAY_BOX_DEFAULT_DISTANT_PROXY_BANDWIDTH_SEND",
			enableUnlimited = true,
			min = 0)
	private static int bandwidthSend_proxy;
	
	@IntSimulationProperty( name="Distant proxy receive bandwidth (mbit/s)", 
			key="BASIC_DELAY_BOX_DEFAULT_DISTANT_PROXY_BANDWIDTH_RECEIVE",
			enableUnlimited = true,
			min = 0)
	private static int bandwidthReceive_proxy;
	
	@IntSimulationProperty( name="Distant proxy latency (ms)", 
			key="BASIC_DELAY_BOX_DEFAULT_DISTANT_PROXY_LATENCY",
			min = 0)
	private static int latency_proxy;
		
	public static DelayBoxImpl getInstance(int bandwidthSend, int bandwidthReceive, int latency) {
		String desiredImpl = Simulator.settings.getProperty("TYPE_OF_DELAY_BOX");
		if (desiredImpl.equals("BASIC_DELAY_BOX"))
			return new BasicDelayBox(bandwidthSend, bandwidthReceive, latency);
		else if (desiredImpl.equals("NO_DELAY_BOX"))
			return new NoDelayDelayBox();
		else
			throw new RuntimeException("ERROR: no DelayBox with the name \"" +desiredImpl  
				+"\" available (Key \"TYPE_OF_DELAY_BOX\" in experiment config file. See " +
				"\"DelayBox.java\" for available DelayBoxes.");
	}
		
		
	public static DelayBoxImpl getInstance(TypeOfNode typeOfNode) {
		String desiredImpl = Simulator.settings.getProperty("TYPE_OF_DELAY_BOX");
		if (desiredImpl.equals("NO_DELAY_BOX")) {
			return new NoDelayDelayBox();
		} else if (desiredImpl.equals("BASIC_DELAY_BOX")) {
//			int bandwidthSend;
//			int bandwidthReceive;
//			int latency;
			if (typeOfNode == TypeOfNode.CLIENT) {
				if (Simulator.settings.getProperty("BASIC_DELAY_BOX_DEFAULT_CLIENT_BANDWIDTH_SEND").equals("UNLIMITED"))
					bandwidthSend_client = UNLIMITD_BANDWIDTH;
				else
					bandwidthSend_client = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_CLIENT_BANDWIDTH_SEND");
				if (Simulator.settings.getProperty("BASIC_DELAY_BOX_DEFAULT_CLIENT_BANDWIDTH_RECEIVE").equals("UNLIMITED"))
					bandwidthReceive_client = UNLIMITD_BANDWIDTH;
				else
					bandwidthReceive_client = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_CLIENT_BANDWIDTH_RECEIVE");
				latency_client = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_CLIENT_LATENCY");
				return new BasicDelayBox(bandwidthSend_client, bandwidthReceive_client, latency_client);
			} else if (typeOfNode == TypeOfNode.MIX) {
				if (Simulator.settings.getProperty("BASIC_DELAY_BOX_DEFAULT_MIX_BANDWIDTH_SEND").equals("UNLIMITED"))
					bandwidthSend_mix = UNLIMITD_BANDWIDTH;
				else
					bandwidthSend_mix = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_MIX_BANDWIDTH_SEND");
				if (Simulator.settings.getProperty("BASIC_DELAY_BOX_DEFAULT_MIX_BANDWIDTH_RECEIVE").equals("UNLIMITED"))
					bandwidthReceive_mix = UNLIMITD_BANDWIDTH;
				else
					bandwidthReceive_mix = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_MIX_BANDWIDTH_RECEIVE");
				latency_mix = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_MIX_LATENCY");
				return new BasicDelayBox(bandwidthSend_mix, bandwidthReceive_mix, latency_mix);
			} else if (typeOfNode == TypeOfNode.DISTANT_PROXY) {
				if (Simulator.settings.getProperty("BASIC_DELAY_BOX_DEFAULT_DISTANT_PROXY_BANDWIDTH_SEND").equals("UNLIMITED"))
					bandwidthSend_proxy = UNLIMITD_BANDWIDTH;
				else
					bandwidthSend_proxy = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_DISTANT_PROXY_BANDWIDTH_SEND");
				if (Simulator.settings.getProperty("BASIC_DELAY_BOX_DEFAULT_DISTANT_PROXY_BANDWIDTH_RECEIVE").equals("UNLIMITED"))
					bandwidthReceive_proxy = UNLIMITD_BANDWIDTH;
				else
					bandwidthReceive_proxy = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_DISTANT_PROXY_BANDWIDTH_RECEIVE");
				latency_proxy = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_DISTANT_PROXY_LATENCY");
				return new BasicDelayBox(bandwidthSend_proxy, bandwidthReceive_proxy, latency_proxy);
			} else {
				new InternalError("add new case for TypeOfNode " +typeOfNode);
				return null;
			}
//			return new BasicDelayBox(bandwidthSend, bandwidthReceive, latency);
		} else
			throw new RuntimeException("ERROR: no DelayBox with the name \"" +desiredImpl  
				+"\" available (Key \"TYPE_OF_DELAY_BOX\" in experiment config file. See " +
				"\"DelayBox.java\" for available DelayBoxes.");
	}
		
		
	public static DelayBoxImpl getInstance() {
		return new NoDelayDelayBox();
	}
		
}
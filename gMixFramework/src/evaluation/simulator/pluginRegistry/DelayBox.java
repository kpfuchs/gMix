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
import evaluation.simulator.plugins.delayBox.BasicDelayBox;
import evaluation.simulator.plugins.delayBox.DelayBoxImpl;
import evaluation.simulator.plugins.delayBox.NoDelayDelayBox;


public enum DelayBox {

	NO_DELAY,
	BASIC_DELAY_BOX
	;
		
	public static enum TypeOfNode {CLIENT, MIX, DISTANT_PROXY};
	public static int UNLIMITD_BANDWIDTH = -1;
		
		
	public static DelayBoxImpl getInstance(int bandwidthSend, int bandwidthReceive, int latency) {
		String desiredImpl = Simulator.settings.getProperty("TYPE_OF_DELAY_BOX");
		if (desiredImpl.equals("BASIC_DELAY_BOX"))
			return new BasicDelayBox(bandwidthSend, bandwidthReceive, latency);
		else if (desiredImpl.equals("NO_DELAY"))
			return new NoDelayDelayBox();
		else
			throw new RuntimeException("ERROR: no DelayBox with the name \"" +desiredImpl  
				+"\" available (Key \"TYPE_OF_DELAY_BOX\" in experiment config file. See " +
				"\"DelayBox.java\" for available DelayBoxes.");
	}
		
		
	public static DelayBoxImpl getInstance(TypeOfNode typeOfNode) {
		String desiredImpl = Simulator.settings.getProperty("TYPE_OF_DELAY_BOX");
		if (desiredImpl.equals("NO_DELAY")) {
			return new NoDelayDelayBox();
		} else if (desiredImpl.equals("BASIC_DELAY_BOX")) {
			int bandwidthSend;
			int bandwidthReceive;
			int latency;
			if (typeOfNode == TypeOfNode.CLIENT) {
				if (Simulator.settings.getProperty("BASIC_DELAY_BOX_DEFAULT_CLIENT_BANDWIDTH_SEND").equals("UNLIMITED"))
					bandwidthSend = UNLIMITD_BANDWIDTH;
				else
					bandwidthSend = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_CLIENT_BANDWIDTH_SEND");
				if (Simulator.settings.getProperty("BASIC_DELAY_BOX_DEFAULT_CLIENT_BANDWIDTH_RECEIVE").equals("UNLIMITED"))
					bandwidthReceive = UNLIMITD_BANDWIDTH;
				else
					bandwidthReceive = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_CLIENT_BANDWIDTH_RECEIVE");
				latency = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_CLIENT_LATENCY");
			} else if (typeOfNode == TypeOfNode.MIX) {
				if (Simulator.settings.getProperty("BASIC_DELAY_BOX_DEFAULT_MIX_BANDWIDTH_SEND").equals("UNLIMITED"))
					bandwidthSend = UNLIMITD_BANDWIDTH;
				else
					bandwidthSend = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_MIX_BANDWIDTH_SEND");
				if (Simulator.settings.getProperty("BASIC_DELAY_BOX_DEFAULT_MIX_BANDWIDTH_RECEIVE").equals("UNLIMITED"))
					bandwidthReceive = UNLIMITD_BANDWIDTH;
				else
					bandwidthReceive = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_MIX_BANDWIDTH_RECEIVE");
				latency = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_MIX_LATENCY");
			} else if (typeOfNode == TypeOfNode.DISTANT_PROXY) {
				if (Simulator.settings.getProperty("BASIC_DELAY_BOX_DEFAULT_DISTANT_PROXY_BANDWIDTH_SEND").equals("UNLIMITED"))
					bandwidthSend = UNLIMITD_BANDWIDTH;
				else
					bandwidthSend = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_DISTANT_PROXY_BANDWIDTH_SEND");
				if (Simulator.settings.getProperty("BASIC_DELAY_BOX_DEFAULT_DISTANT_PROXY_BANDWIDTH_RECEIVE").equals("UNLIMITED"))
					bandwidthReceive = UNLIMITD_BANDWIDTH;
				else
					bandwidthReceive = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_DISTANT_PROXY_BANDWIDTH_RECEIVE");
				latency = Simulator.settings.getPropertyAsInt("BASIC_DELAY_BOX_DEFAULT_DISTANT_PROXY_LATENCY");
			} else {
				new InternalError("add new case for TypeOfNode " +typeOfNode);
				return null;
			}
			return new BasicDelayBox(bandwidthSend, bandwidthReceive, latency);
		} else
			throw new RuntimeException("ERROR: no DelayBox with the name \"" +desiredImpl  
				+"\" available (Key \"TYPE_OF_DELAY_BOX\" in experiment config file. See " +
				"\"DelayBox.java\" for available DelayBoxes.");
	}
		
		
	public static DelayBoxImpl getInstance() {
		return new NoDelayDelayBox();
	}
		
}
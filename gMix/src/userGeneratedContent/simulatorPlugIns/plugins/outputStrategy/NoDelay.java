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
package userGeneratedContent.simulatorPlugIns.plugins.outputStrategy;


import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.annotations.plugin.Plugin;
import staticContent.evaluation.simulator.core.message.MixMessage;
import staticContent.evaluation.simulator.core.networkComponent.AbstractClient;
import staticContent.evaluation.simulator.core.networkComponent.Mix;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.ClientSendStyle;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.MixSendStyle;
import userGeneratedContent.simulatorPlugIns.plugins.clientSendStyle.ClientSendStyleImpl;
import userGeneratedContent.simulatorPlugIns.plugins.mixSendStyle.MixSendStyleImpl;

// TODO: Sometimes this plugin is not recognized

// forwards messages without delay (use for base line measurments)
@Plugin(pluginKey = "NO_DELAY", pluginName = "No Delay")
public class NoDelay extends OutputStrategyImpl {

	
	public NoDelay (Mix mix, Simulator simulator) {
		super(mix, simulator);
	}


	@Override
	public void incomingRequest(MixMessage mixMessage) {
		mix.putOutRequest(mixMessage);
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		mix.putOutReply(mixMessage);
	}


	@Override
	public ClientSendStyleImpl getClientSendStyle(AbstractClient client) {
		return ClientSendStyle.getInstance(client);
	}


	@Override
	public MixSendStyleImpl getMixSendStyle() {
		return MixSendStyle.getInstance(mix, mix);
	}

}

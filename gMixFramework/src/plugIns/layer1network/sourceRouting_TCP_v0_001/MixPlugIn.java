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
package plugIns.layer1network.sourceRouting_TCP_v0_001;

import java.util.Vector;

import framework.core.controller.Implementation;
import framework.core.controller.LocalClassLoader;
import framework.core.controller.SubImplementation;
import framework.core.interfaces.Layer1NetworkMix;


public class MixPlugIn extends Implementation implements Layer1NetworkMix {
	
	public SubImplementation clientComHandler;
	public SubImplementation nextMixHandler;
	public SubImplementation prevMixHandler;
	private Vector<SubImplementation> implementations = new Vector<SubImplementation>();
	
	
	@Override
	public void constructor() {
		if (anonNode.NUMBER_OF_MIXES > 1) {
			instantiatePrevMixComHandler();
			instantiateNextMixComHandler();
		}
		instantiateClientComHandler();
		if (anonNode.NUMBER_OF_MIXES > 1)
			((ClientReplyReceiver)clientComHandler).setClientReplyProvider((ClientReplyProvider)prevMixHandler);
		for (SubImplementation impl: implementations)
			impl.constructor();
	}

	
	@Override
	public void initialize() {
		for (SubImplementation impl: implementations)
			impl.initialize();
	}

	
	@Override
	public void begin() {
		for (SubImplementation impl: implementations) 
			impl.begin();
	}

	
	private void instantiateClientComHandler() {
		this.clientComHandler = LocalClassLoader.instantiateSubImplementation(
				"plugIns.layer1network." +settings.getProperty("LAYER_1_PLUG-IN_MIX"), 
				settings.getProperty("CLIENT_CONNECTION_HANDLER"),
				anonNode
				);
		implementations.add(clientComHandler);
	}
	
	
	private void instantiateNextMixComHandler() {
		this.nextMixHandler = LocalClassLoader.instantiateSubImplementation(
				"plugIns.layer1network." +settings.getProperty("LAYER_1_PLUG-IN_MIX"), 
				settings.getProperty("NEXT_MIX_CONNECTION_HANLDER"),
				anonNode
				);
		implementations.add(nextMixHandler);
	}
	

	private void instantiatePrevMixComHandler() {
		this.prevMixHandler = LocalClassLoader.instantiateSubImplementation(
				"plugIns.layer1network." +settings.getProperty("LAYER_1_PLUG-IN_MIX"), 
				settings.getProperty("PREVIOUS_MIX_CONNECTION_HANLDER"),
				anonNode
				); 
		implementations.add(prevMixHandler);
	}
	
}

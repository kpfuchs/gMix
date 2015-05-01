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
package staticContent.framework.controller;

import staticContent.framework.AnonNode;
import staticContent.framework.clock.Clock;
import staticContent.framework.config.Settings;
import staticContent.framework.infoService.InfoServiceClient;
import staticContent.framework.interfaces.Layer3OutputStrategyMix;
import staticContent.framework.message.Reply;
import staticContent.framework.message.Request;
import staticContent.framework.userDatabase.User;
import staticContent.framework.userDatabase.UserDatabase;


public class Layer3OutputStrategyMixController extends Controller implements Layer3OutputStrategyMix {

	private Layer3OutputStrategyMix implementation;
	
	
	public Layer3OutputStrategyMixController(AnonNode anonNode, Settings settings,
			UserDatabase userDatabase, Clock clock,
			InfoServiceClient infoService) {
		super(anonNode, settings, userDatabase, clock, infoService);
	}

	
	@Override
	public void instantiateSubclass() {
		this.implementation = LocalClassLoader.instantiateImplementation(
				"userGeneratedContent.testbedPlugIns.layerPlugIns.layer3outputStrategy." +settings.getProperty("LAYER_3_PLUG-IN_MIX"), 
				"MixPlugIn.java",
				this,
				Layer3OutputStrategyMix.class
				);
	}


	@Override
	public void addRequest(Request request) {
		this.implementation.addRequest(request);
	}


	@Override
	public void addReply(Reply reply) {
		this.implementation.addReply(reply);
	}


	@Override
	public int getMaxSizeOfNextWrite() {
		return implementation.getMaxSizeOfNextWrite();
	}


	@Override
	public int getMaxSizeOfNextRead() {
		return implementation.getMaxSizeOfNextRead();
	}

	
	@Override
	public void write(User user, byte[] data) {
		if (data.length > getMaxSizeOfNextWrite())
			throw new RuntimeException("use getMaxSizeOfNextReply() to determine the maximum size of a reply..."); 
		implementation.write(user, data);
	}

}
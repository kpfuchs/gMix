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
import staticContent.framework.interfaces.Layer2RecodingSchemeMix;
import staticContent.framework.message.Reply;
import staticContent.framework.message.Request;
import staticContent.framework.userDatabase.User;
import staticContent.framework.userDatabase.UserDatabase;


public class Layer2RecodingSchemeMixController extends Controller implements Layer2RecodingSchemeMix {

	private Layer2RecodingSchemeMix implementation;
	
	
	public Layer2RecodingSchemeMixController(AnonNode anonNode, Settings settings,
			UserDatabase userDatabase, Clock clock,
			InfoServiceClient infoService) {
		super(anonNode, settings, userDatabase, clock, infoService);
	}

	
	@Override
	public void instantiateSubclass() {
		this.implementation = LocalClassLoader.instantiateImplementation(
				"userGeneratedContent.testbedPlugIns.layerPlugIns.layer2recodingScheme." +settings.getProperty("LAYER_2_PLUG-IN_MIX"), 
				"MixPlugIn.java",
				this,
				Layer2RecodingSchemeMix.class
				);
	}


	@Override
	public int getMaxSizeOfNextReply() {
		return this.implementation.getMaxSizeOfNextReply();
	}


	@Override
	public int getMaxSizeOfNextRequest() {
		return this.implementation.getMaxSizeOfNextRequest();
	}


	@Override
	public Request generateDummy(int[] route, User user) {
		return this.implementation.generateDummy(route, user);
	}


	@Override
	public Request generateDummy(User user) {
		return this.implementation.generateDummy(user);
	}


	@Override
	public Reply generateDummyReply(int[] route, User user) {
		return this.implementation.generateDummyReply(route, user);
	}


	@Override
	public Reply generateDummyReply(User user) {
		return this.implementation.generateDummyReply(user);
	}

}
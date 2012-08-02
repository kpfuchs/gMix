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
package framework.core.controller;

import framework.core.AnonNode;
import framework.core.clock.Clock;
import framework.core.config.Settings;
import framework.core.interfaces.Layer3OutputStrategyMix;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.userDatabase.UserDatabase;
import framework.infoService.InfoServiceClient;


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
				"plugIns.layer3outputStrategy." +settings.getProperty("LAYER_3_PLUG-IN_MIX"), 
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
	public int getMaxSizeOfNextReply() {
		return implementation.getMaxSizeOfNextReply();
	}


	@Override
	public int getMaxSizeOfNextRequest() {
		return implementation.getMaxSizeOfNextRequest();
	}

}
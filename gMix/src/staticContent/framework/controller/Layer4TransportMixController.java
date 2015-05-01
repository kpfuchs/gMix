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
import staticContent.framework.interfaces.Layer4TransportMix;
import staticContent.framework.message.Reply;
import staticContent.framework.message.Request;
import staticContent.framework.userDatabase.User;
import staticContent.framework.userDatabase.UserDatabase;


public class Layer4TransportMixController extends Controller implements Layer4TransportMix {

	
	private Layer4TransportMix implementation;
	
	
	public Layer4TransportMixController(AnonNode anonNode, Settings settings,
			UserDatabase userDatabase, Clock clock,
			InfoServiceClient infoService) {
		super(anonNode, settings, userDatabase, clock, infoService);
	}

	
	@Override
	public void instantiateSubclass() {
		this.implementation = LocalClassLoader.instantiateImplementation(
				"userGeneratedContent.testbedPlugIns.layerPlugIns.layer4transport." +settings.getProperty("LAYER_4_PLUG-IN_MIX"), 
				"MixPlugIn.java",
				this,
				Layer4TransportMix.class
				);
		settings.addProperties(settings.getProperty("LAYER_4_PLUG-IN_MIX_PATH") 
				+settings.getProperty("LAYER_4_PLUG-IN_MIX") 
				+"/PlugInSettings.txt");
	}


	@Override
	public void forwardRequest(Request request) {
		this.implementation.forwardRequest(request);
	}


	@Override
	public void write(User user, byte[] data) {
		this.implementation.write(user, data);
	}
	

	@Override
	public Reply addLayer4Header(Reply reply) {
		return this.implementation.addLayer4Header(reply);
	}


	@Override
	public int getSizeOfLayer4Header() {
		return this.implementation.getSizeOfLayer4Header();
	}


	@Override
	public int getMaxSizeOfNextWrite() {
		return this.implementation.getMaxSizeOfNextWrite();
	}


	@Override
	public int getMaxSizeOfNextRead() {
		return this.implementation.getMaxSizeOfNextRead();
	}


}
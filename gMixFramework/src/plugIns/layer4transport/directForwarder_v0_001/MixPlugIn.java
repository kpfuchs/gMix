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
package plugIns.layer4transport.directForwarder_v0_001;

import framework.core.controller.Implementation;
import framework.core.controller.Layer3OutputStrategyMixController;
import framework.core.interfaces.Layer4TransportMix;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.userDatabase.User;


public class MixPlugIn extends Implementation implements Layer4TransportMix {

	private Layer3OutputStrategyMixController layer3controller;
	
	
	@Override
	public void constructor() {
		System.out.println("loaded " +this.getClass().getCanonicalName()); 
		this.layer3controller = anonNode.getOutputStrategyLayerControllerMix();
	}

	
	@Override
	public void initialize() {
		
	}

	
	@Override
	public void begin() {
		
	}

	
	@Override
	public void forwardRequest(Request request) {
		anonNode.forwardToLayer5(request);
	}


	@Override
	public void write(User user, byte[] data) {
		anonNode.write(user, data);
	}


	@Override
	public Reply addLayer4Header(Reply reply) {
		return reply;
	}


	@Override
	public int getSizeOfLayer4Header() {
		return 0;
	}


	@Override
	public int getMaxSizeOfNextWrite() {
		return layer3controller.getMaxSizeOfNextWrite() - getSizeOfLayer4Header();
	}


	@Override
	public int getMaxSizeOfNextRead() {
		return layer3controller.getMaxSizeOfNextRead() - getSizeOfLayer4Header();
	}

}

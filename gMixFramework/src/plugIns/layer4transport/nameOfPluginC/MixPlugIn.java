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
package plugIns.layer4transport.nameOfPluginC;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer4TransportMix;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.userDatabase.User;


public class MixPlugIn extends Implementation implements Layer4TransportMix {

	
	@Override
	public void constructor() {
		// TODO Auto-generated method stub
		System.out.println("loaded " +this.getClass().getCanonicalName()); 
		
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void begin() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void forwardRequest(Request request) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void write(User user, byte[] data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Reply addLayer4Header(Reply reply) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getSizeOfLayer4Header() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxSizeOfNextWrite() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxSizeOfNextRead() {
		// TODO Auto-generated method stub
		return 0;
	}
	
}

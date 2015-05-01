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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer3outputStrategy.nameOfPluginA;

import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer1NetworkClient;
import staticContent.framework.interfaces.Layer2RecodingSchemeClient;
import staticContent.framework.interfaces.Layer3OutputStrategyClient;
import staticContent.framework.interfaces.Layer4TransportClient;


public class ClientPlugIn extends Implementation implements Layer3OutputStrategyClient {

	
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
	public void connect() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void connect(int destPseudonym) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public int getMaxSizeOfNextWrite() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public int getMaxSizeOfNextReceive() {
		// TODO Auto-generated method stub
		return 0;
	}



	@Override
	public int availableReplies() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public int availableReplyData() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public void setReferences(Layer1NetworkClient layer1,
			Layer2RecodingSchemeClient layer2,
			Layer3OutputStrategyClient layer3, Layer4TransportClient layer4) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public byte[] receive() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void write(byte[] data) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void write(byte[] data, int destPseudonym) {
		// TODO Auto-generated method stub
		
	}

}

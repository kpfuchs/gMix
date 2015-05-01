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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer4transport.directForwarder_v0_001;

import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer1NetworkClient;
import staticContent.framework.interfaces.Layer2RecodingSchemeClient;
import staticContent.framework.interfaces.Layer3OutputStrategyClient;
import staticContent.framework.interfaces.Layer4TransportClient;


public class ClientPlugIn extends Implementation implements Layer4TransportClient {

	
	private Layer3OutputStrategyClient layer3;
	
	
	@Override
	public void constructor() {
		
	}


	@Override
	public void initialize() {
		
	}


	@Override
	public void begin() {
		
	}


	@Override
	public void setReferences(Layer1NetworkClient layer1,
			Layer2RecodingSchemeClient layer2,
			Layer3OutputStrategyClient layer3, 
			Layer4TransportClient layer4) {
		assert layer4 == this;
		this.layer3 = layer3;
	}


	@Override
	public void connect() {
		layer3.connect();
	}


	@Override
	public void connect(int destPseudonym) {
		layer3.connect(destPseudonym);
	}


	@Override
	public void disconnect() {
		layer3.disconnect();
	}


	@Override
	public int getMaxSizeOfNextWrite() {
		return layer3.getMaxSizeOfNextWrite();
	}


	@Override
	public int getMaxSizeOfNextReply() {
		return layer3.getMaxSizeOfNextReceive();
	}


	@Override
	public int availableReplies() {
		return layer3.availableReplies();
	}


	@Override
	public int availableReplyPayload() {
		return layer3.availableReplyData();
	}


	@Override
	public byte[] receive() {
		return layer3.receive();
	}


	@Override
	public void write(byte[] data) {
		layer3.write(data);
	}


	@Override
	public void write(byte[] data, int destPseudonym) {
		layer3.write(data, destPseudonym);
	}

}

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
package plugIns.layer1network.nameOfPluginA;

import staticFunctions.layer1network.nameOfStaticFunctionA.SomeClassUsefulForSeveralPlugIns;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer1NetworkClient;
import framework.core.interfaces.Layer2RecodingSchemeClient;
import framework.core.interfaces.Layer3OutputStrategyClient;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.MixList;


public class ClientPlugIn extends Implementation implements Layer1NetworkClient {

	
	@Override
	public void constructor() {
		// TODO Auto-generated method stub
		System.out.println("loaded " +this.getClass().getCanonicalName()); 
		
		System.out.println("Test:");
		System.out.println("settings: " +settings); 
		System.out.println("anonNode: " +anonNode);
		System.out.println("infoService: " +infoService);
		System.out.println("nw-layer controller: " +networkLayerClient); 
		
		// common class example
		new SomeClassUsefulForMixAndClient().exampleDoSth();
		
		// static function example
		new SomeClassUsefulForSeveralPlugIns().exampleDoSth();
	}


	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		System.out.println("nw-layer client plug-in: " +networkLayerClient.getImplementation()); 
	}


	@Override
	public void setReferences(Layer1NetworkClient layer1,
			Layer2RecodingSchemeClient layer2, Layer3OutputStrategyClient layer3) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void begin() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void sendMessage(Request request) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void connect() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Reply receiveReply() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public int availableReplies() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public void connect(MixList route) {
		// TODO Auto-generated method stub
		
	}


}

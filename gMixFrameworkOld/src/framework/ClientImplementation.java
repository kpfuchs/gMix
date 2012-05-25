/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2011  Karl-Peter Fuchs
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

package framework;

import infoService.InfoServiceClient_v1;
import client.ClientController;
import client.MixSocket;
import client.applicationTunnel.ApplicationTunnelClient;
import client.communicationBehaviour.ClientCommunicationBehaviour;
import client.communicationBehaviour.RecodingSchemeWrapper;
import client.mixCommunicationHandler.MixCommunicationHandler;


public abstract class ClientImplementation implements ClientComponentReferences, ThreePhaseStart {

	protected ClientController client;
	protected ApplicationTunnelClient applicationTunnel;
	protected MixCommunicationHandler mixCommunicationHandler; // abstracts from the communication protocol between mix and client
	protected ClientCommunicationBehaviour communicationBehaviour;
	protected RecodingSchemeWrapper recodingScheme;
	protected InfoServiceClient_v1 infoService;
	protected Settings settings;
	
	
	//showing the actual implementation time of all the components...
	static {
		//System.out.println("client: " +ClientImplementation.class.getSimpleName());
	}

	
	public ClientController getClient() {
		return this.client;
	}
	
	
	public Settings getSettings() {
		return this.settings;
	}
	
	
	public void setController(ClientController client) {
		
		assert this.client == null : "client already set";
		assert client != null;
		this.client = client;
		this.settings = client.getSettings();
		client.registerClientImplementation(this);

	}
	
	
	public void setReferences(ClientController client) {
		this.applicationTunnel = client.getApplicationTunnelClient();
		this.mixCommunicationHandler = client.getMixCommunicationHandler();
		this.communicationBehaviour = client.getClientCommunicationBehaviour();
		this.applicationTunnel = client.getApplicationTunnelClient();
		this.settings = client.getSettings();
		this.infoService = client.getInfoServiceClient();
		constructor();

	}
	
	
	public String getBinaryName() {
		return this.getClass().getName();
	}


	@Override
	public ApplicationTunnelClient getApplicationTunnelClient() {
		return this.applicationTunnel;
	}


	@Override
	public MixSocket getMixSocket() {
		return this.communicationBehaviour;
	}


	@Override
	public ClientCommunicationBehaviour getClientCommunicationBehaviour() {
		return this.communicationBehaviour;
	}


	@Override
	public MixCommunicationHandler getMixCommunicationHandler() {
		return this.mixCommunicationHandler;
	}


	@Override
	public RecodingSchemeWrapper getRecodingSchemeWrapper() {
		return this.recodingScheme;
	}
	
	
	@Override
	public InfoServiceClient_v1 getInfoServiceClient() {
		return this.infoService;
	}
}

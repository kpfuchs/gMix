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

package client;


import infoService.InfoServiceClient_v1;

import java.util.Vector;

import client.applicationTunnel.ApplicationTunnelClient;
import client.communicationBehaviour.ClientCommunicationBehaviour;
import client.communicationBehaviour.RecodingSchemeWrapper;
import client.mixCommunicationHandler.MixCommunicationHandler;

import framework.ClientComponentReferences;
import framework.ClientImplementation;
import framework.Implementation;
import framework.LocalClassLoader;
import framework.Logger;
import framework.Paths;
import framework.Settings;
import framework.Util;


public class ClientController implements ClientComponentReferences {
	
	private int identifier;
	private ApplicationTunnelClient applicationTunnel;
	private MixCommunicationHandler mixCommunicationHandler; // abstracts from the communication protocol between mix and client
	private ClientCommunicationBehaviour communicationBehaviour;
	private RecodingSchemeWrapper recodingScheme;
	private Settings settings;
	private InfoServiceClient_v1 infoService;
	private boolean duplex;
	protected Vector<ClientImplementation> clientImplementations;
	protected Vector<Implementation> implementations;
	
	
	public ClientController(Settings settings) {
		Logger.init();
		this.settings = settings;
		this.infoService = new InfoServiceClient_v1(settings.getPropertyAsInetAddress("INFO_SERVICE_ADDRESS"), settings.getPropertyAsInt("INFO_SERVICE_PORT"));
		Util.checkIfBCIsInstalled();
		this.identifier = infoService.registerAsClient();
		this.duplex = infoService.getIsDuplexModeOn();
		loadImplementations();
		setReferencesBetweenImplementations();
		validateImplementationComposition();
		callConstructors();
		initializeComponents();
		begin();
	}
	
	
	/**
	 * Creates a new <code>Mix</code>.
	 * 
	 * @param args	Not used.
	 */
	public static void main(String[] args) {
		new ClientController(new Settings(Paths.PATH_TO_GLOBAL_CONFIG));
	} 
	
	
	private void loadImplementations() {
		this.recodingScheme = LocalClassLoader.instantiateClientImplementation("client.communicationBehaviour", "RecodingSchemeWrapper.java", this, RecodingSchemeWrapper.class);
		this.applicationTunnel = LocalClassLoader.instantiateClientImplementation("client.applicationTunnel", settings.getProperty("APPLICATION_TUNNEL"), this, ApplicationTunnelClient.class);
		this.mixCommunicationHandler = LocalClassLoader.instantiateClientImplementation("client.mixCommunicationHandler", settings.getProperty("MIX_XOMMUNICATION_HANDLER"), this, MixCommunicationHandler.class);
		this.communicationBehaviour = LocalClassLoader.instantiateClientImplementation("client.communicationBehaviour", settings.getProperty("CLIENT_COMMUNICATION_BEHAVIOUR"), this, ClientCommunicationBehaviour.class);
	}
	
	
	private void setReferencesBetweenImplementations() {
		for (int i=0; i<clientImplementations.size(); i++)
			clientImplementations.get(i).setReferences(this);
	}

	private void callConstructors() {
		for (int i=0; i<clientImplementations.size(); i++)
			clientImplementations.get(i).constructor();
	}
	
	private void validateImplementationComposition() {
		return; // TODO
	}
	
	
	private void initializeComponents() {
		if (implementations != null)
			for (int i=0; i<implementations.size(); i++)
				implementations.get(i).initialize();
		for (int i=0; i<clientImplementations.size(); i++)
			clientImplementations.get(i).initialize();
	}
	
	
	
	private void begin() {
		if (implementations != null)
			for (int i=0; i<implementations.size(); i++)
				implementations.get(i).begin();
		for (int i=0; i<clientImplementations.size(); i++)
			clientImplementations.get(i).begin(); 
	}
	
	
	public Settings getSettings() {
		return this.settings;
	}
	

	@Override
	public ApplicationTunnelClient getApplicationTunnelClient() {
		return this.applicationTunnel;
	}
	

	@Override
	public ClientController getClient() {
		return this;
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
	public RecodingSchemeWrapper getRecodingSchemeWrapper() {
		return this.recodingScheme;
	}
	
	
	@Override
	public MixCommunicationHandler getMixCommunicationHandler() {
		return this.mixCommunicationHandler;
	}

	
	public int getIdentifier() {
		return this.identifier;
	}
	
	
	public void registerClientImplementation(ClientImplementation clientImplementation) {
		
		if (clientImplementations == null)
			clientImplementations = new Vector<ClientImplementation>();
		clientImplementations.add(clientImplementation);
		
	}

	
	public void registerImplementation(Implementation implementation) {
		
		if (implementations == null)
			implementations = new Vector<Implementation>();
		implementations.add(implementation);
		
	}
	

	public boolean isDuplex() {
		return duplex;
	}
	
	
	@Override
	public String toString() {
		return "client " +this.identifier;// +"(" +super.toString() +")";
	}


	@Override
	public InfoServiceClient_v1 getInfoServiceClient() {
		assert infoService != null;
		return infoService;
	}
	
}
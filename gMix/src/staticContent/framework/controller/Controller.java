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
import staticContent.framework.controller.routing.DynamicRoutingClientController;
import staticContent.framework.controller.routing.DynamicRoutingMixController;
import staticContent.framework.controller.routing.GlobalRoutingClientController;
import staticContent.framework.controller.routing.SourceRoutingClientController;
import staticContent.framework.infoService.InfoServiceClient;
import staticContent.framework.userDatabase.UserDatabase;


public abstract class Controller {
	
	// references to framework classes
	protected AnonNode anonNode;
	protected Settings settings;
	protected UserDatabase userDatabase;
	protected Clock clock;
	protected InfoServiceClient infoService;
	
	// references to other layers
	protected Layer1NetworkMixController networkLayerMix;
	protected Layer1NetworkClientController networkLayerClient;
	protected Layer2RecodingSchemeMixController recodingLayerMix;
	protected Layer2RecodingSchemeClientController recodingLayerClient;
	protected Layer3OutputStrategyMixController outputStrategyLayerMix;
	protected Layer3OutputStrategyClientController outputStrategyLayerClient;
	protected Layer4TransportMixController transportLayerMix;
	protected Layer4TransportClientController transportLayerClient;
	protected Layer5ApplicationMixController applicationLayerMix;
	protected Layer5ApplicationClientController applicationLayerClient;
	
	protected DynamicRoutingClientController dynamicRoutingPlugInClient;
	protected DynamicRoutingMixController dynamicRoutingPlugInMix;
	protected GlobalRoutingClientController globalRoutingPlugInClient;
	protected SourceRoutingClientController sourceRoutingPlugInClient;
	
	// implementation/plug-in that will be loaded with the class loader at runtime
	public Implementation implementation;
	
	
	public Controller(
			AnonNode anonNode, 
			Settings settings, 
			UserDatabase userDatabase, 
			Clock clock, 
			InfoServiceClient infoService
			) {
		
		this.anonNode = anonNode;
		this.settings = settings;
		this.userDatabase = userDatabase;
		this.clock = clock;
		this.infoService = infoService;
		//anonNode.registerComponent(this);
	}
	
	
	public void setComponentReferences(
			Layer1NetworkMixController networkLayerMix,
			Layer1NetworkClientController networkLayerClient,
			Layer2RecodingSchemeMixController recodingLayerMix,
			Layer2RecodingSchemeClientController recodingLayerClient,
			Layer3OutputStrategyMixController outputStrategyLayerMix,
			Layer3OutputStrategyClientController outputStrategyLayerClient,
			Layer4TransportMixController transportLayerMix,
			Layer4TransportClientController transportLayerClient,
			Layer5ApplicationMixController applicationLayerMix,
			Layer5ApplicationClientController applicationLayerClient,
			DynamicRoutingClientController dynamicRoutingPlugInClient,
			DynamicRoutingMixController dynamicRoutingPlugInMix,
			GlobalRoutingClientController globalRoutingPlugInClient,
			SourceRoutingClientController sourceRoutingPlugInClient
			) {
		
		this.networkLayerMix = networkLayerMix;
		this.networkLayerClient = networkLayerClient;
		this.recodingLayerMix = recodingLayerMix;
		this.recodingLayerClient = recodingLayerClient;
		this.outputStrategyLayerMix = outputStrategyLayerMix;
		this.outputStrategyLayerClient = outputStrategyLayerClient;
		this.transportLayerMix = transportLayerMix;
		this.transportLayerClient = transportLayerClient;
		this.applicationLayerMix = applicationLayerMix;
		this.applicationLayerClient = applicationLayerClient;
		this.dynamicRoutingPlugInClient = dynamicRoutingPlugInClient;
		this.dynamicRoutingPlugInMix = dynamicRoutingPlugInMix;
		this.globalRoutingPlugInClient = globalRoutingPlugInClient;
		this.sourceRoutingPlugInClient = sourceRoutingPlugInClient;
		
	}

	
	public void initialize() {
		if (implementation != null)
			this.implementation.callInitialize();
	}


	public void begin() {
		if (implementation != null)
			this.implementation.callBegin();
	}
	

	public void setImplementation(Implementation implementation) {
		if (anonNode != null && !anonNode.IS_CLIENT) // TODO
			assert this.implementation == null : "each controller may have only one implementation";
		assert implementation != null;
		this.implementation = implementation;
	}
	
	
	public Implementation getImplementation() {
		assert implementation != null;
		return implementation;
	}
	
	
	public abstract void instantiateSubclass();

}

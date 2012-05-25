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
import framework.core.interfaces.ThreePhaseStart;
import framework.core.userDatabase.UserDatabase;
import framework.infoService.InfoServiceClient;


public abstract class Implementation implements ThreePhaseStart {
	
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
	
	// reference on controller class of this implementation
	protected Controller controller;
	//protected Vector<SubImplementation> subImplementations;
	
	
	/*public void registerSubImplementation(SubImplementation subImplementation) {
		if (subImplementations == null)
			subImplementations = new Vector<SubImplementation>();
		subImplementations.add(subImplementation);
	}*/
	
	
	public void setController(Controller controller) {
		assert this.controller == null : "controller already set";
		assert controller != null;
		controller.setImplementation(this);
		this.controller = controller;
		
		this.anonNode = controller.anonNode;
		this.settings = controller.settings;
		this.userDatabase = controller.userDatabase;
		this.clock = controller.clock;
		this.infoService = controller.infoService;
		
		this.networkLayerMix = controller.networkLayerMix;
		this.networkLayerClient = controller.networkLayerClient;
		this.recodingLayerMix = controller.recodingLayerMix;
		this.recodingLayerClient = controller.recodingLayerClient;
		this.outputStrategyLayerMix = controller.outputStrategyLayerMix;
		this.outputStrategyLayerClient = controller.outputStrategyLayerClient;
		this.transportLayerMix = controller.transportLayerMix;
		this.transportLayerClient = controller.transportLayerClient;
		this.applicationLayerMix = controller.applicationLayerMix;
		this.applicationLayerClient = controller.applicationLayerClient;
	}
	
	
	protected void callInitialize() {
		initialize();
		/*if (subImplementations != null)
			for (SubImplementation si: subImplementations)
				si.initialize();*/
	}
	
	
	protected void callBegin() {
		begin();
		/*if (subImplementations != null)
			for (SubImplementation si: subImplementations)
				si.begin();*/
	}
	
}
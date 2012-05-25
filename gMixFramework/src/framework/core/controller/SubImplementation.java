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


public abstract class SubImplementation implements ThreePhaseStart {

	protected AnonNode anonNode;
	protected Settings settings;
	protected UserDatabase userDatabase;
	protected Clock clock;
	protected InfoServiceClient infoService;
	
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
	
	
	protected void setAnonNode(AnonNode anonNode) {
		this.anonNode = anonNode;
		this.settings = anonNode.getSettings();
		this.userDatabase = anonNode.getUserDatabase();
		this.clock = anonNode.getClock();
		this.infoService = anonNode.getInfoService();
		
		this.networkLayerMix = anonNode.getNetworkLayerControllerMix();
		this.networkLayerClient = anonNode.getNetworkLayerControllerClient();
		this.recodingLayerMix = anonNode.getRecodingLayerControllerMix();
		this.recodingLayerClient = anonNode.getRecodingLayerControllerClient();
		this.outputStrategyLayerMix = anonNode.getOutputStrategyLayerControllerMix();
		this.outputStrategyLayerClient = anonNode.getOutputStrategyLayerControllerClient();
		this.transportLayerMix = anonNode.getTransportLayerControllerMix();
		this.transportLayerClient = anonNode.getTransportLayerControllerClient();
		this.applicationLayerMix = anonNode.getApplicationLayerControllerMix();
		this.applicationLayerClient = anonNode.getApplicationLayerControllerClient();
	}
	
}

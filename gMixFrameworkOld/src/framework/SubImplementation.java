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
import inputOutputHandler.InputOutputHandlerController;
import java.util.logging.Logger;

import client.ClientController;
import networkClock.NetworkClockController;
import outputStrategy.OutputStrategyController;
import recodingScheme.RecodingSchemeController;
import userDatabase.UserDatabaseController;


public abstract class SubImplementation implements ComponentReferences, ThreePhaseStart {

	protected InputOutputHandlerController inputOutputHandler;
	protected OutputStrategyController outputStrategy;
	protected RecodingSchemeController recodingScheme;
	protected InfoServiceClient_v1 infoService;
	protected NetworkClockController networkClock;
	protected UserDatabaseController userDatabase;
	protected Mix mix;
	protected ClientController client;
	protected Logger logger;
	protected Settings settings;
	protected Controller controller;
	protected Implementation owner; 
	protected ClientImplementation ownerClient; 
	
	
	public void setOwner(Implementation owner) {
		
		assert this.owner == null : "owner already set";
		assert owner != null;
		
		owner.registerSubImplementation(this);
		
		this.owner = owner;
		this.controller = owner.controller;
		this.inputOutputHandler = controller.getInputOutputHandler();
		this.outputStrategy = controller.getOutputStrategy();
		this.recodingScheme = controller.getRecodingScheme();
		this.infoService = controller.getInfoService();
		this.networkClock = controller.getNetworkClock();
		this.userDatabase = controller.getUserDatabase();
		this.settings = controller.getSettings();
		this.mix = controller.getMix();
		
		constructor();
		
	}
	
	
	public void setOwner(ClientImplementation owner) {
		
		assert this.ownerClient == null : "owner already set";
		assert owner != null;
		this.ownerClient = owner;
		this.client = owner.getClient();
		assert client != null;
		this.settings = owner.getSettings();
		this.infoService = owner.getInfoServiceClient();
		//constructor();
		
	}

	
	@Override
	public InputOutputHandlerController getInputOutputHandler() {
		return this.inputOutputHandler;
	}

	@Override
	public OutputStrategyController getOutputStrategy() {
		return this.outputStrategy;
	}

	@Override
	public RecodingSchemeController getRecodingScheme() {
		return this.recodingScheme;
	}

	@Override
	public InfoServiceClient_v1 getInfoService() {
		return this.infoService;
	}

	@Override
	public NetworkClockController getNetworkClock() {
		return this.networkClock;
	}

	@Override
	public UserDatabaseController getUserDatabase() {
		return this.userDatabase;
	}
	

	@Override
	public Settings getSettings() {
		return this.settings;
	}

	@Override
	public Mix getMix() {
		return this.mix;
	}
	
	public Implementation getOwner() {
		return this.owner;
	}
	
	public Controller getController() {
		return this.controller;
	}
	
}

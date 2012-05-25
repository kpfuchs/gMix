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


import networkClock.NetworkClockController;
import infoService.InfoServiceClient_v1;
import inputOutputHandler.InputOutputHandlerController;
import outputStrategy.OutputStrategyController;
import recodingScheme.RecodingSchemeController;
import userDatabase.UserDatabaseController;


public abstract class Controller implements ComponentReferences {
	
	protected InputOutputHandlerController inputOutputHandler;
	protected OutputStrategyController outputStrategy;
	//protected MessageProcessorController messageProcessor;
	protected RecodingSchemeController recodingScheme;
	protected InfoServiceClient_v1 infoService;
	protected NetworkClockController networkClock;
	protected UserDatabaseController userDatabase;
	protected Settings settings;
	protected Implementation implementation; // konkrete Implementierung, die durch den ClassLoader geladen wird
	protected Mix mix;
	
	
	public Controller(Mix mix) {
		
		this.mix = mix;
		mix.registerComponent(this);
		
	}
	
	public void setComponentReferences(
			InputOutputHandlerController inputOutputHandler,
			OutputStrategyController outputStrategy,
			RecodingSchemeController recodingScheme,
			InfoServiceClient_v1 infoService,
			NetworkClockController networkClock,
			UserDatabaseController userDatabase,
			Settings settings
			) {
		
		this.infoService = infoService;
		this.inputOutputHandler = inputOutputHandler;
		this.outputStrategy = outputStrategy;
		this.recodingScheme = recodingScheme;
		this.networkClock = networkClock;
		this.userDatabase = userDatabase;
		this.settings = settings;
		
	}

	
	public void initialize() {
		this.implementation.callInitialize();
	}


	public void begin() {
		this.implementation.callBegin();
	}
	
	
	//overrides aus der Schnittstelle
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
		return networkClock;
	}
	
	
	@Override
	public UserDatabaseController getUserDatabase() {
		return userDatabase;
	}
	
	
	@Override
	public Settings getSettings() {
		return settings;
	}
	
	
	@Override
	public Mix getMix() {
		return mix;
	}

	
	//Getter und Setter für die konkrete Implementierung
	public void setImplementation(Implementation implementation) {
		
		assert this.implementation == null : "each controller may have only one implementation";
		assert implementation != null;
		
		this.implementation = implementation;
		
	}
	
	
	public Implementation getImplementation() {
		return implementation;
	}
	
	//abstrakte Methode, jeder Controller muss eine andere Schnittstelle implementieren
	public abstract void instantiateSubclass();
	public abstract String getPropertyKey();


	
}

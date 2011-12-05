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

import java.util.logging.Logger;

import networkClock.NetworkClockController;
import keyGenerator.KeyGeneratorController;
import externalInformationPort.ExternalInformationPortController;
import inputOutputHandler.InputOutputHandlerController;
import messageProcessor.MessageProcessorController;
import outputStrategy.OutputStrategyController;
import userDatabase.UserDatabaseController;


public abstract class Controller implements ComponentReferences {
	
	protected InputOutputHandlerController inputOutputHandler;
	protected OutputStrategyController outputStrategy;
	protected KeyGeneratorController keyGenerator;
	protected MessageProcessorController messageProcessor;
	protected ExternalInformationPortController externalInformationPort;
	protected NetworkClockController networkClock;
	protected UserDatabaseController userDatabase;
	protected Logger logger;
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
			KeyGeneratorController keyGenerator,
			MessageProcessorController messageProcessor,
			ExternalInformationPortController externalInformationPort,
			NetworkClockController networkClock,
			UserDatabaseController userDatabase,
			Logger logger,
			Settings settings
			) {
		
		this.inputOutputHandler = inputOutputHandler;
		this.outputStrategy = outputStrategy;
		this.keyGenerator = keyGenerator;
		this.messageProcessor = messageProcessor;
		this.externalInformationPort = externalInformationPort;
		this.networkClock = networkClock;
		this.userDatabase = userDatabase;
		this.logger = logger;
		this.settings = settings;
		
	}

	
	public void initialize() {
		this.implementation.initialize();
	}


	public void begin() {
		this.implementation.begin();
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
	public KeyGeneratorController getKeyGenerator() {
		return this.keyGenerator;
	}

	
	@Override
	public MessageProcessorController getMessageProcessor() {
		return this.messageProcessor;
	}
	
	
	@Override
	public ExternalInformationPortController getExternalInformationPort() {
		return this.externalInformationPort;
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
	public Logger getLogger() {
		return logger;
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
		this.implementation = implementation;
	}
	
	
	public Implementation getImplementation() {
		return implementation;
	}
	
	//abstrakte Methode, jeder Controller muss eine andere Schnittstelle implementieren
	public abstract void instantiateSubclass();
}

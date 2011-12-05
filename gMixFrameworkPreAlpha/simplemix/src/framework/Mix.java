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

import java.security.KeyPair;
import java.util.Vector;
import java.util.logging.Logger;

import inputOutputHandler.InputOutputHandlerController;
import networkClock.NetworkClockController;
import keyGenerator.KeyGeneratorController;
import externalInformationPort.ExternalInformationPortController;
import outputStrategy.OutputStrategyController;
import userDatabase.UserDatabaseController;
import messageProcessor.MessageProcessorController;


public class Mix implements ComponentReferences {
	
	private OutputStrategyController outputStrategy;
	private ExternalInformationPortController externalInformationPort;
	private InputOutputHandlerController inputOutputHandler;
	private KeyGeneratorController keyGenerator;
	private MessageProcessorController messageProcessor;
	private NetworkClockController networkClock;
	private UserDatabaseController userDatabase;
	private Settings settings = Settings.getSettings();
	private Logger logger;
	private KeyPair keyPair;
	
	private Vector<Controller> components = new Vector<Controller>();
	private Vector<Implementation> implementations = new Vector<Implementation>();
	
	
	/**
	 * Creates a new <code>Mix</code>.
	 * 
	 * @param args	Not used.
	 */
	public static void main(String[] args) {
		new Mix();
	} 
	

	//Zusammenfassung des Ablaufs...
	public Mix() {

		generateComponents();
		setReferencesBetweenComponents();
		loadImplementations();
		validateImplementationComposition();
		
		this.keyPair = (KeyPair) keyGenerator.generateKey(3);
		
		initializeComponents();
		beginMixing();

	}
	

	
	private void generateComponents() {
		
		this.logger = null; // TODO: logger einbinden
		this.settings = Settings.getSettings();		
		
		// NOTE: the order of the items is important (do not change it)
		this.keyGenerator = new KeyGeneratorController(this);
		this.outputStrategy = new OutputStrategyController(this);
		this.networkClock = new NetworkClockController(this);
		this.userDatabase = new UserDatabaseController(this);
		this.messageProcessor = new MessageProcessorController(this);
		this.inputOutputHandler = new InputOutputHandlerController(this);
		this.externalInformationPort = new ExternalInformationPortController(this);
		
	}
	
	
	//every component is automatically registered in the mix as this method is called in the constructor
	//of each controller
	protected void registerComponent(Controller controllerToRegister) {
		components.add(controllerToRegister);
	}
	
	//alle Controller, die zu diesem Mix geh�ren...
	public Controller[] getComponents() {
		return components.toArray(new Controller[0]);
	}

	
	private void setReferencesBetweenComponents() {
		
		for (Controller c:components)
			c.setComponentReferences(	inputOutputHandler, 
										outputStrategy, 
										keyGenerator, 
										messageProcessor, 
										externalInformationPort, 
										networkClock, 
										userDatabase, 
										logger,
										settings
									);

	}
	
	
	private void loadImplementations() {
		for (Controller c:components)
			c.instantiateSubclass();
	}
	
	protected void registerImplementation(Implementation implementationToRegister) {
		implementations.add(implementationToRegister);	
	}

	
	public Implementation[] getImplementations() {
		return implementations.toArray(new Implementation[0]);
	}
	
	
	private void validateImplementationComposition() {
		for (Implementation i:implementations)
			for (Implementation j:implementations)
				if (i.getBinaryName().equals(j.getBinaryName()))
					continue; // implementation always supports itself
				else if (!contains(i.getBinaryName(), j.getCompatibleImplementations()))
					throw new RuntimeException("ERROR: invalid composition of implementations!\n" +i.getBinaryName() +" does not support " +j.getBinaryName()); 
		// TODO: l�sung so noch nicht gut, weil f�r einen neue implementierung
		//(die mit allen bisherigen implementierungen kompatible ist) alle anderen implementierungen
		//um den namen der neuen implementierung erweitert werden m�ssen (= quelltext �ndern und neu compilieren...)
		//-> mindestens gegenseitiges pr�fen: sagt eine der implementierungen, sie funktioniert mit der anderen, reicht das aus
		
	}

	
	private void initializeComponents() {
		for (Controller c:components)
			c.initialize();
	}
	
	
	
	private void beginMixing() {
		for (Controller c:components)
			c.begin(); 
	}


	
	
	private boolean contains(String check, String[] pool) {
		for (String s: pool)
			if (s.equals(check))
				return true;
		return false;
	}
	

	//override aus ComponentReferences
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
		return this.networkClock;
	}


	@Override
	public UserDatabaseController getUserDatabase() {
		return this.userDatabase;
	}


	@Override
	public Logger getLogger() {
		return this.logger;
	}


	@Override
	public Settings getSettings() {
		return this.settings;
	}


	@Override
	public Mix getMix() {
		return this;
	}
	
	
	//R�ckgabe des Schl�sselpaars
	public KeyPair getKeyPair() {
		return this.keyPair;
	}	
}
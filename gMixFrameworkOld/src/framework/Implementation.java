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

import java.util.Vector;
import java.util.logging.Logger;

import client.ClientController;
import networkClock.NetworkClockController;
import outputStrategy.OutputStrategyController;
import recodingScheme.RecodingSchemeController;
import userDatabase.UserDatabaseController;


public abstract class Implementation implements ComponentReferences, ThreePhaseStart {
	
	protected InputOutputHandlerController inputOutputHandler;
	protected OutputStrategyController outputStrategy;
	protected RecodingSchemeController recodingScheme;
	protected InfoServiceClient_v1 infoService;
	protected NetworkClockController networkClock;
	protected UserDatabaseController userDatabase;
	protected Mix mix;
	protected Logger logger;
	protected Settings settings;
	protected Controller controller;
	protected ClientController clientController;
	protected Vector<SubImplementation> subImplementations;
	
	
	//showing the actual implementation time of all the components...
	static {
		//System.out.println(Implementation.class.getSimpleName());
	}
	
	public Implementation() {
		//System.out.println(this.getBinaryName());
	}
	
	
	public void registerSubImplementation(SubImplementation subImplementation) {
		if (subImplementations == null)
			subImplementations = new Vector<SubImplementation>();
		subImplementations.add(subImplementation);
	}
	
	
	//overrides aus der Schnittstelle ComponentReferences
	@Override
	public InputOutputHandlerController getInputOutputHandler() {
		return inputOutputHandler;
	}


	@Override
	public OutputStrategyController getOutputStrategy() {
		return outputStrategy;
	}


	@Override
	public RecodingSchemeController getRecodingScheme() {
		return recodingScheme;
	}


	@Override
	public InfoServiceClient_v1 getInfoService() {
		return infoService;
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
	public Mix getMix() {
		return mix;
	}
	
	
	@Override
	public Settings getSettings() {
		return settings;
	}
	
	
	public void setController(Controller controller) {
		
		assert this.controller == null : "controller already set";
		assert controller != null;
		
		controller.setImplementation(this);
		
		this.controller = controller;
		this.inputOutputHandler = controller.getInputOutputHandler();
		this.outputStrategy = controller.getOutputStrategy();
		this.recodingScheme = controller.getRecodingScheme();
		this.infoService = controller.getInfoService();
		this.networkClock = controller.getNetworkClock();
		this.userDatabase = controller.getUserDatabase();
		this.settings = controller.getSettings();
		this.mix = controller.getMix();
		this.mix.registerImplementation(this);
		
		//constructor();

	}
	
	
	public void setController(ClientController clientController) {
		
		assert this.clientController == null : "client already set";
		assert clientController != null;
		this.clientController = clientController;
		this.settings = clientController.getSettings();
		clientController.registerImplementation(this);
		
		//constructor();

	}
	
	
	protected void callInitialize() {
		/*if (subImplementations != null)
			for (SubImplementation si: subImplementations)
				si.constructor();*/
		initialize();
		//this.initializeCalled = true;
		if (subImplementations != null)
			for (SubImplementation si: subImplementations)
				si.initialize();
	}
	
	
	protected void callBegin() {
		begin();
		//this.beginCalled = true;
		if (subImplementations != null)
			for (SubImplementation si: subImplementations)
				si.begin();
	}
	
	// TODO: laden des strings mit den unterstützten implementierungen über instanzmethoden (so wie hier umgesetzt) nicht optimal,
	// weil so eine konkrete implementierung erst instanziiert werden muss, um ihre abhängigkeiten zu erfahren...
	// dies sollte vor dem instanziieren möglich sein! -> lösung über code-annotations o.ä.
	// (anforderung: nutzer muss spätestetns beim compilieren eine fehlermeldung erhalten,
	// wenn er keine abhängigkeiten für seine implementierung angegeben hat! -> nicht erst zur laufzeit)
	//public abstract String[] getCompatibleImplementations();
	
	// name of property file must be className.properties (e.g. BasicBatch.properties for the implementation BasicBatch.java)
	// TODO: wo sollen die property-dateien liegen müssen? (bei class files: -> werden dann normal nicht in eclipse angezeigt;
	// bei source-files: schlecht für deployment...)
	//public abstract boolean usesPropertyFile();
	
	public String getBinaryName() {
		return this.getClass().getName();
	}
	
}
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;
import framework.Logger;
import infoService.InfoServiceClient_v1;
import inputOutputHandler.InputOutputHandlerController;
import networkClock.NetworkClockController;
import outputStrategy.OutputStrategyController;
import recodingScheme.RecodingSchemeController;
import userDatabase.UserDatabaseController;



public class Mix implements ComponentReferences {
	
	private InfoServiceClient_v1 infoService;
	private OutputStrategyController outputStrategy;
	private InputOutputHandlerController inputOutputHandler;
	private RecodingSchemeController recodingScheme;
	private NetworkClockController networkClock;
	private UserDatabaseController userDatabase;
	private Settings settings;
	private boolean duplex;
	
	private Vector<Controller> components = new Vector<Controller>();
	private Vector<Implementation> implementations = new Vector<Implementation>();
	
	private int identifier;
	private boolean isLastMix;
	private boolean isFirstMix;
	private int numberOfMixes;
	
	/**
	 * Creates a new <code>Mix</code>.
	 * 
	 * @param args	Not used.
	 */
	public static void main(String[] args) {
		new Mix(new Settings(Paths.PATH_TO_GLOBAL_CONFIG));
	} 
	

	//Zusammenfassung des Ablaufs...
	public Mix(Settings settings) {

		initAndRegisterAtInfoService(settings);
		generateComponents();
		setReferencesBetweenComponents();
		loadImplementations();
		callConstructors();
		validateImplementationComposition();
		infoService.waitForEndOfRegistrationPhase();
		initializeComponents(); // calls "initialize()" for each implementation
		infoService.waitForEndOfInitializationPhase();
		beginMixing(); // calls "begin()" for each implementation
		
		System.out.println(this +" is up");
		infoService.waitForEndOfBeginPhase();
		
	}
	
	
	private void initAndRegisterAtInfoService(Settings settings) {
		Logger.init();
		this.settings = settings;
		Util.checkIfBCIsInstalled(); 
		this.infoService = new InfoServiceClient_v1(settings.getPropertyAsInetAddress("INFO_SERVICE_ADDRESS"), settings.getPropertyAsInt("INFO_SERVICE_PORT"));
		this.identifier = infoService.registerAsMix();
		boolean localMode = infoService.getIsLocalModeOn();
		if (!localMode) {
			System.err.println("WARNING: LOCAL_MODE is set to FALSE; Mixes and Information Service will be available via Network; Only safe for trusted networks!"); 
			if (settings.getProperty("BIND_ADDRESS").equalsIgnoreCase("AUTO"))
				try {
					settings.setProperty("BIND_ADDRESS", InetAddress.getLocalHost().getHostAddress());
				} catch (UnknownHostException e) {
					e.printStackTrace();
					throw new RuntimeException("could not detect ip address!"); 
				}
		} else {
			settings.setProperty("BIND_ADDRESS", "localhost");
		}
		if (settings.getProperty("PORT").equalsIgnoreCase("AUTO")) {
			int port = infoService.getSuggestedPort();
			settings.setProperty("PORT", ""+port);
		}
		infoService.postValueAsMix(identifier, "ADDRESS", settings.getPropertyAsInetAddress("BIND_ADDRESS").getAddress());
		infoService.postValueAsMix(identifier, "PORT", Util.intToByteArray(settings.getPropertyAsInt("PORT")));
		
		this.duplex = infoService.getIsDuplexModeOn();
		this.numberOfMixes = infoService.getNumberOfMixes();
		if (identifier == 0)
			isFirstMix = true;
		if (identifier == numberOfMixes-1)
			isLastMix = true;
	}
	
	
	private void generateComponents() {
		
		// NOTE: the order of the items is important (do not change it)
		this.outputStrategy = new OutputStrategyController(this);
		this.networkClock = new NetworkClockController(this);
		this.userDatabase = new UserDatabaseController(this);
		this.recodingScheme = new RecodingSchemeController(this);
		this.inputOutputHandler = new InputOutputHandlerController(this);

	}
	
	
	//every component is automatically registered in the mix as this method is called in the constructor
	//of each controller
	public void registerComponent(Controller controllerToRegister) {
		components.add(controllerToRegister);
	}
	
	//alle Controller, die zu diesem Mix gehören...
	public Controller[] getComponents() {
		return components.toArray(new Controller[0]);
	}

	
	private void setReferencesBetweenComponents() {
		
		for (Controller c:components)
			c.setComponentReferences(	inputOutputHandler, 
										outputStrategy, 
										recodingScheme, 
										infoService, 
										networkClock, 
										userDatabase, 
										settings
									);

	}
	
	
	private void loadImplementations() {
		for (Controller c:components) {
			//System.out.println("loading " +c.getPropertyKey() + " " +settings.getProperty(c.getPropertyKey())); 
			c.instantiateSubclass();
		}
	}
	
	
	private void callConstructors() {
		for (Controller c:components) {
			//System.out.println("loading " +c.getPropertyKey() + " " +settings.getProperty(c.getPropertyKey())); // TODO
			c.implementation.constructor();
		}
	}

	
	protected void registerImplementation(Implementation implementationToRegister) {
		implementations.add(implementationToRegister);	
	}

	
	public Implementation[] getImplementations() {
		return implementations.toArray(new Implementation[0]);
	}
	
	
	private void validateImplementationComposition() {
		return; // TODO
		/*for (Implementation i:implementations)
			for (Implementation j:implementations)
				if (i.getBinaryName().equals(j.getBinaryName()))
					continue; // implementation always supports itself
				else if (!contains(i.getBinaryName(), j.getCompatibleImplementations()))
					throw new RuntimeException("ERROR: invalid composition of implementations!\n" +i.getBinaryName() +" does not support " +j.getBinaryName()); 
		*/
		// TODO: lösung so noch nicht gut, weil für einen neue implementierung
		//(die mit allen bisherigen implementierungen kompatible ist) alle anderen implementierungen
		//um den namen der neuen implementierung erweitert werden müssen (= quelltext ändern und neu compilieren...)
		//-> mindestens gegenseitiges prüfen: sagt eine der implementierungen, sie funktioniert mit der anderen, reicht das aus
		
	}

	
	private void initializeComponents() {
		for (Controller c:components)
			c.initialize();
	}
	
	
	private void beginMixing() {
		for (Controller c:components)
			c.begin(); 
	}
	
	
	/*private boolean contains(String check, String[] pool) {
		for (String s: pool)
			if (s.equals(check))
				return true;
		return false;
	}*/
	

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
	public RecodingSchemeController getRecodingScheme() {
		return this.recodingScheme;
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
	public InfoServiceClient_v1 getInfoService() {
		return this.infoService;
	}
	

	@Override
	public Settings getSettings() {
		return this.settings;
	}


	@Override
	public Mix getMix() {
		return this;
	}
	
	@Override
	public String toString() {
		return "mix " +this.identifier;// +"(" +super.toString() +")";
	}


	public int getIdentifier() {
		return this.identifier;
	}

	
	public int getNumberOfMixes() {
		return this.numberOfMixes;
	}

	public boolean isLastMix() {
		return this.isLastMix;
	}

	
	public boolean isFirstMix() {
		return this.isFirstMix;
	}
	
	
	public boolean isSingleMix() {
		return isLastMix() && isFirstMix();
	}
	
	public boolean isDuplex() {
		return duplex;
	}
	
	
	
}
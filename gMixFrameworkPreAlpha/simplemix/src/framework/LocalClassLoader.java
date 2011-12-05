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

import architectureInterface.ExternalInformationPortInterface;
import architectureInterface.InputOutputHandlerInterface;
import architectureInterface.KeyGeneratorInterface;
import architectureInterface.MessageProcessorInterface;
import architectureInterface.NetworkClockInterface;
import architectureInterface.OutputStrategyInterface;
import architectureInterface.UserDatabaseInterface;


public class LocalClassLoader extends ClassLoader {

	private static LocalClassLoader localClassLoader = new LocalClassLoader();
    
	
	public static Class<?> loadClassFile(String binaryClassName) throws ClassNotFoundException { 
		
		binaryClassName = normalizeBinaryClassName(binaryClassName);
		
		if (getSystemClassLoader().loadClass(binaryClassName) != null)
			return getSystemClassLoader().loadClass(binaryClassName);
		else
			return localClassLoader.loadClass(binaryClassName, true);
		
	}


	public static Object newInstance(String binaryClassName) throws Exception { 
		
		binaryClassName = normalizeBinaryClassName(binaryClassName);
		
		Class<?> r = loadClassFile(binaryClassName);
		return r.newInstance();

	}
	
	//f�r Testzwecke
	static void checkFlag(String binaryClassName) throws ClassNotFoundException, SecurityException, NoSuchFieldException {
		//TODO Methode muss sinnvoll genutzt werden, beispielsweise von der GUI...
		@SuppressWarnings("unused")
		Class<?> r = loadClassFile(binaryClassName);
		//Field s = r.getDeclaredField("teststring");		
		//Class<?> e = r.ge
		//System.out.println(s.toString());
		//Class<Implementation> i = ((Class<Implementation>) loadClassFile(binaryClassName));
		//System.out.println(i.teststring);
		//.getDeclaredConstructors().toString());
	}
	
	
	public static String normalizeBinaryClassName(String binaryClassName) {
		if (binaryClassName.endsWith(".class"))
			binaryClassName = binaryClassName.replace(".class", "");
		else if (binaryClassName.endsWith(".java"))
			binaryClassName = binaryClassName.replace(".java", "");
		return binaryClassName;
	}

	
	public static Implementation instantiateImplementation(String className, String packageName) { 

		try {
			
			String binaryName = packageName + "." +className;
			
			Object o;
			o = newInstance(binaryName);
			
			if (!(o instanceof Implementation))
				throw new RuntimeException("ERROR: " +className +" must be a child of the class \"framework.Implementation.java\"!"); 
			
			return (Implementation)o;
			
		} catch (Exception e) {
			
			e.printStackTrace();
			throw new RuntimeException("ERROR: could not load " +className +"\nwrong name in property file?"); 
			
		}

	} 
	
	
	public static OutputStrategyInterface instantiateOutputStrategyImplementation(Controller controller) { 
		
		Implementation implementation = instantiateImplementation(Settings.getProperty("OUTPUT_STRATEGY"), "outputStrategy");
		
		if (!(implementation instanceof OutputStrategyInterface))
			throw new RuntimeException("ERROR: " +Settings.getProperty("OUTPUT_STRATEGY") +" must implement the interface \"OutputStrategy.java\"!"); 
			
		implementation.setController(controller);
		return (OutputStrategyInterface)implementation;
		
	}
	
	
	public static InputOutputHandlerInterface instantiateInputOutputHandlerImplementation(Controller controller) { 
		
		Implementation implementation = instantiateImplementation(Settings.getProperty("INPUT_OUTPUT_HANDLER"), "inputOutputHandler");
		
		if (!(implementation instanceof InputOutputHandlerInterface))
			throw new RuntimeException("ERROR: " +Settings.getProperty("INPUT_OUTPUT_HANDLER") +" must implement the interface \"InputOutputHandler.java\"!"); 
		
		implementation.setController(controller);
		return (InputOutputHandlerInterface)implementation;
		
	}
	
	public static ExternalInformationPortInterface instantiateExternalInformationPortImplementation(Controller controller) { 
		
		Implementation implementation = instantiateImplementation(Settings.getProperty("EXTERNAL_INFORMATION_PORT"), "externalInformationPort");
		
		if (!(implementation instanceof ExternalInformationPortInterface))
			throw new RuntimeException("ERROR: " +Settings.getProperty("EXTERNAL_INFORMATION_PORT") +" must implement the interface \"ExternalInformationPort.java\"!"); 
		
		implementation.setController(controller);
		return (ExternalInformationPortInterface)implementation;
		
	}

	
	public static KeyGeneratorInterface instantiateKeyGeneratorImplementation(Controller controller) { 
		
		Implementation implementation = instantiateImplementation(Settings.getProperty("KEY_GENERATOR"), "keyGenerator");
		
		if (!(implementation instanceof KeyGeneratorInterface))
			throw new RuntimeException("ERROR: " +Settings.getProperty("KEY_GENERATOR") +" must implement the interface \"KeyGenerator.java\"!"); 
		
		implementation.setController(controller);
		return (KeyGeneratorInterface)implementation;
		
	}
	
	
	public static MessageProcessorInterface instantiateMessageProcessorImplementation(Controller controller) { 
		
		Implementation implementation = instantiateImplementation(Settings.getProperty("MESSAGE_PROCESSOR"), "messageProcessor");
		
		if (!(implementation instanceof MessageProcessorInterface))
			throw new RuntimeException("ERROR: " +Settings.getProperty("MESSAGE_PROCESSOR") +" must implement the interface \"MessageProcessor.java\"!"); 
		
		implementation.setController(controller);
		return (MessageProcessorInterface)implementation;
		
	}
	
	
	public static NetworkClockInterface instantiateNetworkClockImplementation(Controller controller) { 
		
		Implementation implementation = instantiateImplementation(Settings.getProperty("NETWORK_CLOCK"), "networkClock");
		
		if (!(implementation instanceof NetworkClockInterface))
			throw new RuntimeException("ERROR: " +Settings.getProperty("NETWORK_CLOCK") +" must implement the interface \"NetworkClock.java\"!"); 
		
		implementation.setController(controller);
		return (NetworkClockInterface)implementation;
	
	}
	
	
	public static UserDatabaseInterface instantiateUserDatabaseImplementation(Controller controller) { 
		
		Implementation implementation = instantiateImplementation(Settings.getProperty("USER_DATABASE"), "userDatabase");
		
		if (!(implementation instanceof UserDatabaseInterface))
			throw new RuntimeException("ERROR: " +Settings.getProperty("USER_DATABASE") +" must implement the interface \"UserDatabase.java\"!"); 
		
		implementation.setController(controller);
		return (UserDatabaseInterface)implementation;
	
	}

}
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
import framework.core.interfaces.ArchitectureInterface;


public class LocalClassLoader extends ClassLoader {

	private static LocalClassLoader localClassLoader = new LocalClassLoader();
	
	
	public static Class<?> loadClassFile(String packageName, String className) throws ClassNotFoundException { 
		
		String binaryClassName = packageName + "." +removeFileExtension(className);
		
		if (getSystemClassLoader().loadClass(binaryClassName) != null)
			return getSystemClassLoader().loadClass(binaryClassName);
		else
			return localClassLoader.loadClass(binaryClassName, true);
		
	}
	
	
	@SuppressWarnings("unchecked") // generic declaration assures type safety here; classes loaded at runtime can only be validated at runtime
	public static <T> T newInstance(String packageName, String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException { 
		className = removeFileExtension(className);
		Class<?> r = loadClassFile(packageName, className);
		T instance = (T) r.newInstance();
		
		assert !(instance instanceof Implementation) :"Implementations must be instantiated using \"instantiateImplementation(String packageName, String className, Controller controller, Class<T> desiredType\"";
		// TODO assert !(instance instanceof SubImplementation) :"SubImplementations must be instantiated using \"instantiateSubImplementation(String packageName, String className, Implementation owner, Class<T> desiredType)\"";
		
		return instance;
	}
	
	
	public static <T extends ArchitectureInterface> T instantiateImplementation(String packageName, String className, Controller controller, Class<T> desiredType) { 
		try {
			className = removeFileExtension(className);
			Class<?> r = loadClassFile(packageName, className);
			Object o = r.newInstance();
			if (!(o instanceof Implementation))
				throw new RuntimeException(packageName + "." +className +" must extend framework.core.controller.Implementation.java");
			T impl = desiredType.cast(o);
			((Implementation)impl).setController(controller);
			return impl;
		} catch (ClassCastException e) {
			throw new RuntimeException(packageName + "." +className +" must implement the " +desiredType.toString() +".java"); 
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("ERROR: could not load " +packageName + "." +className +"\nwrong name in property file?");  
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException(packageName + "." +className +" must implement the " +desiredType.toString() +".java");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException(packageName + "." +className +" must implement the " +desiredType.toString() +".java");
		}
	}
	
	
	public static SubImplementation instantiateSubImplementation(String packageName, String className, AnonNode owner) { 
		try {
			className = removeFileExtension(className);
			Class<?> r = loadClassFile(packageName, className);
			Object o = r.newInstance();
			if (!(o instanceof SubImplementation))
				throw new RuntimeException(packageName + "." +className +" must extend framework.core.controller.SubImplementation.java");
			((SubImplementation)o).setAnonNode(owner);
			return (SubImplementation)o;
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new RuntimeException(); 
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("ERROR: could not load " +packageName + "." +className +"\nwrong name in property file?");  
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	
	
	/*public static <T extends ArchitectureInterface> T instantiateImplementation(Controller controller, Class<T> desiredType, Settings settings) { 
		String className = settings.getProperty(controller.getPropertyKey());
		className = removeFileExtension(className);
		String packageName = controller.getClass().getPackage().getName(); 
		return instantiateImplementation(packageName, className, controller, desiredType);
	}*/

	
	public static String removeFileExtension(String string) {
		string = string.replace(".class", "");
		string = string.replace(".java", "");
		return string;
	}
	
	
	/*public static Request instantiateMixRequest(String className, String packageName) { 
		try {
			Object o = newInstance(packageName, className);
			if (!(o instanceof MixMessage))
				throw new RuntimeException("ERROR: " +className +" must be a child of the class \"message.Request.java\"!"); 
			return (Request)o;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("ERROR: could not load " +className +"\nwrong name in property file?"); 
		}
	}
	
	
	public static Request instantiateMixRequest() {
		return new Request();
	}
	
	
	public static Request instantiateMixRequest(Settings settings) { 
		return instantiateMixRequest(settings.getProperty("MIX_REQUEST_FORMAT"), "message");
	}
	
	
	public static Reply instantiateMixReply(String className, String packageName) { 

		try {
			
			Object o = newInstance(packageName, className);
			
			if (!(o instanceof MixMessage))
				throw new RuntimeException("ERROR: " +className +" must be a child of the class \"message.Reply.java\"!"); 
			
			return (Reply)o;
			
		} catch (Exception e) {
			
			e.printStackTrace();
			throw new RuntimeException("ERROR: could not load " +className +"\nwrong name in property file?"); 
			
		}

	}
	
	
	public static Reply instantiateMixReply(String className, Settings settings) { 
		return instantiateMixReply(className, "message");
	}
	
	
	public static Reply instantiateMixReply(Settings settings) { 
		return instantiateMixReply(settings.getProperty("MIX_REPLY_FORMAT"), "message");
	}
	*/
	
	/*public static <T extends ArchitectureInterface> T instantiateImplementation(String packageName, String className, ClientController clientController, Class<T> desiredType) { 
		try {
			className = removeFileExtension(className);
			Class<?> r = loadClassFile(packageName, className);
			Object o = r.newInstance();
			if (!(o instanceof Implementation))
				throw new RuntimeException(packageName + "." +className +" must extend framework.Implementation.java");
			T impl = desiredType.cast(o);
			((Implementation)impl).setController(clientController);
			return impl;
		} catch (ClassCastException e) {
			throw new RuntimeException(packageName + "." +className +" must implement the " +desiredType.toString() +".java"); 
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("ERROR: could not load " +packageName + "." +className +"\nwrong name in property file?");  
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException(packageName + "." +className +" must implement the " +desiredType.toString() +".java");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException(packageName + "." +className +" must implement the " +desiredType.toString() +".java");
		}
	}*/
	
	
	/*public static <T extends SubImplementationInterface> T instantiateSubImplementation(String packageName, String className, Implementation owner, Class<T> desiredType) { 
		try {
			className = removeFileExtension(className);
			Class<?> r = loadClassFile(packageName, className);
			Object o = r.newInstance();
			if (!(o instanceof SubImplementation))
				throw new RuntimeException(packageName + "." +className +" must extend framework.SubImplementation.java");
			T impl = desiredType.cast(o);
			((SubImplementation)impl).setOwner(owner);
			return impl;
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new RuntimeException(packageName + "." +className +" must implement the interface " +desiredType.toString().replace("class ", "") +".java"); 
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("ERROR: could not load " +packageName + "." +className +"\nwrong name in property file?");  
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException(packageName + "." +className +" must implement the interface " +desiredType.toString().replace("class ", "") +".java");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException(packageName + "." +className +" must implement the interface " +desiredType.toString().replace("class ", "") +".java");
		}
	}
	
	
	public static SubImplementation instantiateSubImplementation(String packageName, String className, Implementation owner) { 
		try {
			className = removeFileExtension(className);
			Class<?> r = loadClassFile(packageName, className);
			Object o = r.newInstance();
			if (!(o instanceof SubImplementation))
				throw new RuntimeException(packageName + "." +className +" must extend framework.SubImplementation.java");
			((SubImplementation)o).setOwner(owner);
			return (SubImplementation)o;
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new RuntimeException(); 
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("ERROR: could not load " +packageName + "." +className +"\nwrong name in property file?");  
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	
	public static <T extends SubImplementationInterface> T instantiateSubImplementation(String packageName, String className, ClientImplementation owner, Class<T> desiredType) { 
		try {
			className = removeFileExtension(className);
			Class<?> r = loadClassFile(packageName, className);
			Object o = r.newInstance();
			if (!(o instanceof SubImplementation))
				throw new RuntimeException(packageName + "." +className +" must extend framework.SubImplementation.java");
			T impl = desiredType.cast(o);
			((SubImplementation)impl).setOwner(owner);
			return impl;
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new RuntimeException(packageName + "." +className +" must implement the interface " +desiredType.toString().replace("class ", "") +".java"); 
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("ERROR: could not load " +packageName + "." +className +"\nwrong name in property file?");  
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException(packageName + "." +className +" must implement the interface " +desiredType.toString().replace("class ", "") +".java");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException(packageName + "." +className +" must implement the interface " +desiredType.toString().replace("class ", "") +".java");
		}
	}
	
	
	public static SubImplementation instantiateSubImplementation(String packageName, String className, ClientImplementation owner) { 
		try {
			className = removeFileExtension(className);
			Class<?> r = loadClassFile(packageName, className);
			Object o = r.newInstance();
			if (!(o instanceof SubImplementation))
				throw new RuntimeException(packageName + "." +className +" must extend framework.SubImplementation.java");
			((SubImplementation)o).setOwner(owner);
			return (SubImplementation)o;
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new RuntimeException(); 
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("ERROR: could not load " +packageName + "." +className +"\nwrong name in property file?");  
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}*/
	
	
	/*public static <T extends ClientImplementation> T instantiateClientImplementation(String packageName, String className, ClientController owner, Class<T> desiredType) { 
		try {
			className = removeFileExtension(className);
			Class<?> r = loadClassFile(packageName, className);
			Object o = r.newInstance();
			if (!(o instanceof ClientImplementation))
				throw new RuntimeException(packageName + "." +className +" must extend client.ClientImplementation.java");
			T impl = desiredType.cast(o);
			((ClientImplementation)impl).setController(owner);
			return impl;
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new RuntimeException(packageName + "." +className +" must implement the interface " +desiredType.toString().replace("class ", "") +".java"); 
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("ERROR: could not load " +packageName + "." +className +"\nwrong name in property file?");  
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException(packageName + "." +className +" must implement the interface " +desiredType.toString().replace("class ", "") +".java");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException(packageName + "." +className +" must implement the interface " +desiredType.toString().replace("class ", "") +".java");
		}
	}*/
	
	
	/*public static RecodingSchemeMix instantiateRecodingSchemeSubImplementationMix(Controller controller) {
		return instantiateImplementation("recodingScheme", Settings.getProperty("RECODING_SCHEME").replace(".java", "_Mix.java"), controller, RecodingSchemeMix.class);
		/*
		Implementation implementation = newImplementationInstance("recodingScheme", Settings.getProperty("RECODING_SCHEME").replace(".java", "_Mix.java"), controller);
		
		if (!(implementation instanceof RecodingSchemeMix))
			throw new RuntimeException("ERROR: " +Settings.getProperty("RECODING_SCHEME") +" must implement the interface \"RecodingSchemeMix.java\"!"); 
		
		implementation.setController(controller);
		return (RecodingSchemeMix)implementation;*/
	/*}

	
	public static RecodingSchemeClient instantiateRecodingSchemeSubImplementationClient(Controller controller) {
	
		Implementation implementation = newImplementationInstance("recodingScheme", Settings.getProperty("RECODING_SCHEME").replace(".java", "_Client.java"), controller);
	
		if (!(implementation instanceof RecodingSchemeClient))
			throw new RuntimeException("ERROR: " +Settings.getProperty("RECODING_SCHEME") +" must implement the interface \"RecodingSchemeClient.java\"!"); 
	
		implementation.setController(controller);
		return (RecodingSchemeClient)implementation;
	}*/

}
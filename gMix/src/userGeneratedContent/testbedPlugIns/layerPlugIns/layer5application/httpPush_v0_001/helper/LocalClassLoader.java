/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
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
 *******************************************************************************/
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper;

import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.improvement.EntryImprovementInterface;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.improvement.ExitImprovementInterface;


public class LocalClassLoader extends ClassLoader {

	private static LocalClassLoader localClassLoader = new LocalClassLoader();

	public static Class<?> loadClassFile(String packageName, String className)
			throws ClassNotFoundException {

		String binaryClassName = packageName + "."
				+ removeFileExtension(className);

		if (getSystemClassLoader().loadClass(binaryClassName) != null)
			return getSystemClassLoader().loadClass(binaryClassName);
		else
			return localClassLoader.loadClass(binaryClassName, true);

	}

	@SuppressWarnings("unchecked")
	// generic declaration assures type safety here; classes loaded at runtime
	// can only be validated at runtime
	public static <T> T newInstance(String packageName, String className)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		className = removeFileExtension(className);
		Class<?> r = loadClassFile(packageName, className);
		T instance = (T) r.newInstance();

		return instance;
	}

	public static <T> T instantiateImprovement(
			String packageName, String className,
			Class<T> desiredType) {
		try {
			className = removeFileExtension(className);
			Class<?> r = loadClassFile(packageName, className);
			Object o = r.newInstance();
			if (!(o instanceof ExitImprovementInterface) && !(o instanceof EntryImprovementInterface))
				throw new RuntimeException(
						packageName
								+ "."
								+ className
								+ " must implement the improvement interfaces");
			T impl = desiredType.cast(o);
			
			return impl;
		} catch (ClassCastException e) {
			throw new RuntimeException(packageName + "." + className
					+ " must implement the " + desiredType.toString() + ".java");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("ERROR: could not load " + packageName
					+ "." + className + "\nwrong name in property file?");
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException(packageName + "." + className
					+ " must implement the " + desiredType.toString() + ".java");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException(packageName + "." + className
					+ " must implement the " + desiredType.toString() + ".java");
		}
	}


	public static String removeFileExtension(String string) {
		string = string.replace(".class", "");
		string = string.replace(".java", "");
		return string;
	}

	

}
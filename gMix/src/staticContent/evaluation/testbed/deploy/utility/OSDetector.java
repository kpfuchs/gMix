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
package staticContent.evaluation.testbed.deploy.utility;


public class OSDetector {
	
	private static final String OS = System.getProperty("os.name", "unknown").toLowerCase();

	
	/**
	 * @return true if the OS is a version of Windows, otherwise false.
	 */
	public static boolean isWindows() {
		return (OS.indexOf("windows") >= 0);
	}

	/**
	 * @return true if the OS is a version of Mac, otherwise false.
	 */
	public static boolean isMac() {
		return (OS.startsWith("mac") || OS.startsWith("darwin"));
	}

	/**
	 * @return true if the OS is a version of Unix, otherwise false.
	 */
	public static boolean isLinux() {
		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 );
	}

	/**
	 * @return true if the OS is a version of Windows, otherwise false.
	 */
	public static boolean isSolaris() {
		return (OS.indexOf("sunos") >= 0);
	}
	
	
	public static boolean isUnknown() {
		return (!isWindows() && !isMac() && !isLinux() && !isSolaris()) ? true : false;
	}
}
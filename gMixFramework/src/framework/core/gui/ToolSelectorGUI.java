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
package framework.core.gui;

import framework.core.launcher.CommandLineParameters;


// displays a menu to choose some tool of the framework (r.g. the simulator, 
// the load generator, the info service, a mix, a client etc.)
public class ToolSelectorGUI {

	
	public ToolSelectorGUI(CommandLineParameters params) {
		System.out.println("GUI not yet available...");
		new ToolSelectorCommandLine(params);
	}

	
	public static void main(String[] args) {
		new ToolSelectorGUI(new CommandLineParameters(args));
	} 

}

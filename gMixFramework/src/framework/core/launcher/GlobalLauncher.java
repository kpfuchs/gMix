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
package framework.core.launcher;

import framework.core.gui.ToolSelectorCommandLine;
import framework.core.gui.ToolSelectorGUI;


// main class of the project/framework
public class GlobalLauncher {

	
	public static void main(String[] args) {
		CommandLineParameters params = new CommandLineParameters(args);
		System.out.println(params); // TODO: remove
		if (params.gMixTool != null && params.gMixTool != ToolName.NOT_SET) // start desired tool directly
			ToolName.execute(params);
		else if (params.useGui) // no tool specified; GUI mode on
			new ToolSelectorGUI(params);
		else // no tool specified; GUI mode off
			new ToolSelectorCommandLine(params);
	} 

}

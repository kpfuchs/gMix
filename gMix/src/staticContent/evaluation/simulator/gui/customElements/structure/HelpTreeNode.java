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
package staticContent.evaluation.simulator.gui.customElements.structure;

import org.apache.log4j.Logger;

import staticContent.evaluation.simulator.gui.launcher.GuiLauncher;

public class HelpTreeNode {
	private String helpTreeNodeName;
	private String helpTreeNodeURL;
	
	private static Logger logger = Logger.getLogger(GuiLauncher.class);

	public HelpTreeNode(String string, String filename) {
		setHelpTreeNodeName(string);
		setHelpTreeNodeURL(filename);
		if (getHelpTreeNodeURL() == null) {
			logger.error("Couldn't find file: " + filename);
		}
	}

	public String toString() {
		return getHelpTreeNodeName();
	}

	public String getHelpTreeNodeName() {
		return helpTreeNodeName;
	}

	public void setHelpTreeNodeName(String helpTreeNodeName) {
		this.helpTreeNodeName = helpTreeNodeName;
	}

	public String getHelpTreeNodeURL() {
		return helpTreeNodeURL;
	}

	public void setHelpTreeNodeURL(String helpTreeNodeURL) {
		this.helpTreeNodeURL = helpTreeNodeURL;
	}
}

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
package evaluation.simulator.gui.actionListeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import evaluation.simulator.gui.customElements.ConfigChooserPanel;
import evaluation.simulator.gui.customElements.PluginPanel;

/**
 * implements the ActionListener for resetting the {@link PluginPanel}. By now
 * it is just updating the config file directory.
 * 
 * @author nachkonvention
 */
public class ResetButtonAction implements ActionListener {

	private static Logger logger = Logger.getLogger(ResetButtonAction.class);

	@Override
	public void actionPerformed(ActionEvent e) {
		logger.log(Level.DEBUG, "Reset config");
		ConfigChooserPanel.getInstance().updateConfigDirectory();

	}

}

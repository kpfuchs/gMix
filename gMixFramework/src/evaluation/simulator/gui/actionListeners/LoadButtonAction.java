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
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import evaluation.simulator.conf.service.SimulationConfigService;
import evaluation.simulator.gui.customElements.ConfigChooserPanel;
import evaluation.simulator.gui.customElements.PluginPanel;
import evaluation.simulator.gui.pluginRegistry.SimPropRegistry;
import evaluation.simulator.gui.service.GuiService;

/**
 * implements the ActionListener for loading a configuration file into the
 * {@link PluginPanel}
 * 
 * @author nachkonvention
 */
public class LoadButtonAction implements ActionListener {
	private static Logger logger = Logger.getLogger(LoadButtonAction.class);

	@Override
	public void actionPerformed(ActionEvent e) {
		logger.log(Level.DEBUG, "Load config");
		if (SimPropRegistry.getInstance().getUnsavedChanges()) {
			int backValue = JOptionPane
					.showConfirmDialog(
							GuiService.getInstance().getMainGui(),
							"There are unsaved changes! They will get lost if you load a new configuration. Do you want to discard changes and load a new configuration?",
							"Unsaved changes",
							JOptionPane.YES_NO_OPTION);
			if (backValue == JOptionPane.YES_OPTION) {
				load();
			}
		} else {
			load();
		}
	}

	private void load() {
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File("inputOutput/simulator/experimentDefinitions"));
		fc.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {
				return "Config File";
			}

			@Override
			public boolean accept(File f) {
				return f.getName().toLowerCase().endsWith(".cfg");
			}
		});
		int state = fc.showOpenDialog(null);
		if (state == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			SimPropRegistry simPropRegistry = SimPropRegistry.getInstance();
			SimulationConfigService simulationConfigService = new SimulationConfigService(
					simPropRegistry);
			simulationConfigService.loadConfig(file);
		}
		ConfigChooserPanel.getInstance().updateConfigDirectory();
	}
}
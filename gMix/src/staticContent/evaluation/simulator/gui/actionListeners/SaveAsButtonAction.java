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
package staticContent.evaluation.simulator.gui.actionListeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import staticContent.evaluation.simulator.conf.service.SimulationConfigService;
import staticContent.evaluation.simulator.gui.customElements.ConfigChooserPanel;
import staticContent.evaluation.simulator.gui.customElements.PluginPanel;
import staticContent.evaluation.simulator.gui.pluginRegistry.SimPropRegistry;

/**
 * implements the ActionListener for saving the configuration into a .cfg file
 * from the {@link PluginPanel}.
 * 
 * @author nachkonvention
 */
public class SaveAsButtonAction implements ActionListener {
	private static Logger logger = Logger.getLogger(SaveAsButtonAction.class);

	@Override
	public void actionPerformed(ActionEvent e) {

		logger.log(Level.DEBUG, "Save config");

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

		int state = fc.showSaveDialog(null);
		if (state == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			if (!fc.getSelectedFile().getAbsolutePath().endsWith(".cfg")) {
				file = new File (fc.getSelectedFile() +".cfg" );
			}
		
			SimPropRegistry simPropRegistry = SimPropRegistry.getInstance();
			SimulationConfigService simulationConfigService = new SimulationConfigService(simPropRegistry);
			simulationConfigService.writeConfig(file);
			// Now there are no unsaved changes
			SimPropRegistry.getInstance().setUnsavedChanges(false);

		}
		ConfigChooserPanel.getInstance().updateConfigDirectory();

	}

}

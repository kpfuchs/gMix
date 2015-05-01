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

import staticContent.evaluation.simulator.core.binding.gMixBinding;
import staticContent.evaluation.simulator.gui.customElements.ConfigChooserPanel;
import staticContent.evaluation.simulator.gui.layout.SimulationTab;

/**
 * implements the ActionListener for clearing the Simulator output
 * 
 * @author nachkonvention
 */
public class ClearButtonAction implements ActionListener {

	@Override
	public void actionPerformed(ActionEvent e) {
		SimulationTab.getInstance().getResultsPanel().removeAll();
		gMixBinding.getInstance().resetExperiments();
		SimulationTab.getInstance().getResultsPanel().add("Welcome", SimulationTab.getInstance().homeTab);
		/*try {
			IOActions.cleanOutputFolder();
		} catch (IOException e1) {
			JOptionPane.showMessageDialog(null, "Could not clean up Output directory " + GnuplotPanel.outputFolder,
					"Cleanup Error", JOptionPane.ERROR_MESSAGE);
		}*/
		ConfigChooserPanel.getInstance();
		ConfigChooserPanel.getExportPictureButton().setEnabled(false);
		ConfigChooserPanel.getInstance().updateConfigDirectory();
	}

}

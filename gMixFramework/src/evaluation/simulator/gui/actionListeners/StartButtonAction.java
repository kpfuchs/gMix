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

import javax.swing.JList;
import javax.swing.JOptionPane;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import evaluation.simulator.core.binding.gMixBinding;
import evaluation.simulator.gui.customElements.ProgressWorker;
import evaluation.simulator.gui.customElements.SimConfigPanel;
import evaluation.simulator.gui.service.ProgressRegistry;

/**
 * implements the ActionListener for starting the Simulator with the previosuly
 * chosen config from {@link SimConfigPanel}.
 * 
 * @author nachkonvention
 */
public class StartButtonAction implements ActionListener {

	JList<File> configList;
	private final Logger logger = Logger.getLogger(gMixBinding.class);

	/**
	 * @param configList
	 *            the list of config files selected in {@link SimConfigPanel}
	 */
	public StartButtonAction(JList<File> configList) {
		this.configList = configList;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (configList.getSelectedIndices().length == 0) {
			JOptionPane.showMessageDialog(null, "Please select at least one config!");
		} else {
			this.logger.log(Level.INFO, "Start simulator");
			
			ProgressWorker progress = new ProgressWorker();
			ProgressRegistry.getInstance().setProgressWorker(progress);
			progress.enable();
			progress.execute();
		}
	}

}

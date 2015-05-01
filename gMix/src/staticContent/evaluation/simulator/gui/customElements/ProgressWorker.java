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
package staticContent.evaluation.simulator.gui.customElements;

import java.io.File;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

import staticContent.evaluation.simulator.core.binding.gMixBinding;
import staticContent.evaluation.simulator.gui.layout.SimulationTab;
import staticContent.evaluation.simulator.gui.service.ConfigParser;

/**
 * {@link SwingWorker} implementing a simple {@link JProgressBar} for displaying
 * that the Simulator is running and generating output.
 * 
 * @author nachkonvention
 * 
 */
@SuppressWarnings("rawtypes")
public class ProgressWorker extends SwingWorker {
	
	private boolean stop = false;
	
	protected String doInBackground() {
		
		ConfigChooserPanel.getProgressBar().setVisible(true);
		ConfigChooserPanel.getProgressBar().setIndeterminate(true);

		SimulationTab.getInstance().getResultsPanel().remove(SimulationTab.getInstance().homeTab);

		ConfigParser configParser = new ConfigParser();

		String[][] params = new String[ConfigChooserPanel.getInstance().getConfigList().getSelectedValuesList().size()][1];

		int i = 0;
		for (File file : ConfigChooserPanel.getInstance().getConfigList().getSelectedValuesList()) {
			
			if (stop){
				return null;
			}
			
			params[i][0] = configParser.cleanupConfigurationForSimulator(file);

			ConfigChooserPanel.setCallSimulation(gMixBinding.getInstance());
			ConfigChooserPanel.getCallSimulation().setParams(params[i]);
			ConfigChooserPanel.getCallSimulation().enable();
			ConfigChooserPanel.getCallSimulation().run();
			final int j = i;

			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					ConfigChooserPanel.getProgressBar().setValue(j);
				}
			});
			i++;
		}
		return null;
	}

	protected void done() {
		ConfigChooserPanel.getProgressBar().setVisible(false);
	}
	
	public void requestStop(){
		stop = true;
	}
	
	public void enable(){
		stop = false;
	}
}
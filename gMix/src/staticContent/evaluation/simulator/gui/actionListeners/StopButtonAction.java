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

import org.apache.log4j.Logger;

import staticContent.evaluation.simulator.gui.customElements.ConfigChooserPanel;
import staticContent.evaluation.simulator.gui.customElements.ProgressWorker;
import staticContent.evaluation.simulator.gui.service.ProgressRegistry;

/**
 * implements the ActionListener for stopping the currently running Simulator.
 * 
 * @author nachkonvention
 */
public class StopButtonAction implements ActionListener {
	
	private static Logger logger = Logger.getLogger(StopButtonAction.class);

	@Override
	public void actionPerformed(ActionEvent e) {
		ConfigChooserPanel.getInstance();
		logger.info("Interrupt simulator");
		
		ProgressWorker progress = ProgressRegistry.getInstance().getProgressWorker();
		progress.requestStop();
		
		ConfigChooserPanel.getCallSimulation().requestStop();
	}

}

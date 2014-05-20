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
package evaluation.simulator.gui.launcher;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import evaluation.simulator.conf.service.SimulationConfigService;
import evaluation.simulator.gui.pluginRegistry.DependencyChecker;
import evaluation.simulator.gui.pluginRegistry.SimPropRegistry;
import evaluation.simulator.gui.service.GuiService;

/**
 * This class provides the laucher for the gmix simulation gui
 * 
 * @author nachkonvention
 * 
 */
public class GuiLauncher {

	private static Logger logger = Logger.getLogger(GuiLauncher.class);
	private static String LOG_OUTPUT_FOLDER = "./etc/log";
	public static boolean guiActive = false;
	
	/**
	 * Main method - calling this method results in launching the gui
	 * 
	 * @param args
	 *            (not used)
	 */
	public static void main(String[] args) {
		guiActive = true;
		logger.debug("simGUI start.");
		logger.debug("creating folder etc/log if not exists");
		File f = new File (LOG_OUTPUT_FOLDER);
		if (!f.exists() && !f.mkdir()){
			throw new RuntimeException("Failed to create folder "+LOG_OUTPUT_FOLDER+"!. Please check user rights!");
		}
		
		@SuppressWarnings("unused")
		SimPropRegistry simPropRegistry;

		class simPropInitializer implements Callable<SimPropRegistry> {
			@Override
			public SimPropRegistry call() {
				// initial creation of the simulaton property registry
				SimPropRegistry simPropRegistry = SimPropRegistry.getInstance();

				// initial dependency-check for per plugin configurations
				DependencyChecker.checkAll(simPropRegistry);

				return simPropRegistry;
			}
		}

		final ExecutorService service;
		final Future<SimPropRegistry> task;

		service = Executors.newFixedThreadPool(1);
		task = service.submit(new simPropInitializer());

		try {
			// block until finished
			simPropRegistry = task.get();
			// loading of default values (template config)

		} catch (final InterruptedException ex) {
			ex.printStackTrace();
		} catch (final ExecutionException ex) {
			ex.printStackTrace();
		}

		service.shutdownNow();

		try {
			javax.swing.UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Show installed look and feels
		for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
			logger.debug(info);

		}

		GuiService.getInstance();

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// loading initial experiment configuration (template)
				SimPropRegistry simPropRegistry = SimPropRegistry.getInstance();
				File file = new File("inputOutput/simulator/config/experiment_template.cfg");
				SimulationConfigService simulationConfigService = new SimulationConfigService(simPropRegistry);
				simulationConfigService.loadConfig(file);
			}
		});
	}
}

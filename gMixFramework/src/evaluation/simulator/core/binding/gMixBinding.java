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
package evaluation.simulator.core.binding;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.statistics.ResultSet;
import evaluation.simulator.core.statistics.Statistics;
import evaluation.simulator.gui.layout.SimulationTab;
import evaluation.simulator.gui.layout.frames.GraphFrame;
import evaluation.simulator.gui.results.GnuplotPanel;
import evaluation.simulator.gui.results.ResultPanelFactory;
import framework.core.launcher.CommandLineParameters;

/**
 * @author alex
 * 
 */
public class gMixBinding extends Thread {

	private final Logger logger = Logger.getLogger(gMixBinding.class);

	private static gMixBinding instance = null;
	private CommandLineParameters params;
	//private String resultsFileName;
	Statistics stats;
	private static int experimentsPerformed = 0;
	Simulator gMixSim;

	private static boolean stop = false;

	/**
	 * Default constructor
	 */
	private gMixBinding() {

	}

	/**
	 * @param configFile
	 *            String with config file content
	 */
	public void setParams(String[] configFile) {
		this.params = new CommandLineParameters(configFile);
		this.params.useSimGui = true;
	}

	/**
	 * Requests the thread to stop working
	 */
	public void requestStop() {
		stop = true;
	}

	/**
	 * Enables a threads
	 */
	public void enable() {
		stop = false;
	}

	public static boolean shouldStop() {
		return stop;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {

		ResultSet results = null;

		Simulator.reset();

		try {
			sleep(1);
			gMixSim = new Simulator(this.params);
			gMixSim.setBinging(this);
			results = gMixSim.results;
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}

		if (stop) {
			return;
		}

		if (results != null) {
			this.logger.log(Level.INFO, "Finished simulator with results");
		}

	}

	public static void createResult(String gnuplotResultFileName) {
		final JPanel resultPlot = ResultPanelFactory.getGnuplotResultPanel(gnuplotResultFileName);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JTabbedPane resultsTabs = SimulationTab.getInstance().getResultsPanel();
				resultsTabs.addTab("Experiment " + gMixBinding.experimentsPerformed, resultPlot);
				resultsTabs.setSelectedComponent(resultPlot);
				setMaximizeButton(resultsTabs);
				resultPlot.updateUI();
				resultPlot.repaint();
				gMixBinding.experimentsPerformed++;
			}

			private void setMaximizeButton(final JTabbedPane resultsTabs) {
				final int tabIndex = resultsTabs.getSelectedIndex();
				JPanel tabPanel = new JPanel();
				JLabel tabLabel = new JLabel("Experiment " + gMixBinding.experimentsPerformed);
				tabPanel.add(tabLabel);
				final GnuplotPanel tmpGnuplotPanel = (GnuplotPanel) resultPlot;
				JButton maximizeButton = new JButton(new ImageIcon("etc/img/icons/maximize.png"));
				maximizeButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						GraphFrame.getInstance(tmpGnuplotPanel.svgCanvas.getURI(),
								tmpGnuplotPanel.gnuplotResultFileName);

					}
				});
				JButton closeButton = new JButton("x");
				closeButton.setOpaque(false);
				closeButton.setFocusable(false);
				closeButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {

						resultsTabs.remove(tabIndex);
					}
				});
				tabPanel.add(maximizeButton);
				tabPanel.add(closeButton);
				tabPanel.setOpaque(false);
				resultsTabs.setTabComponentAt(tabIndex, tabPanel);
			}

		});
	}

	/**
	 * Singleton
	 * 
	 * @return reference of {@link gMixBinding}
	 */
	public static gMixBinding getInstance() {
		if (instance == null) {
			instance = new gMixBinding();
		}
		return instance;
	}

	/**
	 * Resets the number of performed experiments
	 */
	public void resetExperiments() {
		gMixBinding.experimentsPerformed = 0;
	}

}

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
package evaluation.simulator.gui.layout;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;
import evaluation.simulator.gui.customElements.ConfigChooserPanel;
import evaluation.simulator.gui.layout.frames.ConsoleFrame;

/**
 * Tab for Simulation Control
 * 
 * @author nachkonvention
 * 
 */
@SuppressWarnings("serial")
public class SimulationTab extends JPanel implements ActionListener {

	public HomeTab homeTab = new HomeTab();
	private final JTabbedPane resultsTabs;
	private final JSplitPane splitPane;
	private final JSplitPane rightSplitPane;
	private static SimulationTab instance = null;

	/**
	 * Singleton
	 * 
	 * @return an instance of {@link SimulationTab}
	 */
	public static SimulationTab getInstance() {
		if (instance == null) {
			instance = new SimulationTab();
		}
		return instance;
	}

	/**
	 * Constructor
	 */
	public SimulationTab() {

		MigLayout migLayout = new MigLayout();
		this.setLayout(migLayout);

		this.splitPane = new JSplitPane();
		this.add(this.splitPane, "grow,push");

		this.rightSplitPane = new JSplitPane();
		this.rightSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

		JPanel configChooserPanelInstance = new JPanel(new BorderLayout());
		configChooserPanelInstance.add(ConfigChooserPanel.getInstance(), BorderLayout.CENTER);

		this.splitPane.setLeftComponent(configChooserPanelInstance);
		this.splitPane.setRightComponent(this.rightSplitPane);

		JPanel consolePanel = ConsoleFrame.getInstance().getPanel();
		consolePanel.setBorder(new TitledBorder(null, "Console", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		this.rightSplitPane.setLeftComponent(consolePanel);

		this.resultsTabs = new JTabbedPane(JTabbedPane.VERTICAL);
		this.resultsTabs
				.setBorder(new TitledBorder(null, "Results", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		this.resultsTabs.addTab("Welcome", this.homeTab);
		this.rightSplitPane.setRightComponent(this.resultsTabs);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				SimulationTab.getInstance().getRightSplitPane().setResizeWeight(0.5);
			}
		});

	}

	/**
	 * @return
	 * 		results pane
	 */
	public JTabbedPane getResultsPanel() {
		return this.getResultsTabs();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

	}

	/**
	 * @return
	 * 		splitpane
	 */
	public JSplitPane getSplitPane() {
		return this.splitPane;
	}

	/**
	 * @return
	 * 		right splitpane
	 */
	public JSplitPane getRightSplitPane() {
		return this.rightSplitPane;
	}

	/**
	 * @return
	 * 		results tab
	 */
	public JTabbedPane getResultsTabs() {
		return this.resultsTabs;
	}
}

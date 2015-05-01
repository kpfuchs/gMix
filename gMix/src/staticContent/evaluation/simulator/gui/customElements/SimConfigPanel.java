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

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import staticContent.evaluation.simulator.gui.actionListeners.LoadButtonAction;
import staticContent.evaluation.simulator.gui.actionListeners.ResetButtonAction;
import staticContent.evaluation.simulator.gui.actionListeners.SaveAsButtonAction;
import staticContent.evaluation.simulator.gui.actionListeners.SaveButtonAction;
import staticContent.evaluation.simulator.gui.customElements.accordion.AccordionEntry;

/**
 * Encapsules the whole configuration of the Simulator. Offers the possibility
 * to load, save and reset the config. Holds the {@link AccordionEntry}
 * 
 * @author nachkonvention
 * 
 */
@SuppressWarnings("serial")
public class SimConfigPanel extends JPanel {

	private static SimConfigPanel instance = null;

	public static SimConfigPanel getInstance() {
		if (instance == null) {
			instance = new SimConfigPanel();
		}
		return instance;
	}

	/**
	 * @param enabled
	 *            enables the button
	 */
	public static void setStatusofSaveButton(Boolean enabled) {
		getInstance().saveAsButton.setEnabled(enabled);
	}

	// private Accordion _accordian;
	private JPanel buttonBar;
	private JButton loadButton;

	private PluginPanel pluginPanel;

	private JButton resetButton;
	private JButton saveAsButton;
	private JButton saveButton;

	@SuppressWarnings("deprecation")
	private SimConfigPanel() {
		this.initialize();
		this.resize(this.pluginPanel.getWidth(), this.pluginPanel.getHeight());
	}

	private void initialize() {

		// this._accordian = new Accordion();
		// this.plugInSelection = new PlugInSelection();

		this.pluginPanel = new PluginPanel();

		this.buttonBar = new JPanel();

		this.loadButton = new JButton("Load");
		this.loadButton.addActionListener(new LoadButtonAction());
		this.buttonBar.add(this.loadButton, BorderLayout.SOUTH);
		
		this.saveAsButton = new JButton("Save as");
		this.saveAsButton.addActionListener(new SaveAsButtonAction());
		this.buttonBar.add(this.saveAsButton, BorderLayout.SOUTH);
		
		this.saveButton = new JButton("Save");
		this.saveButton.addActionListener(new SaveButtonAction());
		this.buttonBar.add(this.saveButton, BorderLayout.SOUTH);

		this.resetButton = new JButton("Reset");
		this.resetButton.addActionListener(new ResetButtonAction());
		this.buttonBar.add(this.resetButton, BorderLayout.SOUTH);

		this.setLayout(new BorderLayout());
		// this.add(this.plugInSelection, BorderLayout.NORTH);
		// this.add(this._accordian, BorderLayout.CENTER);

		this.add(this.pluginPanel, BorderLayout.CENTER);
		this.add(this.buttonBar, BorderLayout.SOUTH);

	}

	/**
	 * updates the GUI
	 */
	public void update() {
		this.pluginPanel.update();
		this.updateUI();
	}

	public void foldAccordions() {
		this.pluginPanel.toggleFoldAccordions();
	}

	public void unfoldAccordions() {
		this.pluginPanel.toggleUnfoldAccordions();
	}
}

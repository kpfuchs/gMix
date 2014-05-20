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
package evaluation.simulator.gui.customElements;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * {@link JPanel} putting together {@link SimHelpContentPanel} and
 * {@link SimHelpMenuPanel}
 * 
 * @author nachkonvention
 * 
 */
public class SimHelpPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static SimHelpPanel instance;

	/**
	 * Singleton
	 * 
	 * @return an instance of {@link SimHelpPanel}
	 */
	public static SimHelpPanel getInstance() {
		if (instance == null) {
			instance = new SimHelpPanel();
		}
		return instance;
	}

	private SimHelpPanel() {
		this.initialize();
	}

	private void initialize() {

		this.setLayout(new BorderLayout());
		JSplitPane splitPlane = new JSplitPane();

		SimHelpContentPanel content = SimHelpContentPanel.getInstance();
		SimHelpMenuPanel navigation = SimHelpMenuPanel.getInstance();

		splitPlane.setLeftComponent(navigation);
		splitPlane.setRightComponent(content);
		this.add(splitPlane, BorderLayout.CENTER);
	}

}

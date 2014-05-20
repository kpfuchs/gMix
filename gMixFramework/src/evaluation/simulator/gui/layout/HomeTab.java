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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The Welcome Tab of gMix GUI
 * 
 * @author nachkonvention
 * 
 */
@SuppressWarnings("serial")
public class HomeTab extends JPanel {

	/**
	 * Default Constructor
	 */
	public HomeTab() {

		JLabel welcomeLabel = new JLabel("Welcome to gMixSim");
		welcomeLabel.setFont(new Font("arial", 1, 35));

		JLabel logosLabel = new JLabel(new ImageIcon("etc/img/icons/logoc.png"));

		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.weightx = 0;
		gbc.weighty = 1;
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbl.setConstraints(this, gbc);
		this.setLayout(gbl);

		this.add(welcomeLabel, gbc);
		this.add(logosLabel, gbc);

	}

}

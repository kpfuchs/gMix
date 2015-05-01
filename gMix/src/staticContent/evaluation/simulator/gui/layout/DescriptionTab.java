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
package staticContent.evaluation.simulator.gui.layout;

import java.awt.Color;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JTextArea;

import staticContent.evaluation.simulator.gui.service.DescriptionService;

/**
 * @author alex
 *
 */
@SuppressWarnings("serial")
public class DescriptionTab extends JTextArea implements Observer {

	/**
	 *  Default constructor
	 */
	public DescriptionTab() {
		super();
		DescriptionService ds = DescriptionService.getInstance();
		ds.addObserver(this);
		this.setEditable(false);

		// TODO: find correct bg color!
		// or use another component than JTextArea
		this.setBackground(new Color(220, 218, 213));
	}

	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(Observable o, Object arg) {
		this.setText((String) arg);
	}
}

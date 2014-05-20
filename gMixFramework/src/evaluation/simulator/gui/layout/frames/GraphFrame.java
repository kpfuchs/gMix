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
package evaluation.simulator.gui.layout.frames;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JFrame;

import org.apache.batik.swing.JSVGCanvas;

/**
 * Window for dispatching generated graphs (plotted Simulator output)
 * 
 * @author nachkonvention
 * 
 */
public class GraphFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private static GraphFrame _instance = null;

	/**
	 * @param uri
	 * @param filename
	 */
	public GraphFrame(String uri, String filename) {
		GridBagLayout gridBagLayout = new GridBagLayout();
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.anchor = GridBagConstraints.NORTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagLayout.setConstraints(this, gridBagConstraints);
		this.setLayout(gridBagLayout);

		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		JSVGCanvas svgCanvas = new JSVGCanvas();
		this.add(svgCanvas, gridBagConstraints);
		svgCanvas.setURI(uri);
		this.setSize( new Dimension( 640, 480) );
//		this.pack();
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setTitle(filename);
		this.setVisible(true);
	}

	/**
	 * No Singleton. Kind of factory.
	 * 
	 * @param uri
	 * @param filename
	 * @return
	 */
	public static GraphFrame getInstance(String uri, String filename) {
		_instance = new GraphFrame(uri, filename);
		return _instance;
	}
}

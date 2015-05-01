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
package staticContent.evaluation.simulator.gui.results;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.apache.batik.swing.JSVGCanvas;

import staticContent.evaluation.simulator.gui.customElements.ConfigChooserPanel;
import staticContent.evaluation.simulator.gui.layout.frames.GraphFrame;

/**
 * {@link JPanel} for graph representation after GnuPlot plotting
 * 
 * @author nachkonvention
 * 
 */
@SuppressWarnings("serial")
public class GnuplotPanel extends JPanel {

	public JSVGCanvas svgCanvas;
	public static String outputFolder = "inputOutput/simulator/output/";
	public String gnuplotResultFileName;

	/**
	 * Constructor
	 * 
	 * @param gnuplotResultFileName
	 *            path to generated output file
	 */
	@SuppressWarnings({ "static-access", "deprecation" })
	public GnuplotPanel(final String gnuplotResultFileName) {
		this.gnuplotResultFileName = gnuplotResultFileName;
		// BufferedImage resultsDiagram = null;
		try {
			File f = new File(outputFolder + gnuplotResultFileName);
			while (!f.exists()) {
				Thread.sleep(1);
			}

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
			svgCanvas = new JSVGCanvas();
			this.svgCanvas.addMouseListener(new MouseAdapter() {

				public void mousePressed(MouseEvent e) {
					if (SwingUtilities.isRightMouseButton(e)) {
						JPopupMenu menu = new JPopupMenu();
						JMenuItem item = new JMenuItem("Show in seperate window");
						item.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								GraphFrame.getInstance(svgCanvas.getURI(), gnuplotResultFileName);
							}
						});
						menu.add(item);

						menu.show(svgCanvas, e.getX(), e.getY());
					}
				}
			});
			this.add(svgCanvas, gridBagConstraints);
			svgCanvas.setURI(f.toURL().toString());
			ConfigChooserPanel.getInstance().getExportPictureButton().setEnabled(true);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		;
	}

}

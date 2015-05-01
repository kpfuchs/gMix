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
import java.awt.Color;
import java.io.File;

import javax.swing.JPanel;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xhtmlrenderer.simple.FSScrollPane;
import org.xhtmlrenderer.simple.XHTMLPanel;

/**
 * Creates the {@link JPanel} which offers the representation of a website
 * 
 * @author nachkonvention
 * 
 */
@SuppressWarnings("serial")
public class SimHelpContentPanel extends JPanel {

	private static Logger logger = Logger.getLogger(SimHelpContentPanel.class);

	private static SimHelpContentPanel instance = null;

	/**
	 * Singleton
	 * 
	 * @return an instance of {@link SimHelpContentPanel}
	 */
	public static SimHelpContentPanel getInstance() {
		if (instance == null) {
			instance = new SimHelpContentPanel();
		}
		return instance;
	}

	XHTMLPanel _htmlContent;

	private SimHelpContentPanel() {
		this.initialize();
	}

	private void initialize() {

		try {
			this._htmlContent = new XHTMLPanel();
			this.setLayout(new BorderLayout());
			this.setBackground(Color.WHITE);
			this._htmlContent.setDocument(new File("etc/html/index.html"));

			this.add(new FSScrollPane(this._htmlContent));
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	/**
	 * Load a html file
	 * @param url
	 *            the URL to load represented as {@link String}
	 */
	public void loadURL(String url) {
		logger.log(Level.DEBUG, "Loading help-page: " + url);
		try {
			this._htmlContent.setDocument(new File(url));
		} catch (Exception e) {
			logger.error("Probably no stylesheet defined... processing.");
		}
	}
}

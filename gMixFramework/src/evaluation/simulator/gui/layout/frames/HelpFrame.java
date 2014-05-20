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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

import evaluation.simulator.conf.service.UserConfigService;
import evaluation.simulator.gui.customElements.SimHelpPanel;
import evaluation.simulator.gui.service.GuiService;
// import log.Level;
// import log.Logger;

/**
 * Window showing the {@link SimHelpPanel} when being seperated
 * 
 * @author nachkonvention
 * 
 */
@SuppressWarnings("serial")
public class HelpFrame extends JFrame {

	private static HelpFrame instance = null;
	private JPanel panel;

	/**
	 * Singleton
	 * 
	 * @return an instance of {@link HelpFrame}
	 */
	public static HelpFrame getInstance() {
		if (instance == null) {
			instance = new HelpFrame();
		}
		return instance;
	}

	private int helpFrameHeight;
	private int helpFrameWidth;
	private int helpFrameXPos;
	private int helpFrameYPos;

	private HelpFrame() {

		this.initialize();

		this.addWindowListener(new WindowListener() {

			@Override
			public void windowActivated(WindowEvent arg0) {
			}

			@Override
			public void windowClosed(WindowEvent arg0) {
				HelpFrame.this.safeProperties();
			}

			@Override
			public void windowClosing(WindowEvent arg0) {
				HelpFrame.this.safeProperties();
				GuiService.getInstance().toogleHelpTools();
			}

			@Override
			public void windowDeactivated(WindowEvent arg0) {
				HelpFrame.this.safeProperties();
			}

			@Override
			public void windowDeiconified(WindowEvent arg0) {
			}

			@Override
			public void windowIconified(WindowEvent arg0) {
			}

			@Override
			public void windowOpened(WindowEvent arg0) {
			}
		});
	}

	/**
	 * @return the panel
	 */
	public JPanel getPanel() {
		return this.panel;
	}

	/**
	 * Initializes the HelpFrame instance
	 */
	public void initialize() {

		this.loadProperties();
		this.getContentPane().setLayout(new BorderLayout());

		this.setPanel(SimHelpPanel.getInstance());
		this.add(this.getPanel());

		this.setTitle("Help Tool");
		this.setIconImage(Toolkit.getDefaultToolkit().createImage("etc/img/icons/icon128.png"));

		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setPreferredSize(new Dimension(this.helpFrameWidth, this.helpFrameHeight));
		this.pack();
		this.setVisible(false);

	}

	private void safeProperties() {
		UserConfigService.setHELPFRAME_HEIGHT(this.getHeight());
		UserConfigService.setHELPFRAME_WIDTH(this.getWidth());
		UserConfigService.setHELPFRAME_XPOS(this.getX());
		UserConfigService.setHELPFRAME_YPOS(this.getY());
	}

	private void loadProperties() {
		this.helpFrameXPos = UserConfigService.getHELPFRAME_XPOS();
		this.helpFrameYPos = UserConfigService.getHELPFRAME_XPOS();
		this.helpFrameWidth = UserConfigService.getHELPFRAME_WIDTH();
		this.helpFrameHeight = UserConfigService.getHELPFRAME_HEIGHT();

	}

	/**
	 * set the {@link SimHelpPanel}
	 * 
	 * @param panel
	 *            the panel
	 */
	public void setPanel(JPanel panel) {
		this.panel = panel;
	}

}

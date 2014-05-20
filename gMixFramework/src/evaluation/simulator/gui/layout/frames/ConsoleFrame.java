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
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

import evaluation.simulator.conf.service.UserConfigService;
import evaluation.simulator.gui.customElements.ConsolePanel;

/**
 * Window for Console
 * This is implemented as a singleton
 * 
 * @author nachkonvention
 * 
 */
public class ConsoleFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private static ConsoleFrame instance = null;
	private int consoleFrameHeight;
	private int consoleFrameWidth;
	private int consoleFrameXPos;
	private int consoleFrameYPos;
	private ConsolePanel simConsolePanel;
	private JPanel panel;

	/**
	 * 
	 * @return an instance of {@link ConsoleFrame}
	 */

	public static ConsoleFrame getInstance() {
		if (instance == null) {
			instance = new ConsoleFrame();
		}
		return instance;
	}

	/**
	 * @return the panel
	 */
	public JPanel getPanel() {
		return this.panel;
	}

	private void initialize() {
		this.panel = new JPanel();

		this.panel.setLayout(new BorderLayout());
		this.simConsolePanel = ConsolePanel.getInstance();
		this.panel.add(this.simConsolePanel, BorderLayout.CENTER);
		this.add(this.panel);

		this.consoleFrameXPos = UserConfigService.getCONSOLEFRAME_XPOS();
		this.consoleFrameYPos = UserConfigService.getCONSOLEFRAME_YPOS();
		this.consoleFrameWidth = UserConfigService.getCONSOLEFRAME_WIDTH();
		this.consoleFrameHeight = UserConfigService.getCONSOLEFRAME_HEIGHT();

		this.setBounds(this.consoleFrameXPos, this.consoleFrameYPos, this.consoleFrameWidth, this.consoleFrameHeight);
	}

	private void safeProperties() {
		UserConfigService.setCONSOLEFRAME_HEIGHT(this.getHeight());
		UserConfigService.setCONSOLEFRAME_WIDTH(this.getWidth());
		UserConfigService.setCONSOLEFRAME_XPOS(this.getX());
		UserConfigService.setCONSOLEFRAME_YPOS(this.getY());
	}

	private ConsoleFrame() {

		this.initialize();

		this.setTitle("Console");
		this.setIconImage(Toolkit.getDefaultToolkit().createImage("etc/img/icons/icon128.png"));

		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setVisible(false);

		this.addWindowListener(new WindowListener() {

			@Override
			public void windowActivated(WindowEvent arg0) {
			}

			@Override
			public void windowClosed(WindowEvent arg0) {
				ConsoleFrame.this.safeProperties();
			}

			@Override
			public void windowClosing(WindowEvent arg0) {
				ConsoleFrame.this.safeProperties();
				// GuiService.getInstance().toggleConsole();
			}

			@Override
			public void windowDeactivated(WindowEvent arg0) {
				ConsoleFrame.this.safeProperties();
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

}

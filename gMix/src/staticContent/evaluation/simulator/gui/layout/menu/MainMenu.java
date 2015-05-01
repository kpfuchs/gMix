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
package staticContent.evaluation.simulator.gui.layout.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import staticContent.evaluation.simulator.gui.service.GuiService;

/**
 * @author alex
 *
 */
@SuppressWarnings("serial")
public class MainMenu extends JMenuBar {

	private static MainMenu _instance = null;

	public static MainMenu getInstance() {
		if (_instance == null) {
			_instance = new MainMenu();
		}
		return _instance;
	}

	private final JMenuItem _about;
	private final JMenuItem _close;

	private final JMenuItem _faq;
	private final JMenu _fileMenu;
	private final JMenu _helpMenu;
	private final JMenuItem _open;
	private final JMenuItem _preferences;
	private final JMenuItem _seperateConfigTool;
	private final JMenuItem _seperateHelpTool;

	private final JMenu _windowMenu;

	private MainMenu() {

		this._fileMenu = new JMenu("File");
		this._windowMenu = new JMenu("Window");
		this._helpMenu = new JMenu("Help");

		this._open = new JMenuItem("Open");
		this._close = new JMenuItem("Exit");

		this._preferences = new JMenuItem("Preferences");
		this._seperateConfigTool = new JMenuItem("Separate Configuration Tool");
		this._seperateConfigTool.setMnemonic('C');
		this._seperateConfigTool.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_C, java.awt.Event.CTRL_MASK));

		this._seperateHelpTool = new JMenuItem("Separate Help Tool");
		this._seperateHelpTool.setMnemonic('H');
		this._seperateHelpTool.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_H, java.awt.Event.CTRL_MASK));

		this._faq = new JMenuItem("F.A.Q.");
		this._about = new JMenuItem("About");

		this._fileMenu.add(this._open);
		this._fileMenu.addSeparator();
		this._fileMenu.add(this._close);

		this._windowMenu.add(this._preferences);
		this._windowMenu.add(this._seperateConfigTool);
		this._windowMenu.add(this._seperateHelpTool);

		this._helpMenu.add(this._faq);
		this._helpMenu.add(this._about);

		this.add(this._fileMenu);
		this.add(this._windowMenu);
		this.add(this._helpMenu);

		// action listeners
		this._seperateConfigTool.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				GuiService.getInstance().toogleConfTools();
			}
		});
		this._seperateHelpTool.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				GuiService.getInstance().toogleHelpTools();
			}
		});
	}

}

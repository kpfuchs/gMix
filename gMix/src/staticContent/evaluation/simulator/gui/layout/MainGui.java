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

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import staticContent.evaluation.simulator.conf.service.UserConfigService;
import staticContent.evaluation.simulator.gui.actionListeners.ClearButtonAction;
import staticContent.evaluation.simulator.gui.actionListeners.LoadButtonAction;
import staticContent.evaluation.simulator.gui.actionListeners.SaveAsButtonAction;
import staticContent.evaluation.simulator.gui.actionListeners.StartButtonAction;
import staticContent.evaluation.simulator.gui.actionListeners.StopButtonAction;
import staticContent.evaluation.simulator.gui.customElements.ConfigChooserPanel;
import staticContent.evaluation.simulator.gui.customElements.SimConfigPanel;
import staticContent.evaluation.simulator.gui.customElements.SimHelpPanel;
import staticContent.evaluation.simulator.gui.layout.frames.HelpFrame;
import staticContent.evaluation.simulator.gui.layout.frames.ToolFrame;
import staticContent.evaluation.simulator.gui.service.GuiService;

/**
 * Putting together all GUI elementes like configuration, menu and tabs
 * 
 * @author nachkonvention
 * 
 */
@SuppressWarnings("serial")
public class MainGui extends JFrame {

	private static MainGui instance = null;

	/**
	 * Singleton
	 * 
	 * @return an instance of {@link MainGui}
	 */
	public static MainGui getInstance() {
		if (instance == null) {
			instance = new MainGui();
		}
		return instance;
	}

	/*
	 * neue Elemente:
	 */
	private JFrame frame;
	public JSplitPane splitPane;
	public JTabbedPane tabbedPane;
	private HelpFrame helpFrame;

	/*
	 * *********************************************************
	 */

	private int consoleHeight;

	public boolean homeTabStatus;
	private int horizontalSplitPlaneDeviderLocation;

	private int mainGuiHeight;
	private int mainGuiWidth;
	private int mainGuiXPos;
	private int mainGuiYPos;

	/**
	 * Constructor
	 */
	private MainGui() {
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.initialize();
	}

	private void initialize() {

		this.mainGuiXPos = UserConfigService.getMAINGUI_XPOS();
		this.mainGuiYPos = UserConfigService.getMAINGUI_YPOS();
		this.mainGuiWidth = UserConfigService.getMAINGUI_WIDTH();
		this.mainGuiHeight = UserConfigService.getMAINGUI_HEIGHT();
		this.horizontalSplitPlaneDeviderLocation = UserConfigService.getMAINGUI_HSPLIT_DEVIDER_LOCATION();
		this.consoleHeight = UserConfigService.getMAINGUI_CONSOLE_HEIGHT();

		this.frame = new JFrame();
		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.frame.addWindowListener(new WindowListener() {

			@Override
			public void windowClosing(WindowEvent arg0) {
				MainGui.this.onClose();
			}

			@Override
			public void windowActivated(WindowEvent e) {

			}

			@Override
			public void windowClosed(WindowEvent e) {

			}

			@Override
			public void windowDeactivated(WindowEvent e) {

			}

			@Override
			public void windowDeiconified(WindowEvent e) {

			}

			@Override
			public void windowIconified(WindowEvent e) {

			}

			@Override
			public void windowOpened(WindowEvent e) {
			}

		});

		this.splitPane = new JSplitPane();
		this.frame.getContentPane().add(this.splitPane, BorderLayout.CENTER);

		this.splitPane.setLeftComponent(ToolFrame.getInstance().getPanel());

		this.tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		this.helpFrame = HelpFrame.getInstance();
		this.tabbedPane.addTab("Simulator", SimulationTab.getInstance());
		this.splitPane.setRightComponent(this.tabbedPane);

		JMenuBar menuBar = new JMenuBar();
		this.frame.setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		mnFile.setMnemonic('F');
		menuBar.add(mnFile);

		JMenuItem menuItemOpen = new JMenuItem("Open");
		menuItemOpen.addActionListener(new LoadButtonAction());
		menuItemOpen.setMnemonic('O');
		mnFile.add(menuItemOpen);
		JMenuItem menuItemSave = new JMenuItem("Save as");
		menuItemSave.addActionListener(new SaveAsButtonAction());
		menuItemSave.setMnemonic('S');
		mnFile.add(menuItemSave);
		JMenuItem menuItemExit = new JMenuItem("Exit");
		menuItemExit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				MainGui.this.onClose();
			}
		});
		menuItemExit.setMnemonic('X');

		mnFile.add(menuItemExit);

		JMenu mnEdit = new JMenu("Edit");
		mnEdit.setMnemonic('E');

		menuBar.add(mnEdit);

		JMenuItem menuItemStart = new JMenuItem("Start Simulation");
		menuItemStart.addActionListener(new StartButtonAction(ConfigChooserPanel.getInstance().getConfigList()));
		menuItemStart.setMnemonic('S');

		mnEdit.add(menuItemStart);
		JMenuItem menuItemStop = new JMenuItem("Stop Simulation");
		menuItemStop.addActionListener(new StopButtonAction());
		menuItemStop.setMnemonic('T');

		mnEdit.add(menuItemStop);
		JMenuItem menuItemClear = new JMenuItem("Clear Results");
		menuItemClear.addActionListener(new ClearButtonAction());
		menuItemClear.setMnemonic('C');

		mnEdit.add(menuItemClear);

		JMenu mnWindow = new JMenu("Window");
		mnWindow.setMnemonic('W');
		menuBar.add(mnWindow);

		JMenu mnHelp = new JMenu("Help");
		mnHelp.setMnemonic('H');
		menuBar.add(mnHelp);

		JMenuItem mntmShowhideHelp = new JMenuItem("De-/Seperate Help");
		mntmShowhideHelp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GuiService.getInstance().toogleHelpTools();
			}
		});
		mnHelp.setMnemonic('D');
		mnHelp.add(mntmShowhideHelp);

		JMenuItem mntmSeperateConfiguration = new JMenuItem("De-/Seperate Configuration");
		mntmSeperateConfiguration.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GuiService.getInstance().toogleConfTools();
			}
		});
		mntmSeperateConfiguration.setMnemonic('D');
		mnWindow.add(mntmSeperateConfiguration);

		this.frame.setTitle("gMixSim");
		this.frame.setIconImage(Toolkit.getDefaultToolkit().createImage("etc/img/icons/icon128.png"));

		SystemTray tray = SystemTray.getSystemTray();
		try {
			tray.add(new TrayIcon(Toolkit.getDefaultToolkit().createImage("etc/img/icons/icon16.png")));
		} catch (AWTException e) {
			e.printStackTrace();
		}

		this.frame.setBounds(this.mainGuiXPos, this.mainGuiYPos, this.mainGuiWidth, this.mainGuiHeight);

		this.frame.setVisible(true);
	}

	/**
	 * Close the GUI
	 */
	public void onClose() {
		/*try {
			IOActions.cleanOutputFolder();
		} catch (IOException e1) {
			JOptionPane.showMessageDialog(null, "Could not clean up Output directory " + GnuplotPanel.outputFolder,
					"Cleanup Error", JOptionPane.ERROR_MESSAGE);
		}*/
		MainGui.this.safeProperties();
		System.exit(0);
	}

	private void safeProperties() {
		UserConfigService.setMAINGUI_CONSOLE_HEIGHT(this.consoleHeight);
		UserConfigService.setMAINGUI_HEIGHT(this.frame.getHeight());
		UserConfigService.setMAINGUI_HSPLIT_DEVIDER_LOCATION(this.horizontalSplitPlaneDeviderLocation);
		UserConfigService.setMAINGUI_WIDTH(this.frame.getWidth());
		UserConfigService.setMAINGUI_XPOS(this.frame.getX());
		UserConfigService.setMAINGUI_YPOS(this.frame.getY());
	}

	/**
	 * De/Separate the {@link SimConfigPanel} into the {@link ToolFrame}
	 * 
	 * @param b
	 *            separate or not
	 */
	public void toogleConfTool(boolean b) {
		if (b) {
			this.splitPane.setLeftComponent(ToolFrame.getInstance().getPanel());
		} else {
			this.splitPane.remove(ToolFrame.getInstance().getPanel());
		}
	}

	/**
	 * De/Seperate the {@link SimHelpPanel} into the {@link HelpFrame}
	 * 
	 * @param b
	 *            separate or not
	 */
	public void toogleHelpTool(boolean b) {
		if (b) {
			this.tabbedPane.addTab("Help", this.helpFrame.getPanel());
		} else {
			this.tabbedPane.remove(this.helpFrame.getPanel());
		}
	}

}

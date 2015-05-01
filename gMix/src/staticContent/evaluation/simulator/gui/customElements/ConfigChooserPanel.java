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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import staticContent.evaluation.simulator.conf.service.SimulationConfigService;
import staticContent.evaluation.simulator.core.binding.gMixBinding;
import staticContent.evaluation.simulator.gui.actionListeners.ClearButtonAction;
import staticContent.evaluation.simulator.gui.actionListeners.ExportButtonAction;
import staticContent.evaluation.simulator.gui.actionListeners.StartButtonAction;
import staticContent.evaluation.simulator.gui.actionListeners.StopButtonAction;
import staticContent.evaluation.simulator.gui.pluginRegistry.SimPropRegistry;
import staticContent.evaluation.simulator.gui.service.GuiService;
import net.miginfocom.swing.MigLayout;

/**
 * {@link JPanel} containing Simulator configuration including config selection,
 * start/stop buttons, export functionality and a {@link JProgressBar}.
 * 
 * @author nachkonvention
 */
public class ConfigChooserPanel extends JPanel {

	private static final long serialVersionUID = 8399323524494928469L;
	private JList<File> configList;
	private DefaultListModel<File> listModel;
	JButton startButton;
	JButton stopButton;
	private static JProgressBar progressBar;

	JButton leftButton = new JButton("<");

	private static JButton exportPictureButton;
	private static gMixBinding callSimulation;
	private JButton clearButton;

	private static ConfigChooserPanel instance = null;

	/**
	 * Constructor
	 */
	public ConfigChooserPanel() {

		this.initialize();

	}

	/**
	 * Singleton
	 * 
	 * @return the instance of {@link ConfigChooserPanel}
	 */
	public static ConfigChooserPanel getInstance() {
		if (instance == null) {
			instance = new ConfigChooserPanel();
		}
		return instance;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {

		JPanel configurationSelectionPanel = this.createConfigSelectionPanel();
		JPanel simulationControlPanel = this.createSimulationControlPanel();
		JPanel exportResultsPanel = this.createExportResultsPanel();
		JPanel statusPanel = this.createStatusPanel();

		MigLayout migLayout = new MigLayout("", "[grow]",
				"[grow][grow][grow][grow]");
		this.setLayout(migLayout);
		this.add(configurationSelectionPanel, "cell 0 0,grow");
		// this.add(additionalPlotOptionsPanel, "cell 0 1,growx");
		this.add(simulationControlPanel, "cell 0 1,growx");
		this.add(exportResultsPanel, "cell 0 2,growx");
		this.add(statusPanel, "cell 0 3,growx");

		// Read names of existing experiment configurations

		updateConfigDirectory();

	}

	/**
	 * updates the available experiment configs in the config selection
	 */
	public void updateConfigDirectory() {
		final File folder = new File("inputOutput/simulator/experimentDefinitions");
		final File[] listOfFiles = folder.listFiles(new FileFilter() {

			@Override
			public boolean accept(File f) {
				return f.getName().toLowerCase().endsWith(".cfg");
			}
		});

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				for (File f : listOfFiles) {
					boolean insertFlag = true;
					for (int i = 0; i < ConfigChooserPanel.getInstance()
							.getConfigList().getModel().getSize(); i++) {
						if (ConfigChooserPanel.getInstance().getConfigList()
								.getModel().getElementAt(i).equals(f)) {
							insertFlag = false;
							break;
						}
					}

					for (int i = 0; i < ConfigChooserPanel.getInstance()
							.getConfigList().getModel().getSize(); i++) {
						if (ConfigChooserPanel.getInstance().getConfigList()
								.getModel().getElementAt(i).equals(f)) {
							insertFlag = false;
							break;
						}
					}

					if (insertFlag) {
						ConfigChooserPanel.getInstance().getListModel()
								.addElement(f);
					}
				}
			}
		});
		this.getConfigList().repaint();
	}

	private JPanel createStatusPanel() {
		MigLayout migLayout = new MigLayout("", "[grow]", "[grow]");
		JPanel panel = new JPanel(migLayout);

		setProgressBar(new JProgressBar(0, 100));
		getProgressBar().setIndeterminate(true);
		getProgressBar().setVisible(false);
		panel.add(getProgressBar(), "growx,push");
		panel.setBorder(new TitledBorder(null, "Simulation Status",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		return panel;
	}

	private JPanel createExportResultsPanel() {

		MigLayout migLayout = new MigLayout("", "[grow][]", "[grow]");
		JPanel panel = new JPanel(migLayout);

		ConfigChooserPanel.setExportPictureButton(new JButton("Export Graph"));
		ConfigChooserPanel.getExportPictureButton().setMnemonic('E');
		getExportPictureButton().addActionListener(new ExportButtonAction());
		getExportPictureButton().setEnabled(false);

		panel.add(ConfigChooserPanel.getExportPictureButton(), "cell 0 1,growx");

		panel.setBorder(new TitledBorder(null, "Export Results",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		return panel;

	}

	private JPanel createSimulationControlPanel() {

		MigLayout migLayout = new MigLayout("", "[grow]", "[grow]");
		JPanel panel = new JPanel(migLayout);

		this.startButton = new JButton("Start Simulation");
		this.startButton.setMnemonic('S');

		this.startButton.addActionListener(new StartButtonAction(
				getConfigList()));

		this.stopButton = new JButton("Stop Simulation");
		this.stopButton.setMnemonic('T');

		this.stopButton.addActionListener(new StopButtonAction());

		this.clearButton = new JButton("Clear Results");
		this.clearButton.setMnemonic('C');
		this.clearButton.addActionListener(new ClearButtonAction());

		panel.add(this.startButton, "cell 0 0,growx");
		panel.add(this.stopButton, "cell 0 1,growx");
		panel.add(this.clearButton, "cell 0 2,growx");

		panel.setBorder(new TitledBorder(null, "Simulation Control",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		return panel;

	}

	private JPanel createConfigSelectionPanel() {

		MigLayout migLayout = new MigLayout("", "[shrink][grow]", "[grow]");
		JPanel panel = new JPanel(migLayout);
		this.setListModel(new DefaultListModel<File>());
		this.setConfigList(new JList<File>(this.getListModel()));
		this.getConfigList().setSelectionMode(
				ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		final int[] selection = getConfigList().getSelectedIndices();

		this.getConfigList().addMouseListener(new MouseAdapter() {

			public void mousePressed(MouseEvent e) {
				final int hoverIndex = getConfigList().locationToIndex(
						e.getPoint());
				if (SwingUtilities.isRightMouseButton(e)) {
					JPopupMenu menu = new JPopupMenu();
					JMenuItem item = new JMenuItem("Load into config tool");
					item.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							getLocale();
							JOptionPane.setDefaultLocale(Locale.ENGLISH);
							int reply = JOptionPane.showConfirmDialog(
									GuiService.getInstance().getMainGui(),
									"Do you want to overwrite your changes?",
									"Overwrite Changes",
									JOptionPane.YES_NO_OPTION);
							if (reply == JOptionPane.YES_OPTION) {
								File file = getConfigList().getModel()
										.getElementAt(hoverIndex);
								SimPropRegistry simPropRegistry = SimPropRegistry
										.getInstance();
								SimulationConfigService simulationConfigService = new SimulationConfigService(
										simPropRegistry);
								simulationConfigService.loadConfig(file);
							}
						}
					});
					JMenuItem addItem = new JMenuItem("Add to selection");
					addItem.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							Set<Integer> newSelection = new HashSet<Integer>();
							for (int i : selection) {
								newSelection.add(i);
							}
							newSelection.add(hoverIndex);
							int[] newIndexSelection = new int[newSelection
									.size()];
							int i = 0;
							for (int each : newSelection) {
								newIndexSelection[i] = each;
								i++;
							}
							getConfigList().setSelectedIndices(
									newIndexSelection);
						}
					});
					JMenuItem delItem = new JMenuItem("Delete from Selection");
					delItem.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							Set<Integer> newSelection = new HashSet<Integer>();
							for (int i : selection) {
								newSelection.add(i);
							}
							newSelection.remove(hoverIndex);
							int[] newIndexSelection = new int[newSelection
									.size()];
							int i = 0;
							for (int each : newSelection) {
								newIndexSelection[i] = each;
								i++;
							}
							getConfigList().setSelectedIndices(
									newIndexSelection);
						}
					});
					menu.add(item);

					boolean selected = false;
					for (int i = 0; i < selection.length; i++) {
						if (selection[i] == hoverIndex)
							selected = true;
					}

					if (!selected) {
						menu.add(addItem);
					} else {
						menu.add(delItem);
					}
					menu.show(getConfigList(), e.getX(), e.getY());
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(this.getConfigList());

		leftButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (getConfigList().getSelectedIndices().length > 1) {
					JOptionPane
							.showMessageDialog(
									GuiService.getInstance().getMainGui(),
									"Please select only one config to load it into Configuration View.",
									"Selection Error",
									JOptionPane.INFORMATION_MESSAGE);
				} else {
					if (getConfigList().getSelectedIndices().length != 0) {
						if (SimPropRegistry.getInstance().getUnsavedChanges()) {
							int backValue = JOptionPane
									.showConfirmDialog(
											GuiService.getInstance()
													.getMainGui(),
											"There are unsaved changes! They will get lost if you load a new configuration. Do you want to discard changes and load a new configuration?",
											"Unsaved changes",
											JOptionPane.YES_NO_OPTION);
							if (backValue == JOptionPane.YES_OPTION) {
								load();
							}
						} else {
							load();
						}
					}
				}
			}

			private void load() {
				File file = getConfigList().getSelectedValue();
				SimPropRegistry simPropRegistry = SimPropRegistry.getInstance();
				SimulationConfigService simulationConfigService = new SimulationConfigService(
						simPropRegistry);
				simulationConfigService.loadConfig(file);
			}
		});

		panel.add(leftButton, "cell 0 0,growy,width 10px");
		panel.add(scrollPane, "cell 1 0,growx,growy, push");
		panel.add(new JLabel("* Multiple selection is possible"),
				"cell 1 1,growx,growy");

		panel.setBorder(new TitledBorder(null, "Configuration Selection",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		return panel;

	}

	/**
	 * @return config list (list of files)
	 */
	public JList<File> getConfigList() {
		return configList;
	}

	/**
	 * @param configList
	 */
	public void setConfigList(JList<File> configList) {
		this.configList = configList;
	}

	/**
	 * @return list model
	 */
	public DefaultListModel<File> getListModel() {
		return listModel;
	}

	/**
	 * @param listModel
	 */
	public void setListModel(DefaultListModel<File> listModel) {
		this.listModel = listModel;
	}

	/**
	 * @return the export picture button
	 */
	public static JButton getExportPictureButton() {
		return exportPictureButton;
	}

	/**
	 * @param exportPictureButton
	 */
	public static void setExportPictureButton(JButton exportPictureButton) {
		ConfigChooserPanel.exportPictureButton = exportPictureButton;
	}

	/**
	 * @return simulation call
	 */
	public static gMixBinding getCallSimulation() {
		return callSimulation;
	}

	/**
	 * @param callSimulation
	 */
	public static void setCallSimulation(gMixBinding callSimulation) {
		ConfigChooserPanel.callSimulation = callSimulation;
	}

	/**
	 * @return the progress bar
	 */
	public static JProgressBar getProgressBar() {
		return progressBar;
	}

	/**
	 * Sets the progress bar
	 * 
	 * @param progressBar
	 */
	public static void setProgressBar(JProgressBar progressBar) {
		ConfigChooserPanel.progressBar = progressBar;
	}

}

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
package evaluation.simulator.gui.customElements.accordion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import evaluation.simulator.gui.actionListeners.FoldActionListener;
import evaluation.simulator.gui.actionListeners.UnfoldActionListener;
import evaluation.simulator.gui.customElements.PluginPanel;
import evaluation.simulator.gui.pluginRegistry.SimPropRegistry;

/**
 * implements the Accordion like elements in the {@link PluginPanel} for plugin
 * selection and property configuration
 * 
 * @author nachkonvention
 */
@SuppressWarnings("serial")
public class AccordionEntry extends JPanel {

	private static Logger logger = Logger.getLogger(AccordionEntry.class);

	private final JComboBox<String> comboBox;
	private final JLabel hintLabel = new JLabel("Plugin:");
	private boolean fresh;
	private final JButton entryButton;
	private final String localName;
	private final PropertyPanel propertyPanel;
	private final Map<String, String> model;

	/**
	 * @param name
	 *            the name of the entry
	 * @param jComboBox
	 *            the JComboBox offering the plugin selection
	 * @param map
	 *            Map holding layer as key and plugin as value
	 */
	public AccordionEntry(String name, JComboBox<String> jComboBox, Map<String, String> map) {
		this.localName = name;
		this.model = map;

		if (jComboBox == null) {
			logger.log(Level.ERROR, "jComboBox == null");
		}
		this.comboBox = jComboBox;
		this.fresh = true;

		this.setLayout(new BorderLayout(0, 0));
		this.setBorder(new TitledBorder(null, "", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		this.entryButton = new JButton(this.localName, new ImageIcon("etc/img/icons/green/arrow-144-24.png"));
		this.entryButton.setForeground(Color.BLACK);
		this.entryButton.setHorizontalAlignment(SwingConstants.LEFT);
		this.entryButton.setHorizontalTextPosition(SwingConstants.RIGHT);

		this.entryButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					JPopupMenu menu = new JPopupMenu();
					JMenuItem itemFold = new JMenuItem("Fold All");
					itemFold.addActionListener(new FoldActionListener());
					JMenuItem itemUnfold = new JMenuItem("Unfold All");
					itemUnfold.addActionListener(new UnfoldActionListener());

					menu.add(itemFold);
					menu.add(itemUnfold);

					menu.show(entryButton, e.getX(), e.getY());
				} else {
					if (AccordionEntry.this.comboBox.isVisible()) {
						AccordionEntry.this.setVibility(false);
					} else {
						AccordionEntry.this.setVibility(true);
					}
				}
			}
		});
		this.entryButton.setAlignmentX(Component.LEFT_ALIGNMENT);

		this.add(this.entryButton, BorderLayout.NORTH);
		this.comboBox.setVisible(false);
		this.comboBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {

				if (e.getStateChange() == ItemEvent.SELECTED && AccordionEntry.this.comboBox.isVisible()) {
					AccordionEntry.this.comboBoxChanged(AccordionEntry.this.comboBox);
				}

			}
		});
		this.comboBox.setPrototypeDisplayValue("xxxxxxxxxxxxxxxxxxx");

		if (this.comboBox.getModel().getSize() > 1) {
			this.add(hintLabel, BorderLayout.WEST);
			this.add(this.comboBox, BorderLayout.CENTER);
		}

		propertyPanel = new PropertyPanel(this.localName);
		propertyPanel.setVisible(false);
		this.add(propertyPanel, BorderLayout.SOUTH);

	}

	private void comboBoxChanged(JComboBox<String> jComboBox) {

		logger.log(Level.DEBUG, "Reload table");
		SimPropRegistry simPropRegistry = SimPropRegistry.getInstance();

		String pluginLevel = this.localName;
		String pluginName = (String) jComboBox.getSelectedItem();

		logger.log(Level.DEBUG, "Set plugin-level " + pluginLevel + " to " + this.model.get(pluginName));
		simPropRegistry.setActivePlugins(pluginLevel, this.model.get(pluginName)); 

		if (this.fresh == true) {
			fresh = false;
			jComboBox.removeItemAt(0);
		}

		propertyPanel.realoadContent(this.model.get(pluginName));
		propertyPanel.setVisible(true);

		this.updateUI();

	}

	/**
	 * folds/unfolds the {@link AccordionEntry}.
	 * 
	 * @param visibility
	 */
	public void setVibility(boolean visibility) {
		if (visibility) {
			entryButton.setIcon(new ImageIcon("etc/img/icons/green/arrow-144-24.png"));
		} else {
			entryButton.setIcon(new ImageIcon("etc/img/icons/red/arrow-144-24.png"));
		}
		propertyPanel.setVisible(visibility);
		hintLabel.setVisible(visibility);
		comboBox.setVisible(visibility);
		this.repaint();
	}
}

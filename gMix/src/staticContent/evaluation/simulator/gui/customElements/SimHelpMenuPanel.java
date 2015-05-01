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
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.ldap.UnsolicitedNotificationEvent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import org.apache.log4j.Logger;

import staticContent.evaluation.simulator.annotations.property.SimProp;
import staticContent.evaluation.simulator.conf.service.UserConfigService;
import staticContent.evaluation.simulator.gui.customElements.structure.HelpTreeNode;
import staticContent.evaluation.simulator.gui.helper.ValueComparator;
import staticContent.evaluation.simulator.gui.pluginRegistry.SimPropRegistry;

/**
 * Builds the menu for the {@link SimHelpContentPanel}
 * 
 * @author nachkonvention
 * 
 */
@SuppressWarnings("serial")
public class SimHelpMenuPanel extends JPanel implements TreeSelectionListener {

	private static Logger logger = Logger.getLogger(SimHelpMenuPanel.class);

	private static SimHelpMenuPanel instance = null;

	private static String path = "etc/html/plugins/";
	private static Map<String, String> layerMapDisplayNameToConfigName;
	private static Map<String, SimProp> propertyMap;
	private static Map<String, String> registeredPlugins;

	private static JTree tree;

	/**
	 * Singleton
	 * 
	 * @return an instance of {@link SimHelpMenuPanel}
	 */
	public static SimHelpMenuPanel getInstance() {
		if (instance == null) {
			instance = new SimHelpMenuPanel();
		}
		return instance;
	}

	private SimHelpMenuPanel() {
		this.initialize();
	}

	private void initialize() {

		DefaultMutableTreeNode top = new DefaultMutableTreeNode("gMixSim Help");
		createNodes(top);
		tree = new JTree(top);

		tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);

		tree.addTreeSelectionListener(this);

		this.setLayout(new BorderLayout());
		this.setBackground(Color.WHITE);

		this.add(new JScrollPane(tree), BorderLayout.CENTER);
	}

	private void createNodes(DefaultMutableTreeNode top) {
		DefaultMutableTreeNode category = null;
		DefaultMutableTreeNode node = null;

		layerMapDisplayNameToConfigName = SimPropRegistry.getInstance()
				.getLayerMapDisplayNameToConfigName();
		propertyMap = SimPropRegistry.getInstance().getProperties();
		registeredPlugins = SimPropRegistry.getInstance()
				.getRegisteredPlugins();

		Map<String, Integer> layerMap = SimPropRegistry.getInstance()
				.getLayerMapDisplayNameToOrder();
		ValueComparator comperatorLayer = new ValueComparator(layerMap);
		TreeMap<String, Integer> sortedLayerMap = new TreeMap<String, Integer>(
				comperatorLayer);
		sortedLayerMap.putAll(layerMap);

		category = new DefaultMutableTreeNode("Videotutorials");
		top.add(category);
		category.add(new DefaultMutableTreeNode(new HelpTreeNode(
				"Load and Start", "http://www.youtube.com/watch?v=cVH1mCc5EvU")));
		category.add(new DefaultMutableTreeNode(new HelpTreeNode(
				"Configuratrion Tool", "http://www.youtube.com/watch?v=cVH1mCc5EvU")));
		category.add(new DefaultMutableTreeNode(new HelpTreeNode(
				"Experiments and Graphs", "http://www.youtube.com/watch?v=cVH1mCc5EvU")));

		for (String layer : sortedLayerMap.keySet()) {
			category = new DefaultMutableTreeNode(layer);
			top.add(category);

			for (String prop : propertyMap.keySet()) {
				if (propertyMap.get(prop).getPluginID().equals("")
						&& (propertyMap.get(prop).isSuperclass() || propertyMap
								.get(prop).isGlobal())
						&& propertyMap
								.get(prop)
								.getPluginLayerID()
								.equals(layerMapDisplayNameToConfigName
										.get(layer))) {
					node = new DefaultMutableTreeNode(new HelpTreeNode(prop,
							path + prop + ".html"));

					category.add(node);
				}
			}
			for (String plugin : registeredPlugins.keySet()) {
				if (registeredPlugins.get(plugin).equals(
						layerMapDisplayNameToConfigName.get(layer))) {

					node = new DefaultMutableTreeNode(new HelpTreeNode(plugin,
							path + plugin + ".html"));
					category.add(node);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event
	 * .TreeSelectionEvent)
	 */
	public void valueChanged(TreeSelectionEvent e) {
		// Returns the last path element of the selection.
		// This method is useful only when the selection model allows a single
		// selection.
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree
				.getLastSelectedPathComponent();

		if (node == null) {
			return;
		}
		Object nodeInfo = node.getUserObject();
		if (node.isLeaf()) {
			
			boolean fallback = false;

			final String IMAGE_PATTERN = "\\b(http|https)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
			Pattern pattern = Pattern.compile(IMAGE_PATTERN);

			HelpTreeNode helpTreeNode = (HelpTreeNode) nodeInfo;
			pattern.matcher(helpTreeNode.getHelpTreeNodeURL());

			Matcher matcher = pattern
					.matcher(helpTreeNode.getHelpTreeNodeURL());

			if (matcher.matches()) { // Videotutorials
				try {
					java.awt.Desktop.getDesktop().browse(java.net.URI.create(helpTreeNode.getHelpTreeNodeURL()));
				} catch ( java.lang.UnsupportedOperationException ex ) {
					fallback = true;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				if ( fallback ){
					try {
						new ProcessBuilder(UserConfigService.getBRWOSER_PATH(), helpTreeNode.getHelpTreeNodeURL()).start();
					} catch (IOException e2) {
						e2.printStackTrace();
					}
				} else {
					JOptionPane.showMessageDialog(this, "Can not find Browser. Plase specify the variable \"BROWSER_PATH\" " +
							"in \"/inputOutput/simulator/config/user.properties\" ");
				}
				
			} else { // static pages
				logger.error(helpTreeNode.getHelpTreeNodeName() + " "
						+ helpTreeNode.getHelpTreeNodeURL());
				displayURL(helpTreeNode.getHelpTreeNodeURL());
			}
		}
	}

	/**
	 * Loads a given url into the SimHelpContentPanel
	 * 
	 * @param helpTreeNodeURL
	 *            is the url which should be loaded
	 */
	private void displayURL(String helpTreeNodeURL) {
		SimHelpContentPanel p = SimHelpContentPanel.getInstance();
		String urlString = helpTreeNodeURL.toString();
		p.loadURL(urlString);
		p.repaint();
	}

}
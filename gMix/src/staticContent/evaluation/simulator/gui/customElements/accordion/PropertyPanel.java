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
package staticContent.evaluation.simulator.gui.customElements.accordion;

import java.awt.Component;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;

import staticContent.evaluation.simulator.annotations.property.BoolProp;
import staticContent.evaluation.simulator.annotations.property.DoubleProp;
import staticContent.evaluation.simulator.annotations.property.FloatProp;
import staticContent.evaluation.simulator.annotations.property.IntProp;
import staticContent.evaluation.simulator.annotations.property.SimProp;
import staticContent.evaluation.simulator.annotations.property.StringProp;
import staticContent.evaluation.simulator.gui.customElements.configElements.BoolConfigElement;
import staticContent.evaluation.simulator.gui.customElements.configElements.DoubleConfigElement;
import staticContent.evaluation.simulator.gui.customElements.configElements.FloatConfigElement;
import staticContent.evaluation.simulator.gui.customElements.configElements.IntConfigElement;
import staticContent.evaluation.simulator.gui.customElements.configElements.StringConfigElement;
import staticContent.evaluation.simulator.gui.helper.SimpropComparator;
import staticContent.evaluation.simulator.gui.pluginRegistry.DependencyChecker;
import staticContent.evaluation.simulator.gui.pluginRegistry.SimPropRegistry;
import net.miginfocom.swing.MigLayout;

/**
 * {@link JPanel} constructing the custom type related configuration elements
 * underneath an unfolded {@link AccordionEntry}.
 * 
 * @author nachkonvention
 */
@SuppressWarnings("serial")
public class PropertyPanel extends JPanel {

	String localName;

	/**
	 * @param name
	 *            the name
	 */
	public PropertyPanel(String name) {
		super();
		this.localName = name;

		MigLayout migLayout = new MigLayout("", "[grow]", "[grow]");
		this.setLayout(migLayout);

		loadContent();
	}

	private void loadContent() {

		// Select all global SimProps for the given plugin layer
		SimPropRegistry simPropRegistry = SimPropRegistry.getInstance();
		String pluginLayer = this.localName;
		List<SimProp> tmpListOfAllVisibleSimProperties = simPropRegistry
				.getGlobalSimPropertiesByPluginLayer(pluginLayer);
		
		Collections.sort(tmpListOfAllVisibleSimProperties, new SimpropComparator());

		// add content
		for (SimProp simProp : tmpListOfAllVisibleSimProperties) {
			if (simProp.getValueType() == Integer.class) {
				this.add(new IntConfigElement((IntProp) simProp), "growx, wrap");
			}
			if (simProp.getValueType() == Float.class) {
				this.add(new FloatConfigElement((FloatProp) simProp), "growx, wrap");
			}
			if (simProp.getValueType() == Double.class) {
				this.add(new DoubleConfigElement((DoubleProp) simProp), "growx, wrap");
			}
			if (simProp.getValueType() == Boolean.class) {
				this.add(new BoolConfigElement((BoolProp) simProp), "growx, wrap");
			}
			if (simProp.getValueType() == String.class) {
				this.add(new StringConfigElement((StringProp) simProp), "growx, wrap");
			}
		}
		DependencyChecker.checkAll(simPropRegistry);
	}

	/**
	 * @param pluginName
	 *            the plugin name
	 */
	public void realoadContent(String pluginName) {
		for (Component c : this.getComponents()) {
			this.remove(c);
		}

		SimPropRegistry simPropRegistry = SimPropRegistry.getInstance();
		List<SimProp> tmpListOfAllVisibleSimProperties = simPropRegistry.getSimPropertiesByPluginOrPluginLayer(
				pluginName, this.localName);
		Collections.sort(tmpListOfAllVisibleSimProperties, new SimpropComparator());

		for (SimProp simProp : tmpListOfAllVisibleSimProperties) {
			if (simProp.getValueType() == Integer.class) {
				this.add(new IntConfigElement((IntProp) simProp), "growx, wrap");
			}
			if (simProp.getValueType() == Float.class) {
				this.add(new FloatConfigElement((FloatProp) simProp), "growx, wrap");
			}
			if (simProp.getValueType() == Double.class) {
				this.add(new DoubleConfigElement((DoubleProp) simProp), "growx, wrap");
			}
			if (simProp.getValueType() == Boolean.class) {
				this.add(new BoolConfigElement((BoolProp) simProp), "growx, wrap");
			}
			if (simProp.getValueType() == String.class) {
				this.add(new StringConfigElement((StringProp) simProp), "growx, wrap");
			}
		}

		// loadContent();
	}

}

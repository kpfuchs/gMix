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
package staticContent.evaluation.simulator.gui.customElements.configElements;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultCaret;

import staticContent.evaluation.simulator.annotations.property.IntProp;
import staticContent.evaluation.simulator.gui.customElements.PluginPanel;
import staticContent.evaluation.simulator.gui.pluginRegistry.SimPropRegistry;
import net.miginfocom.swing.MigLayout;

/**
 * implements the custom {@link Integer} configurator for {@link PluginPanel}.
 * 
 * @author nachkonvention
 */
@SuppressWarnings("serial")
public class IntConfigElement extends JPanel implements ChangeListener, ActionListener, ItemListener, Observer {

	IntProp property;
	JCheckBox auto;
	JCheckBox unlimited;
	JSpinner spinner;
	JSlider slider;
	Component component;
	List<JTextArea> messages;
	Map<Component, Component> icons;

	JLabel valueLabel;

	SimPropRegistry simPropRegistry;

	/**
	 * @param intProp
	 *            the property to configure
	 * 
	 */
	public IntConfigElement(IntProp intProp) {

		simPropRegistry = SimPropRegistry.getInstance();

		this.property = intProp;
		simPropRegistry.registerGuiElement(this, intProp.getPropertyID());

		// this.property.register(this);

		this.valueLabel = new JLabel();
		this.messages = new LinkedList<JTextArea>();
		this.icons = new HashMap<Component, Component>();

		MigLayout migLayout = new MigLayout("", "[grow]", "");
		this.setLayout(migLayout);

		if (intProp.getGuiElement().equals("slider")) {
			this.slider = new JSlider(this.property.getMinValue(), this.property.getMaxValue(),
					(int) this.property.getValue());
			this.slider.addChangeListener(this);
			this.valueLabel.setText("" + this.property.getValue());
			this.add(this.valueLabel, "growx, push, wrap");
			this.add(this.slider, "growx, push, wrap");
			this.component = this.slider;
		} else {
			this.spinner = new JSpinner();
			this.spinner.setModel(new SpinnerNumberModel((int) intProp.getValue(), intProp.getMinValue(), intProp
					.getMaxValue(), intProp.getStepSize()));
			this.spinner.addChangeListener(this);
			this.spinner.setToolTipText(intProp.getTooltip());
			this.add(this.spinner, "growx, push, wrap");
			this.component = this.spinner;
		}

		this.setBorder(BorderFactory.createTitledBorder(intProp.getName()));

		this.auto = new JCheckBox("AUTO");
		this.auto.addItemListener(this);
		this.auto.setToolTipText("Overwrite with AUTO");

		this.unlimited = new JCheckBox("UNLIMITED");
		this.unlimited.addItemListener(this);
		this.unlimited.setToolTipText("Overwrite with UNLIMITED");

		this.auto.setSelected(this.property.getAuto());
		this.unlimited.setSelected(this.property.getUnlimited());

		if (intProp.getEnableAuto()) {
			this.add(auto, "wrap");
		}

		if (intProp.getEnableUnlimited()) {
			this.add(unlimited, "push");
			;
		}

		if (!intProp.getInfo().equals("")) {
			JTextArea textarea = new JTextArea("Info: " + intProp.getInfo());
			textarea.setCaret(new DefaultCaret());
			textarea.setEditable(false);
			textarea.setLineWrap(true);
			textarea.setWrapStyleWord(true);
			textarea.setPreferredSize(new Dimension(10, 25));
			this.add(textarea, "growx, growy, wmin 10");
		}

	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged(ChangeEvent event) {
		if (event.getSource() == this.spinner) {
			simPropRegistry.setValue(this.property.getPropertyID(), this.spinner.getValue());
		}
		if (event.getSource() == this.slider) {
			this.valueLabel.setText("" + this.property.getValue());
			simPropRegistry.setValue(this.property.getPropertyID(), this.slider.getValue());
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent event) {

	}

	/* (non-Javadoc)
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	@Override
	public void itemStateChanged(ItemEvent event) {
		if (this.auto.isSelected()) {
			this.unlimited.setEnabled(false);
			this.component.setEnabled(false);
			this.simPropRegistry.setAuto(this.property.getPropertyID(), true, Integer.class);
		} else if (this.unlimited.isSelected()) {
			this.auto.setEnabled(false);
			this.component.setEnabled(false);
			this.simPropRegistry.setUnlimited(this.property.getPropertyID(), true, Integer.class);
		} else {
			this.unlimited.setEnabled(true);
			this.auto.setEnabled(true);
			this.component.setEnabled(true);
			this.simPropRegistry.setAuto(this.property.getPropertyID(), false, Integer.class);
			this.simPropRegistry.setUnlimited(this.property.getPropertyID(), false, Integer.class);
		}
	}

	// Called when simprop has changed
	@Override
	public void update(Observable observable, Object o) {

		for (Component message : this.messages) {
			this.remove(message);
		}
		for (Component icon : this.icons.values()) {
			this.remove(icon);
		}

		this.messages.clear();
		this.icons.clear();

		this.auto.setSelected(this.property.getAuto());
		this.unlimited.setSelected(this.property.getUnlimited());

		if ((boolean) o) { // enabled by requirement
			this.component.setEnabled(true);
			this.unlimited.setEnabled(true);
			this.auto.setEnabled(true);
		} else { // disabled by requirement
			this.component.setEnabled(false);
			this.unlimited.setEnabled(false);
			this.auto.setEnabled(false);
		}

		if (property.getGuiElement().equals("slider")) {
			this.slider.setValue((int) simPropRegistry.getValue(property.getPropertyID()).getValue());
		} else {
			this.spinner.setValue((int) simPropRegistry.getValue(property.getPropertyID()).getValue());
		}

		if (property.getWarnings() != null && property.getWarnings().size() > 0) {
			for (String each : property.getWarnings()) {
				JTextArea text = new JTextArea(each);
				text.setCaret(new DefaultCaret());
				text.setBackground(new Color(250, 210, 115));
				text.setAutoscrolls(false);
				text.setEditable(false);
				text.setLineWrap(true);
				text.setWrapStyleWord(true);
				text.setPreferredSize(new Dimension(10, 25));
				this.messages.add(text);
				JLabel warning = new JLabel(new ImageIcon("etc/img/icons/warning/warning_16.png"));
				this.icons.put(text, warning);
			}
		}

		if (property.getErrors() != null && property.getErrors().size() > 0) {
			for (String each : property.getErrors()) {
				JTextArea text = new JTextArea(each);
				text.setCaret(new DefaultCaret());
				text.setBackground(new Color(250, 150, 135));
				text.setAutoscrolls(false);
				text.setEditable(false);
				text.setLineWrap(true);
				text.setWrapStyleWord(true);
				text.setPreferredSize(new Dimension(10, 25));
				this.messages.add(text);
				JLabel error = new JLabel(new ImageIcon("etc/img/icons/error/error_16.png"));
				this.icons.put(text, error);

			}
		}

		for (JTextArea message : this.messages) {
			DefaultCaret caret = (DefaultCaret) message.getCaret();
			caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
			message.setCaret(caret);
			message.setAutoscrolls(false);
			this.add(this.icons.get(message), "push, wmin 16, wrap");
			this.add(message, "growx, push, wmin 10");
		}

		updateUI();
	}
}

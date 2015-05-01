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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import staticContent.evaluation.simulator.annotations.property.SimProp;
import staticContent.evaluation.simulator.gui.customElements.structure.HelpPropValues;
import staticContent.evaluation.simulator.gui.pluginRegistry.SimPropRegistry;

/**
 * @author alex
 *
 */
@SuppressWarnings("serial")
public class PropVaryElement extends JPanel {

	private static Logger logger = Logger.getLogger(PropVaryElement.class);

	private JComboBox<String> cBox[];
	private JTextField propElement[];
	private HelpPropValues value[];
	private Class propType[];
	private Boolean propUsed[];
	private final int numOfPropsToVary = 2;
	private String cBoxItemSave;

	Map<String, SimProp> propMap;
	Map<JComboBox<String>, Integer> boxToIndexMap;
	Map<JTextField, Integer> propToIndexMap;

	public PropVaryElement() {
		this.setLayout(new MigLayout("", "[grow]", ""));

		loadContent();
	}

	/**
	 * Loads content
	 */
	private void loadContent() {

		this.propMap = SimPropRegistry.getInstance().getProperties();
		
		// old version
//		String propertyStrings[] = propMap.keySet().toArray(new String[1]);
		
		// added by alex		
		Vector<String> tmp = new Vector<String>();
		for (String key: propMap.keySet()){
			SimProp tmpProp = propMap.get(key);
			if (tmpProp.isPropertyToVary()){
				String tmpString = tmpProp.getPluginID()+ ":" + tmpProp.getName();
				tmp.add(tmpString);
			}
		}
		
		tmp.add(0, "---");
		Collections.sort(tmp);
		
		//String propertyStrings[] = tmp.toArray(new String[1]);		
		
		cBox = new JComboBox[numOfPropsToVary];
		cBox[0] = new JComboBox<String>(tmp);
		cBox[0].setPrototypeDisplayValue("xxxxxxxxxxxxxxxxxxx");
		cBox[1] = new JComboBox<String>(tmp);
		cBox[1].setPrototypeDisplayValue("xxxxxxxxxxxxxxxxxxx");
		addBoxListener(cBox[0]);
		addBoxListener(cBox[1]);
		cBoxItemSave = "EMPTY";

		propElement = new JTextField[numOfPropsToVary];
		propElement[0] = new JTextField();
		propElement[1] = new JTextField();
		addTextListener(propElement[0]);
		addTextListener(propElement[1]);
		addFocusListener(propElement[0]);
		addFocusListener(propElement[1]);

		value = new HelpPropValues[numOfPropsToVary];
		value[0] = null;
		value[1] = null;
		
		propUsed = new Boolean[numOfPropsToVary];
		propUsed[0] = false;
		propUsed[1] = false;

		propType = new Class[numOfPropsToVary];

		this.boxToIndexMap = new HashMap<JComboBox<String>, Integer>();
		this.boxToIndexMap.put(cBox[0], 0);
		this.boxToIndexMap.put(cBox[1], 1);

		this.propToIndexMap = new HashMap<JTextField, Integer>();
		this.propToIndexMap.put(propElement[0], 0);
		this.propToIndexMap.put(propElement[1], 1);
		this.add(cBox[0], "growx, wrap");
		this.add(propElement[0], "growx, wrap");
		this.add(cBox[1], "growx, wrap");
		this.add(propElement[1], "growx");
		// this.add(wrapper1);

		// JPanel wrapper2 = new JPanel(new BorderLayout());
		// wrapper2.add(propElement[0], BorderLayout.CENTER);
		// this.add(wrapper2);
		//
		// JPanel wrapper3 = new JPanel(new BorderLayout());
		// wrapper3.add(cBox[1], BorderLayout.CENTER);
		// this.add(wrapper3);
		//
		// JPanel wrapper4 = new JPanel(new BorderLayout());
		// wrapper4.add(propElement[1], BorderLayout.CENTER);
		// this.add(wrapper4);

		comboboxChanged(cBox[0]);
		comboboxChanged(cBox[1]);

	}

	private void comboboxChanged(JComboBox<String> ComboBox) {

		int index = this.boxToIndexMap.get(ComboBox);
		JTextField currentElement = propElement[index];

		currentElement.setText("");
		//value[index] = null;
		textfieldused(currentElement);

		String currentItem = (String) ComboBox.getSelectedItem();
		String propname = currentItem;
		StringTokenizer	tokenizer = new StringTokenizer(currentItem, ":" );
		
		System.err.println(propname);
		
		while (tokenizer.hasMoreTokens()){
			propname = (String) tokenizer.nextElement();
			System.err.println(propname);
		}
		System.err.println(propname);
		
		if ((index == 0) && (!cBoxItemSave.equals("EMPTY"))){
			
			cBox[1].addItem(cBoxItemSave);
			cBoxItemSave="EMPTY";
		}
		
		if (currentItem.equals("---")) {
			propElement[index].setEnabled(false);
			propType[index] = null;
			propUsed[index] = false;
			if (index == 0) {
				cBox[1].setSelectedItem("---");
				this.cBox[1].setEnabled(false);
				this.propElement[1].setEnabled(false);
				this.propUsed[1] = false;
			}			
		} else {
			propElement[index].setEnabled(true);			
			SimProp tmp = SimPropRegistry.getInstance().getPropertiesByName(propname);			
			propType[index] = tmp.getValueType();
			logger.log(Level.DEBUG, "Proptype is set to" + propType[index].toString());
		}

		if ((ComboBox == cBox[0]) && (!currentItem.equals("---"))) {
			this.cBox[1].setEnabled(true);			
			String prop = SimPropRegistry.getInstance().getPropertiesByName(propname).getPropertyID();
			logger.log(Level.DEBUG,"writing into PROPERTY_TO_VARY: " + prop);
			SimPropRegistry.getInstance().setPropertyToVaryValue("PROPERTY_TO_VARY", prop);
			cBoxItemSave = currentItem;
			cBox[1].removeItem(currentItem);
		}
				
		if ((ComboBox == cBox[1]) && (!currentItem.equals("---"))){
			String prop = SimPropRegistry.getInstance().getPropertiesByName(propname).getPropertyID();
			logger.log(Level.DEBUG,"writing into second PROPERTY_TO_VARY: " + prop);
			SimPropRegistry.getInstance().setPropertyToVaryValue("SECOND_PROPERTY_TO_VARY", prop);
		}
		
		if (String.valueOf(cBox[1].getSelectedItem()).equals("---")){
			SimPropRegistry.getInstance().setPropertyToVaryValue("USE_SECOND_PROPERTY_TO_VARY", "FALSE");
		}else{
			SimPropRegistry.getInstance().setPropertyToVaryValue("USE_SECOND_PROPERTY_TO_VARY", "TRUE");
		}		

		logger.log(Level.DEBUG, currentElement.getSelectedText());
		this.repaint();
		SimPropRegistry.getInstance().setUnsavedChanges(true);

	}

	private void addBoxListener(final JComboBox<String> box) {
		ItemListener il = new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {

				if (e.getStateChange() == ItemEvent.SELECTED) {

					PropVaryElement.this.comboboxChanged(box);					
					
				}
			}
		};
		box.addItemListener(il);
	}

	private void addTextListener(final JTextField field) {
		ActionListener al = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent a) {			
				
				if (a.getActionCommand() != null) {
					textfieldused(field);					
				}
			}

		};
		field.addActionListener(al);
	}
	
	private void addFocusListener(final JTextField field) {
		FocusListener fl = new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent f) {	
				textfieldused(field);
			}

			@Override
			public void focusGained(FocusEvent arg0) {
				field.select(0, field.getSelectionEnd());				
			}
		};
		field.addFocusListener(fl);
	}
	
	private void textfieldused(JTextField field) {
		int i = PropVaryElement.this.propToIndexMap.get(field);
		value[i] = new HelpPropValues(field.getText(), propType[i]);
		logger.log(Level.DEBUG, "PropertyType is: " + value[i].getTypeName());
		logger.log(Level.DEBUG, "PropertyValue is: " + field.getText());
		boolean validity = value[i].isValid();
		logger.log(Level.DEBUG, "Validity is: " + validity);
		
		if (validity && field == propElement[0]){
			SimPropRegistry.getInstance().setPropertyToVaryValue("VALUES_FOR_THE_PROPERTY_TO_VARY", propElement[0].getText());
		}else if (validity && field == propElement[1]){
			SimPropRegistry.getInstance().setPropertyToVaryValue("VALUES_FOR_THE_SECOND_PROPERTY_TO_VARY", propElement[1].getText());
		}else{
			JOptionPane.showMessageDialog(field,
				    "Wrong Value. Values are supposed to be " +value[i].getTypeName() +". Separation by \",\" possible.",
				    "Invalid Value",
				    JOptionPane.ERROR_MESSAGE);
		}
		SimPropRegistry.getInstance().setUnsavedChanges(true);
	}

	/**
	 * Update
	 */
	public void update() {
		logger.log(Level.DEBUG, "PROPERTY_TO_VARY");
		String id = SimPropRegistry.getInstance().getPropertiesToVary().get("PROPERTY_TO_VARY");
		logger.log(Level.DEBUG, "VALUES_FOR_THE_PROPERTY_TO_VARY");
		String values0 = SimPropRegistry.getInstance().getPropertiesToVary().get("VALUES_FOR_THE_PROPERTY_TO_VARY");
		logger.log(Level.DEBUG, values0);
		
		System.err.println("FIRST PROP TO VARRY");
		System.err.println("id: "+ id);		
		String name = SimPropRegistry.getInstance().getPropertieNameByID(id);
		System.err.println("name: "+ name);
		if(name !="---"){
			SimProp prop = SimPropRegistry.getInstance().getPropertiesByName(name);
			System.err.println("prop: "+ prop);
			String plugin = prop.getPluginID();		
			cBox[0].setSelectedItem(plugin +":"+ name);
		}
		else{
			cBox[0].setSelectedItem("---");
		}
		
		comboboxChanged(cBox[0]);		
		propElement[0].setText(values0);
		textfieldused(propElement[0]);
		
				
		
		logger.log(Level.DEBUG, "USE_SECOND_PROPERTY_TO_VARY");
		logger.log(Level.DEBUG, SimPropRegistry.getInstance().getPropertiesToVary().get("USE_SECOND_PROPERTY_TO_VARY"));
		
		logger.log(Level.DEBUG, SimPropRegistry.getInstance().getPropertiesToVary().get("SECOND_PROPERTY_TO_VARY"));
		id = SimPropRegistry.getInstance().getPropertiesToVary().get("SECOND_PROPERTY_TO_VARY");
		logger.log(Level.DEBUG, SimPropRegistry.getInstance().getPropertiesToVary().get("VALUES_FOR_THE_SECOND_PROPERTY_TO_VARY"));		
		logger.log(Level.DEBUG, "VALUES_FOR_THE_SECOND_PROPERTY_TO_VARY");
		String values1 = SimPropRegistry.getInstance().getPropertiesToVary().get("VALUES_FOR_THE_SECOND_PROPERTY_TO_VARY");
		logger.log(Level.DEBUG, values1);
		
		System.err.println("SECOND PROP TO VARRY");
		System.err.println("id: "+ id);	
		name = SimPropRegistry.getInstance().getPropertieNameByID(id);
		System.err.println("name: "+ name);
		if(name !="---"){
			SimProp prop = SimPropRegistry.getInstance().getPropertiesByName(name);
			System.err.println("prop: "+ prop);
			String plugin = prop.getPluginID();			
			cBox[1].setSelectedItem(plugin+ ":" +name);
			
		}
		else{
			cBox[1].setSelectedItem("---");
		}
		
		comboboxChanged(cBox[1]);		
		propElement[1].setText(values1);
		textfieldused(propElement[0]);
		
		
		
	}

}

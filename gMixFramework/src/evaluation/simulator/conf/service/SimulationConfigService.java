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
package evaluation.simulator.conf.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import evaluation.simulator.annotations.property.DoubleProp;
import evaluation.simulator.annotations.property.FloatProp;
import evaluation.simulator.annotations.property.IntProp;
import evaluation.simulator.annotations.property.SimProp;
import evaluation.simulator.gui.customElements.SimConfigPanel;
import evaluation.simulator.gui.pluginRegistry.DependencyChecker;
import evaluation.simulator.gui.pluginRegistry.SimPropRegistry;

public class SimulationConfigService {
	
	private static Logger logger = Logger.getLogger(SimulationConfigService.class);

	private SimPropRegistry simPropRegistry;

	public SimulationConfigService(SimPropRegistry simPropRegistry) {
		this.setSimPropRegistry(simPropRegistry);
	}

	public SimPropRegistry getSimPropRegistry() {
		return this.simPropRegistry;
	}

	public void loadConfig(File file) {
		

		Properties props = new Properties();
		try {
			props.load(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(null, "File not found.");
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Could not read from file.");
		} catch (IllegalArgumentException e) {
			JOptionPane.showMessageDialog(null, "Could not read from file.");
		}
		// This value is not in range
		
		// Properties to vary
		for (String s : this.simPropRegistry.getPropertiesToVary().keySet()) {
			logger.log(Level.DEBUG, "Load value for property to vary " + s);
			try {
				
				// fallback to EDFVersion=0 
				if ( props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("POISSON") &&
						props.getProperty(s).equals("RESOLVE_TIME")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "POISSON_RESOLVE_TIME");
					logger.log(Level.ERROR, s + " = POISSON_RESOLVE_TIME");
					
				} else if (props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("CONSTANT") &&
						props.getProperty(s).equals("RESOLVE_TIME")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "CONSTANT_RESOLVE_TIME");
					logger.log(Level.ERROR, s + " = CONSTANT_RESOLVE_TIME");
					
				} else if (props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("PARETO") &&
						props.getProperty(s).equals("RESOLVE_TIME")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "PARETO_RESOLVE_TIME");
					logger.log(Level.ERROR, s + " = PARETO_RESOLVE_TIME");
					
				} else if (props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("REQUEST_REPLY") &&
						props.getProperty(s).equals("RESOLVE_TIME")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "REQUEST_REPLY_RESOLVE_TIME");
					logger.log(Level.ERROR, s + " = REQUEST_REPLY_RESOLVE_TIME");
					
				} else if ( props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("POISSON") &&
						props.getProperty(s).equals("REPLY_SIZE")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "POISSON_REPLY_SIZE");
					logger.log(Level.ERROR, s + " = POISSON_REPLY_SIZE");
					
				} else if (props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("CONSTANT") &&
						props.getProperty(s).equals("REPLY_SIZE")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "CONSTANT_REPLY_SIZE");
					logger.log(Level.ERROR, s + " = CONSTANT_REPLY_SIZE");
					
				} else if (props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("PARETO") &&
						props.getProperty(s).equals("REPLY_SIZE")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "PARETO_REPLY_SIZE");
					logger.log(Level.ERROR, s + " = PARETO_REPLY_SIZE");
					
				} else if (props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("REQUEST_REPLY") &&
						props.getProperty(s).equals("REPLY_SIZE")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "REQUEST_REPLY_REPLY_SIZE");
					logger.log(Level.ERROR, s + " = REQUEST_REPLY_REPLY_SIZE");
					
				} else if ( props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("POISSON") &&
						props.getProperty(s).equals("REQUEST_SIZE")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "POISSON_REQUEST_SIZE");
					logger.log(Level.ERROR, s + " = POISSON_REQUEST_SIZE");
					
				} else if (props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("CONSTANT") &&
						props.getProperty(s).equals("REQUEST_SIZE")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "CONSTANT_REQUEST_SIZE");
					logger.log(Level.ERROR, s + " = CONSTANT_REQUEST_SIZE");
					
				} else if (props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("PARETO") &&
						props.getProperty(s).equals("REQUEST_SIZE")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "PARETO_REQUEST_SIZE");
					logger.log(Level.ERROR, s + " = PARETO_REQUEST_SIZE");
					
				} else if (props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("REQUEST_REPLY") &&
						props.getProperty(s).equals("REQUEST_SIZE")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "REQUEST_REPLY_REQUEST_SIZE");
					logger.log(Level.ERROR, s + " = REQUEST_REPLY_REQUEST_SIZE");
					
				} else if ( props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("POISSON") &&
						props.getProperty(s).equals("AVERAGE_REQUESTS_PER_SECOND_AND_CLIENT")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "POISSON_AVERAGE_REQUESTS_PER_SECOND_AND_CLIENT");
					logger.log(Level.ERROR, s + " = POISSON_AVERAGE_REQUESTS_PER_SECOND_AND_CLIENT");
					
				} else if (props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("CONSTANT") &&
						props.getProperty(s).equals("AVERAGE_REQUESTS_PER_SECOND_AND_CLIENT")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "CONSTANT_AVERAGE_REQUESTS_PER_SECOND_AND_CLIENT");
					logger.log(Level.ERROR, s + " = CONSTANT_AVERAGE_REQUESTS_PER_SECOND_AND_CLIENT");
					
				} else if (props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("PARETO") &&
						props.getProperty(s).equals("AVERAGE_REQUESTS_PER_SECOND_AND_CLIENT")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "PARETO_AVERAGE_REQUESTS_PER_SECOND_AND_CLIENT");
					logger.log(Level.ERROR, s + " = PARETO_AVERAGE_REQUESTS_PER_SECOND_AND_CLIENT");
					
				} else if ( props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("POISSON") &&
						props.getProperty(s).equals("NUMBER_OF_CLIENTS_TO_SIMULATE")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "POISSON_NUMBER_OF_CLIENTS_TO_SIMULATE");
					logger.log(Level.ERROR, s + " = POISSON_NUMBER_OF_CLIENTS_TO_SIMULATE");
					
				} else if (props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("CONSTANT") &&
						props.getProperty(s).equals("NUMBER_OF_CLIENTS_TO_SIMULATE")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "CONSTANT_NUMBER_OF_CLIENTS_TO_SIMULATE");
					logger.log(Level.ERROR, s + " = CONSTANT_NUMBER_OF_CLIENTS_TO_SIMULATE");
					
				} else if (props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("PARETO") &&
						props.getProperty(s).equals("NUMBER_OF_CLIENTS_TO_SIMULATE")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "PARETO_NUMBER_OF_CLIENTS_TO_SIMULATE");
					logger.log(Level.ERROR, s + " = PARETO_NUMBER_OF_CLIENTS_TO_SIMULATE");
					
				} else if (props.getProperty("TYPE_OF_TRAFFIC_GENERATOR").equals("REQUEST_REPLY") &&
						props.getProperty(s).equals("NUMBER_OF_CLIENTS_TO_SIMULATE")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "REQUEST_REPLY_NUMBER_OF_CLIENTS_TO_SIMULATE");
					logger.log(Level.ERROR, s + " = REQUEST_REPLY_NUMBER_OF_CLIENTS_TO_SIMULATE");
					
				} else if (props.getProperty("OUTPUT_STRATEGY").equals("DLPA_BASIC") &&
						props.getProperty(s).equals("REQUEST_DELAY")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "MAX_DLPAB_REQUEST_DELAY");
					logger.log(Level.ERROR, s + " = MAX_DLPAB_REQUEST_DELAY");
					
				} else if (props.getProperty("OUTPUT_STRATEGY").equals("DLPA_HEURISTIC") &&
						props.getProperty(s).equals("REQUEST_DELAY")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "MAX_DLPAI_REQUEST_DELAY");
					logger.log(Level.ERROR, s + " = MAX_DLPAI_REQUEST_DELAY");
					
				} else if (props.getProperty("OUTPUT_STRATEGY").equals("DLPA_HEURISTIC_2") &&
						props.getProperty(s).equals("REQUEST_DELAY")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "MAX_DLPAII_REQUEST_DELAY");
					logger.log(Level.ERROR, s + " = MAX_DLPAII_REQUEST_DELAY");
					
				} else if (props.getProperty("OUTPUT_STRATEGY").equals("DLPA_BASIC") &&
						props.getProperty(s).equals("REPLY_DELAY")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "MAX_DLPAB_REPLY_DELAY");
					logger.log(Level.ERROR, s + " = MAX_DLPAB_REPLY_DELAY");
					
				} else if (props.getProperty("OUTPUT_STRATEGY").equals("DLPA_HEURISTIC") &&
						props.getProperty(s).equals("REPLY_DELAY")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "MAX_DLPAI_REPLY_DELAY");
					logger.log(Level.ERROR, s + " = MAX_DLPAI_REPLY_DELAY");
					
					
				} else if (props.getProperty("OUTPUT_STRATEGY").equals("DLPA_HEURISTIC_2") &&
						props.getProperty(s).equals("REPLY_DELAY")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "MAX_DLPAII_REPLY_DELAY");
					logger.log(Level.ERROR, s + " = MAX_DLPAII_REPLY_DELAY");
					
					
				}else if (props.getProperty("OUTPUT_STRATEGY").equals("BASIC_BATCH") &&
						props.getProperty(s).equals("BATCH_SIZE")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "BASIC_BATCH_BATCH_SIZE");
					logger.log(Level.ERROR, s + " = BASIC_BATCH_BATCH_SIZE");
					
					
				} else if (props.getProperty("OUTPUT_STRATEGY").equals("BATCH_WITH_TIMEOUT") &&
						props.getProperty(s).equals("BATCH_SIZE")
					){
					this.simPropRegistry.setPropertyToVaryValue(s, "BATCH_WITH_TIMEOUT_BATCH_SIZE");
					logger.log(Level.ERROR, s + " = BATCH_WITH_TIMEOUT_BATCH_SIZE");
					
				} else {
					this.simPropRegistry.setPropertyToVaryValue(s, props.getProperty(s));
					logger.log(Level.DEBUG, s + " = " + props.getProperty(s));
					
				}
				
//				if ( props.getProperty("SECOND_PROPERTY_TO_VARY").equals("") || 
//						props.getProperty("VALUES_FOR_THE_SECOND_PROPERTY_TO_VARY").equals("")
//						){
//					this.simPropRegistry.setPropertyToVaryValue(s, "FALSE");
//					
//				}
				
			} catch (Exception e) {
				
				logger.log(Level.ERROR,"Can not read value for " + s);
				
			}
		}
		
		// Load simProps
		for (Entry<String, SimProp> s : this.simPropRegistry.getAllSimProps()) {
			logger.log(Level.DEBUG, "Load value for " + s.getKey());
			try {
				if (s.getValue().getValueType() == String.class) {
					s.getValue().setValue((props.get(s.getKey())));
				} else if (s.getValue().getValueType() == Integer.class) {
					if (props.get(s.getKey()).equals("AUTO")) {
						((IntProp) s.getValue() ).setAuto(true);
					} else if (props.get(s.getKey()).equals("UNLIMITED")) {
						((IntProp) s.getValue() ).setUnlimited(true);
					}else{
						((IntProp) s.getValue() ).setAuto(false);
						((IntProp) s.getValue() ).setUnlimited(false);
						s.getValue().setValue(Integer.parseInt((String) props.get(s.getKey())));
					}
				} else if (s.getValue().getValueType() == Float.class) {
					if (props.get(s.getKey()).equals("AUTO")) {
						((FloatProp) s.getValue() ).setAuto(true);
					} else if (props.get(s.getKey()).equals("UNLIMITED")) {
						((FloatProp) s.getValue() ).setUnlimited(true);
					}else{
						((FloatProp) s.getValue() ).setAuto(false);
						((FloatProp) s.getValue() ).setUnlimited(false);
						s.getValue().setValue(Float.parseFloat((String) props.get(s.getKey())));
					}
				} else if (s.getValue().getValueType() == Double.class) {
					if (props.get(s.getKey()).equals("AUTO")) {
						((DoubleProp) s.getValue() ).setAuto(true);
					} else if (props.get(s.getKey()).equals("UNLIMITED")) {
						((DoubleProp) s.getValue() ).setUnlimited(true);
					}else{
						((DoubleProp) s.getValue() ).setAuto(false);
						((DoubleProp) s.getValue() ).setUnlimited(false);
						s.getValue().setValue(Double.parseDouble((String) props.get(s.getKey())));
					}
				} else if (s.getValue().getValueType() == Boolean.class) {
					s.getValue().setValue(Boolean.parseBoolean((String) props.get(s.getKey())));
				}
				
				//if ( s.getKey().equals("NAME_OF_PLOT_SCRIPT") ){
				//	s.getValue().setValue("simguiPlotScript.txt");
				//}
				
				s.getValue().changed();
				
			} catch (NullPointerException e) {

//				logger.log(Level.ERROR,"Fallback mode:" + s.getValue().getPropertyID());
				
				// Load EDFVersion=0 Files
				
				String fallback = "FALLBACK";
				
				if ( s.getKey().equals("PARETO_RESOLVE_TIME") ||
						s.getKey().equals("REQUEST_REPLY_RESOLVE_TIME") ||
						s.getKey().equals("POISSON_RESOLVE_TIME") ||
						s.getKey().equals("CONSTANT_RESOLVE_TIME") 
					){
					
					fallback = "RESOLVE_TIME";
					
					logger.log(Level.DEBUG,"Fallback mode: " + s.getKey() + " --> " + fallback);
					
					
				} else if ( s.getKey().equals("POISSON_REPLY_SIZE") ||
						s.getKey().equals("CONSTANT_REPLY_SIZE") ||
						s.getKey().equals("PARETO_REPLY_SIZE") ||
						s.getKey().equals("REQUEST_REPLY_REPLY_SIZE")
					){
					
					fallback = "REPLY_SIZE";
					
					logger.log(Level.DEBUG,"Fallback mode: " + s.getKey() + " --> " + fallback);
					
					
				}  else if ( s.getKey().equals("REQUEST_REPLY_REQUEST_SIZE") ||
						s.getKey().equals("POISSON_REQUEST_SIZE") ||
						s.getKey().equals("CONSTANT_REQUEST_SIZE") ||
						s.getKey().equals("PARETO_REQUEST_SIZE")
					){
					
					fallback = "REQUEST_SIZE";
					
					logger.log(Level.DEBUG,"Fallback mode: " + s.getKey() + " --> " + fallback);
					
				} else if ( s.getKey().equals("CONSTANT_AVERAGE_REQUESTS_PER_SECOND_AND_CLIENT") ||
						s.getKey().equals("POISSON_AVERAGE_REQUESTS_PER_SECOND_AND_CLIENT") ||
						s.getKey().equals("PARETO_AVERAGE_REQUESTS_PER_SECOND_AND_CLIENT")
						
					){
					
					fallback = "AVERAGE_REQUESTS_PER_SECOND_AND_CLIENT";
					
					logger.log(Level.DEBUG,"Fallback mode: " + s.getKey() + " --> " + fallback);
					
				} else if ( s.getKey().equals("POISSON_NUMBER_OF_CLIENTS_TO_SIMULATE") ||
						s.getKey().equals("PARETO_NUMBER_OF_CLIENTS_TO_SIMULATE") ||
						s.getKey().equals("CONSTANT_NUMBER_OF_CLIENTS_TO_SIMULATE") ||
						s.getKey().equals("REQUEST_REPLY_NUMBER_OF_CLIENTS_TO_SIMULATE")
					){
					
					fallback = "NUMBER_OF_CLIENTS_TO_SIMULATE";
					
					logger.log(Level.DEBUG,"Fallback mode: " + s.getKey() + " --> " + fallback);
					
				}  else if ( s.getKey().equals("MAX_DLPAB_REQUEST_DELAY") ||
						s.getKey().equals("MAX_DLPAI_REQUEST_DELAY") ||
						s.getKey().equals("MAX_DLPAII_REQUEST_DELAY")
					){
					
					fallback = "MAX_DLPA_REQUEST_DELAY";
					
					logger.log(Level.DEBUG,"Fallback mode: " + s.getKey() + " --> " + fallback);
					
				} else if ( s.getKey().equals("MAX_DLPAII_REPLY_DELAY") ||
						s.getKey().equals("MAX_DLPAI_REPLY_DELAY") ||
						s.getKey().equals("MAX_DLPAB_REPLY_DELAY")
					){
					
					fallback = "MAX_DLPA_REPLY_DELAY";
					
					logger.log(Level.DEBUG,"Fallback mode: " + s.getKey() + " --> " + fallback);
					
				} else if ( s.getKey().equals("BASIC_BATCH_BATCH_SIZE") ||
						s.getKey().equals("BATCH_WITH_TIMEOUT_BATCH_SIZE")
					){
					
					fallback = "BATCH_SIZE";
					
					logger.log(Level.DEBUG,"Fallback mode: " + s.getKey() + " --> " + fallback);
					
				}  else {
					logger.log(Level.ERROR,"No fallback available for " + s.getKey());
					continue;
				}
				
				logger.log(Level.DEBUG, "Load value for " + s.getKey());
				try {
					if (s.getValue().getValueType() == String.class) {
						s.getValue().setValue((props.get(fallback)));
					} else if (s.getValue().getValueType() == Integer.class) {
						if (props.get(fallback).equals("AUTO")) {
							((IntProp) s.getValue() ).setAuto(true);
						} else if (props.get(fallback).equals("UNLIMITED")) {
							((IntProp) s.getValue() ).setUnlimited(true);
						}else{
							((IntProp) s.getValue() ).setAuto(false);
							((IntProp) s.getValue() ).setUnlimited(false);
							System.out.println("[a: +" +props.get(fallback) +" - b:" + s.getValue() +"] s:" +s); 
							// TODO: remove
							s.getValue().setValue(Integer.parseInt((String) props.get(fallback)));
						}
					} else if (s.getValue().getValueType() == Float.class) {
						if (props.get(fallback).equals("AUTO")) {
							((FloatProp) s.getValue() ).setAuto(true);
						} else if (props.get(fallback).equals("UNLIMITED")) {
							((FloatProp) s.getValue() ).setUnlimited(true);
						}else{
							((FloatProp) s.getValue() ).setAuto(false);
							((FloatProp) s.getValue() ).setUnlimited(false);
							s.getValue().setValue(Float.parseFloat((String) props.get(fallback)));
						}
					} else if (s.getValue().getValueType() == Double.class) {
						if (props.get(fallback).equals("AUTO")) {
							((DoubleProp) s.getValue() ).setAuto(true);
						} else if (props.get(fallback).equals("UNLIMITED")) {
							((DoubleProp) s.getValue() ).setUnlimited(true);
						}else{
							((DoubleProp) s.getValue() ).setAuto(false);
							((DoubleProp) s.getValue() ).setUnlimited(false);
							s.getValue().setValue(Double.parseDouble((String) props.get(fallback)));
						}
					} else if (s.getValue().getValueType() == Boolean.class) {
						s.getValue().setValue(Boolean.parseBoolean((String) props.get(fallback)));
					}
					
					s.getValue().changed();
					
			} catch (NullPointerException ex) {
				logger.log(Level.ERROR,"Can not read value for fallback " + fallback);
			}
		}
		}

		SimPropRegistry simPropRegistry = SimPropRegistry.getInstance();
		List<String> pluginLevels = simPropRegistry.getPluginLayers();

		for (String pluginLevel : pluginLevels) {
			String configName = SimPropRegistry.getInstance().displayNameToConfigName(pluginLevel);
			String selectedPlugin = (String) props.getProperty(configName);
			SimPropRegistry.getInstance().setActivePluginsMapped(configName,selectedPlugin);

			// Update GUI in order to inform JComboBoxes
			SimConfigPanel.getInstance().update();
		}
		this.simPropRegistry.setCurrentConfigFile(file.getAbsolutePath());
		DependencyChecker.checkAll(this.simPropRegistry);
		SimConfigPanel.setStatusofSaveButton(!DependencyChecker.errorsInConfig);
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				SimConfigPanel.getInstance().foldAccordions();
				SimPropRegistry.getInstance().setUnsavedChanges(false);
			}
		});
		this.simPropRegistry.setUnsavedChanges(false);
	} 

	public void setSimPropRegistry(SimPropRegistry simPropRegistry) {
		this.simPropRegistry = simPropRegistry;
	}

	public void writeConfig(File outputFile) {

		PropertiesConfiguration props;
		try {

			props = new PropertiesConfiguration("inputOutput/simulator/config/experiment_template.cfg");

			props.setProperty("EDF_VERSION", 1);

			// static part
			Map<String, String> plugins = this.simPropRegistry.getActivePlugins(true);
			logger.log(Level.DEBUG, "Active plugins are:");
			for (String key : plugins.keySet()) {
				logger.log(Level.DEBUG, key + " with " + plugins.get(key));
				props.setProperty(key, plugins.get(key));
			}
			
			// Properties to vary
			for (Entry<String, String> s : this.simPropRegistry.getPropertiesToVary().entrySet()) {
				logger.log(Level.DEBUG, s.getKey() + "=" + s.getValue().toString());
				props.setProperty(s.getKey(), s.getValue());
			}

			// dynamic part
			for (Entry<String, SimProp> s : this.simPropRegistry.getAllSimProps()) {
				try {
					if (s.getValue().getValueType() == Integer.class && ((IntProp)(s.getValue())).getAuto()) {
						logger.log(Level.DEBUG, s.getKey() + "=AUTO");
						props.setProperty(s.getKey(), "AUTO");
					}else if (s.getValue().getValueType() == Integer.class && ((IntProp)(s.getValue())).getUnlimited()) {
						logger.log(Level.DEBUG, s.getKey() + "=UNLIMITED");
						props.setProperty(s.getKey(), "UNLIMITED");
					}else if (s.getValue().getValueType() == Float.class && ((FloatProp)(s.getValue())).getAuto()) {
						logger.log(Level.DEBUG, s.getKey() + "=AUTO");
						props.setProperty(s.getKey(), "AUTO");
					}else if (s.getValue().getValueType() == Float.class && ((FloatProp)(s.getValue())).getUnlimited()) {
						logger.log(Level.DEBUG, s.getKey() + "=UNLIMITED");
						props.setProperty(s.getKey(), "UNLIMITED");
					}else if (s.getValue().getValueType() == Double.class && ((DoubleProp)(s.getValue())).getAuto()) {
						logger.log(Level.DEBUG, s.getKey() + "=AUTO");
						props.setProperty(s.getKey(), "AUTO");
					}else if (s.getValue().getValueType() == Double.class && ((DoubleProp)(s.getValue())).getUnlimited()) {
						logger.log(Level.DEBUG, s.getKey() + "=UNLIMITED");
						props.setProperty(s.getKey(), "UNLIMITED");
					}else{
						logger.log(Level.DEBUG, s.getKey() + "=" + s.getValue().getValue().toString());
						props.setProperty(s.getKey(), s.getValue().getValue().toString().trim());
					}
				} catch (Exception e) {
					logger.log(Level.DEBUG, s.getKey() + " has no associated property -> SKIP");
				}
			}

			props.save(outputFile);
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}

	}
}

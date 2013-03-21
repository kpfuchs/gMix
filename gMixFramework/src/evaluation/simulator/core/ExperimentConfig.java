/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2013  Karl-Peter Fuchs
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
 */
package evaluation.simulator.core;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import evaluation.simulator.pluginRegistry.PlotType;
import evaluation.simulator.pluginRegistry.StatisticsType;
import framework.core.config.Settings;


public class ExperimentConfig {

	public String simulationScript;
	
	public StatisticsType[] desiredStatisticsTypes;
	public PlotType[] desiredPlotTypes;
	
	public String propertyToVary;
	public String[] values;
	public boolean useSecondPropertyToVary;
	public String secondPropertyToVary;
	public String[] valuesForSecondProperty;
	
	public int runs;
	public String experimentStart;
	
	public boolean isNumeric = true;
	public HashMap<String, Double> propertyToVaryToId;
	
	
	// load experiment config from property file
	public ExperimentConfig(Settings settings) {
		
		this.simulationScript = settings.getProperty("SIMULATION_SCRIPT");
			
		if (settings.getProperty("DESIRED_EVALUATIONS").equals("ALL")) {
			this.desiredStatisticsTypes =  StatisticsType.values();
			for (StatisticsType st: StatisticsType.values())
				st.isActivated = true;
		} else {
			Vector<StatisticsType> tmp = new Vector<StatisticsType>();
			for (String desiredEvaluation: settings.getProperty("DESIRED_EVALUATIONS").split(","))
				for (StatisticsType cur: StatisticsType.values())
					if (cur.toString().equals(desiredEvaluation)) {
						tmp.add(cur);
						cur.isActivated = true;
						break;
					}
			this.desiredStatisticsTypes = tmp.toArray(new StatisticsType[0]);
			if (this.desiredStatisticsTypes.length == 0)
				throw new RuntimeException("unknown StatisticsType specified in config file (parameter DESIRED_EVALUATIONS): " +settings.getProperty("DESIRED_EVALUATIONS")); 		
		}
		HashSet<PlotType> tmpSet = new HashSet<PlotType>();
		for (StatisticsType st: desiredStatisticsTypes) 
			tmpSet.add(st.plotType);
		this.desiredPlotTypes = tmpSet.toArray(new PlotType[0]);
		
		this.propertyToVary = settings.getProperty("PROPERTY_TO_VARY");
		this.values = settings.getProperty("VALUES_FOR_THE_PROPERTY_TO_VARY").split(",");
		
		try {
			Double.parseDouble(values[0]);
		} catch(NumberFormatException e) {
			isNumeric = false;
		} 
		if (!isNumeric) {
			propertyToVaryToId = new HashMap<String, Double>();
			for (int i=0; i<values.length; i++) {
				if (values[i].contains(":")) {
					String[] res = values[i].split(":");
					assert res.length == 2;
					propertyToVaryToId.put(res[1], Double.parseDouble(res[0]));
					values[i] = res[1];
				} else {
					propertyToVaryToId.put(values[i], (double)i);
				}
			} 
		}
		
		this.useSecondPropertyToVary = 
			settings.getProperty("USE_SECOND_PROPERTY_TO_VARY").equals("TRUE");
		
		this.secondPropertyToVary = null;
		this.valuesForSecondProperty = null;
		
		if (useSecondPropertyToVary) {
			this.secondPropertyToVary = 
				settings.getProperty("SECOND_PROPERTY_TO_VARY");
			this.valuesForSecondProperty = 
				settings.getProperty(
						"VALUES_FOR_THE_SECOND_PROPERTY_TO_VARY").split(","
					);	
			
		}
		
		this.runs = new Integer(settings.getProperty("VALIDATION_RUNS")) +1;
		
		this.experimentStart = "";
		Calendar calendar = Calendar.getInstance();
		this.experimentStart += calendar.get(Calendar.YEAR) + "-";
		this.experimentStart += (calendar.get(Calendar.MONTH)+1) + "-";
		this.experimentStart += calendar.get(Calendar.DAY_OF_MONTH) + "_";
		this.experimentStart += calendar.get(Calendar.HOUR_OF_DAY) + "-";
		this.experimentStart += calendar.get(Calendar.MINUTE) + "_";
		this.experimentStart += calendar.get(Calendar.MILLISECOND);
		
		if (	this.useSecondPropertyToVary && 
				this.valuesForSecondProperty.length != this.values.length
				)
			
			throw new RuntimeException(	"ERROR: the same number of " +
										"values must be specified for " +
										"PROPERTY_TO_VARY and " +
										"SECOND_PROPERTY_TO_VARY in " +
										"property file!");
		
	}
	
}

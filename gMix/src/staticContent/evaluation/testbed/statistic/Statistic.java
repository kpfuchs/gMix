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
package staticContent.evaluation.testbed.statistic;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.log4j.Logger;

import staticContent.evaluation.simulator.core.statistics.GeneralStatistics;
import staticContent.evaluation.simulator.core.statistics.Statistics;
import staticContent.evaluation.testbed.core.ExperimentSeries;
import staticContent.evaluation.testbed.plan.XMLConfigReader;
import staticContent.evaluation.testbed.plan.global.GlobalExecutionPlan;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.StatisticsType;
import gnu.trove.TDoubleArrayList;

public class Statistic {
	protected Logger logger = Logger.getLogger(this.getClass());	
	protected GlobalExecutionPlan plan;	
	protected Set<Metric> wantedMetrics;
	protected Map<String, Set<Metric>> measuredMetrics_wantedMetricsMapping; // String -> sensor metric name	
	protected List<Map<Metric, Double>> aggregatedData;	
	protected Statistics statistics;	
	
	public Statistic(GlobalExecutionPlan plan) {
		this.wantedMetrics = plan.getEvalMetrics();
		this.plan = plan;
		
		measuredMetrics_wantedMetricsMapping = new HashMap<String, Set<Metric>>();
		
		for (Metric metric: this.wantedMetrics) {
			metric.activate();
			
			if (!measuredMetrics_wantedMetricsMapping.containsKey(metric.requiredMeasuredMetric)) {
				measuredMetrics_wantedMetricsMapping.put(metric.requiredMeasuredMetric, new HashSet<Metric>());
			}
			
			measuredMetrics_wantedMetricsMapping.get(metric.requiredMeasuredMetric).add(metric);
		}
	}
	
	protected void createStatisticsObject() {		
		statistics = new Statistics(null);
	}
	
	public void readInFile(File file, long startTime, long stopTime) throws FileNotFoundException {
		logger.debug("Read sensor file: "+file.getAbsolutePath());
		
		if (statistics == null) {
			createStatisticsObject();
		}
		
		Scanner sc = new Scanner(file);
        sc.useDelimiter("(\\n)");
        while(sc.hasNext()) {
            String line = sc.next();
            
            if(line.length() > 0 && line.startsWith("Sensor|")) {            	
            	line = line.substring(7);
            	
            	String measuredMetric = line.substring(0, line.indexOf('|'));
            	String unixTimeString = line.substring(measuredMetric.length()+1);
            		   unixTimeString = unixTimeString.substring(0, unixTimeString.indexOf('|'));
            	String measuredMetricValue = line.substring(measuredMetric.length()+unixTimeString.length()+2);
            	
            	long unixTime = Long.parseLong(unixTimeString);
            	
            	// skip the entries out of the experiment time (transient phase and end phase)            	
            	if (startTime != -1 && stopTime != -1 && (unixTime < startTime || unixTime > stopTime)) {
            		continue;
            	}
            	
            	System.out.println("sensor out: "+measuredMetricValue); 
            	
            	if (measuredMetrics_wantedMetricsMapping.containsKey(measuredMetric) && !measuredMetrics_wantedMetricsMapping.get(measuredMetric).isEmpty()) {
            		for (Metric wantedMetric: measuredMetrics_wantedMetricsMapping.get(measuredMetric)) {
            			switch(wantedMetric.inputDataType) {
            				case DOUBLE:
            					statistics.addValue(Double.parseDouble(measuredMetricValue), wantedMetric.statisticsTypeReference);
            					break;
            				case BIG_DECIMAL:
            					statistics.increment(Double.parseDouble(measuredMetricValue), wantedMetric.statisticsTypeReference);
            					break;
            				case BOOLEAN:
            					statistics.addValue(Boolean.parseBoolean(measuredMetricValue), wantedMetric.statisticsTypeReference);
            					break;
            				default:
                		}
            		}
            	}
            }                
        }
        
        sc.close();
	}
	
	public TDoubleArrayList[] calculate(int currentRunIndex) {
		TDoubleArrayList[] result = new TDoubleArrayList[StatisticsType.values().length];
		
		for (StatisticsType statType: plan.getDesiredStatisticTypes()) {
			result[statType.ordinal()] = GeneralStatistics.getResult(statType);
		}
		GeneralStatistics.reset();
		
		return result;
	}
	
	public static void main(String[] args) {
		ExperimentSeries.setGmixSpecificSettings();
		
		String configFilePath = System.getProperty("user.dir") +"/inputOutput/testbed/experimentDefinitions/test6experiment.xml";
		GlobalExecutionPlan plan = XMLConfigReader.createPlanV1(configFilePath);
//		plan.currentRunIndex = 0;
		
		Statistic stat = new Statistic(plan);
		
		try {
//			stat.readInFile(new File("C:/Users/dradoslav/cloudSync/seafile/workspaces/gMix_attached/inputOutput/testbed/tmp/process_5.log"), 9575211, 9619244);
//			stat.readInFile(new File("C:/Users/dradoslav/cloudSync/seafile/workspaces/gMix_attached/inputOutput/testbed/tmp/process_9.log"), 9575211, 9619244);
//			stat.readInFile(new File("C:/Users/dradoslav/cloudSync/seafile/workspaces/gMix_attached/inputOutput/testbed/tmp/process_19.log"), 9575211, 9619244);
//			stat.readInFile(new File("C:/Users/dradoslav/cloudSync/seafile/workspaces/gmixtest_old/coordinatorFolder/test-sensor-1.log"));
			
			for (int i=1; i<=6; i++) {
				stat.readInFile(new File(System.getProperty("user.dir") +"/inputOutput/testbed/tmp/sensor-"+i+".log"), 0, 100000000);
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		stat.calculate();
		
		TDoubleArrayList[] calc = stat.calculate(0);
		
System.out.println("calc: "+Arrays.toString(calc));
	}

	
}

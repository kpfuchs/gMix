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

import java.util.Calendar;
import java.util.Properties;

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.core.ExperimentConfig;
import staticContent.evaluation.simulator.core.statistics.GeneralStatistics;
import staticContent.evaluation.simulator.core.statistics.ResultSet;
import staticContent.evaluation.simulator.core.statistics.Statistics;
import staticContent.framework.config.Paths;
import staticContent.framework.config.Settings;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.StatisticsType;
import gnu.trove.TDoubleArrayList;

public class StatTest {

	public static void main(String[] args) {
		Simulator.settings = new Settings(Paths.SIM_PROPERTY_FILE_PATH);
		
		Properties properties = new Properties();		
		properties.setProperty("CALC_AVG_OF_RUNS", "false");
		properties.setProperty("IS_INVERSE", "false");
		properties.setProperty("SIM_OUTPUT_FOLDER_PATH", "./inputOutput/testbed/output/");
//		properties.setProperty("NAME_OF_PLOT_SCRIPT", "defaultPlotScript.txt");
		properties.setProperty("NAME_OF_PLOT_SCRIPT", "simguiPlotScript.txt");
		properties.setProperty("NONE_OVERWRITABLE_PARAMETERS", "");		
		
		Simulator.settings.addProperties(properties);
		
		Statistics stat = new Statistics(null);
		
		Statistics.setRecordStatistics(true);
		
		StatisticsType.TB_AVG_THROUGHPUT_RECEIVE.isActivated = true;

		stat.addValue(1000.0, StatisticsType.TB_AVG_THROUGHPUT_RECEIVE);
		stat.addValue(3000.0, StatisticsType.TB_AVG_THROUGHPUT_RECEIVE);
		stat.addValue(2000.0, StatisticsType.TB_AVG_THROUGHPUT_RECEIVE);
		stat.addValue(1000.0, StatisticsType.TB_AVG_THROUGHPUT_RECEIVE);
		stat.addValue(5000.0, StatisticsType.TB_AVG_THROUGHPUT_RECEIVE);
		
// System.out.println("contains data: "+stat.containsData(StatisticsType.SENSOR_AVG_THROUGHPUT_RECEIVE));
//		
// System.out.println("Result: "+stat.getResult(StatisticsType.SENSOR_AVG_THROUGHPUT_RECEIVE));		
//
// System.out.println("GeneralStatistics.getResult: "+GeneralStatistics.getResult(StatisticsType.SENSOR_AVG_THROUGHPUT_RECEIVE));
				
		
		StatisticsType[] desiredStatistics = {StatisticsType.TB_AVG_THROUGHPUT_RECEIVE};		
		String[] variableParameterValues = {"10","20","30","40","50"};
		int variableParameterValuesLength = variableParameterValues.length;
		
		
		// create Experiment config
		ExperimentConfig ep = new ExperimentConfig();
		
		ep.propertyToVary = "BATCH_SIZE";
		ep.values = variableParameterValues;
		ep.runs = 1;
		ep.desiredStatisticsTypes = desiredStatistics;
		
		ep.experimentStart = "";
		Calendar calendar = Calendar.getInstance();
		ep.experimentStart += calendar.get(Calendar.YEAR) + "-";
		ep.experimentStart += (calendar.get(Calendar.MONTH)+1) + "-";
		ep.experimentStart += calendar.get(Calendar.DAY_OF_MONTH) + "_";
		ep.experimentStart += calendar.get(Calendar.HOUR_OF_DAY) + "-";
		ep.experimentStart += calendar.get(Calendar.MINUTE) + "_";
		ep.experimentStart += calendar.get(Calendar.MILLISECOND);
		
		// create Experiment config	end
				
		
		ResultSet resultSet = new ResultSet(ep);
		TDoubleArrayList[][][] results = resultSet.results;
		
		// set simulation time to 1000, then it is neutral	
		for (int i=0; i<ep.values.length; i++) {
			for (int j=0; j<ep.runs; j++) {
				resultSet.simulationTime[i][j] = 1000;
			}
		}
		
		for (int i=0; i<variableParameterValuesLength; i++) {
			// calculate results
			for (StatisticsType st: desiredStatistics) {
				results[i][st.ordinal()][0] = GeneralStatistics.getResult(st);
			}
			GeneralStatistics.reset();
		}
		
		// add test entries
		results[1][63] = results[0][63];
		results[2][63] = results[0][63];
		results[3][63] = results[0][63];
		results[4][63] = results[0][63];
		
// System.out.println("ResultSet results[0][63]: "+Arrays.toString(results[0][63]));
		
		
		StatisticsType.TB_AVG_THROUGHPUT_RECEIVE.plotType.plot(resultSet);
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.exit(1);
	}
}

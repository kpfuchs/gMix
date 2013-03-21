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
package evaluation.simulator.plugins.plotType;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import evaluation.simulator.core.statistics.ResultSet;
import evaluation.simulator.core.statistics.aggregator.Aggregator;
import evaluation.simulator.core.statistics.postProcessor.PostProcessor;
import evaluation.simulator.pluginRegistry.PlotType;
import evaluation.simulator.pluginRegistry.StatisticsType;
import evaluation.simulator.pluginRegistry.StatisticsType.Unit;
import framework.core.config.Paths;
import gnu.trove.TDoubleArrayList;


public class TxtWriter extends Plotter {

	private BufferedWriter resultFile;
	private String fileNameOrPath;
	
	
	@Override
	public void plot(ResultSet resultSet) {
		for (String plotName:resultSet.getDesiredPlots(PlotType.TXT_ONLY)) { // for each plot (= file to write) 
			initResultFile(plotName, resultSet);
			StatisticsType[] desiredEvaluations = resultSet.getDesiredEvaluations(plotName);
			for (StatisticsType statisticsType:desiredEvaluations) { // for each StatisticsType
				appendLine("Results for StatisticsType " +statisticsType +" in " +statisticsType.unitAsString +":");
				for (int i=0; i<resultSet.ep.values.length; i++) { // for each varying_value
					appendLine(" Results for " +resultSet.ep.propertyToVary +"=" +resultSet.ep.values[i] +":");
					for (Aggregator ag: statisticsType.sourceAggregators) {
						appendLine("  Results for Aggregator " +ag +":");
						int runs = resultSet.getNumberOfValidationRuns() + 1;
						for (int j=0; j<runs; j++) {
							appendLine("   Results for run " +(j+1) +"/"+(runs+1) +":");
							if (!resultSet.containsData(i, statisticsType, j)) {
								System.err.println("    WARNING: no data recorded for " +statisticsType); 
								appendLine("    no data recorded");
							} else if (resultSet.isSingleValue(i, statisticsType, j)) {
								double res = resultSet.getResultSingleValue(i, statisticsType, j);
								res = performPostProcessing(res, statisticsType, resultSet, i, j);
								appendLine("    " +decimalFormat.format(res));
							} else {
								TDoubleArrayList res = resultSet.getResultArray(i, statisticsType, j);
								res = performPostProcessing(res, statisticsType, resultSet, i, j);
								if (ag == Aggregator.NONE) {
									for (int k=0; k<res.size(); k++)
										appendLine("    " +decimalFormat.format(res.get(k)));
								} else {
									double result = ag.aggregate(res);
									appendLine("    " +decimalFormat.format(result));
								}
								append("\n");
							}
						}
					}  
				}
				append("\n\n\n");
			}
			closeResultfile();
		}	
	}
	

	
	public void initResultFile(String plotName, ResultSet resultSet) {
		this.fileNameOrPath = Paths.SIM_OUTPUT_FOLDER_PATH +resultSet.ep.experimentStart +"-" +plotName +"-results.txt";
		try {
			this.resultFile = new BufferedWriter(new OutputStreamWriter(new DataOutputStream(new FileOutputStream(fileNameOrPath))));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(	"ERROR: could not write to file " +fileNameOrPath +"!");
		}
	}
	
	
	public void append(String content) {
		try {
			resultFile.write(content);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(	"ERROR: could not write to file " +fileNameOrPath +"!");
		}
	}
	
	
	public void appendLine(String content) {
		append("\n");
		append(content);
	}
	
	
	public void closeResultfile() {
		try {
			resultFile.flush();
			resultFile.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(	"ERROR: could not write to file " +fileNameOrPath +"!");
		}
	}
	
	
	private TDoubleArrayList performPostProcessing(TDoubleArrayList values, StatisticsType statisticsType, ResultSet resultSet, int varyingValueId, int runId) {
		for (int i=0; i<values.size(); i++)
			values.set(i, performPostProcessing(values.get(i), statisticsType, resultSet, varyingValueId, runId));
		for (PostProcessor pp:statisticsType.postProcessors)
			if (pp == PostProcessor.SORT)
				values.sort();
		return values;
	}
	
	
	private double performPostProcessing(double value, StatisticsType statisticsType, ResultSet resultSet, int varyingValueId, int runId) {
		if (statisticsType.postProcessors[0] != PostProcessor.NONE) {
			for (PostProcessor pp:statisticsType.postProcessors) {
				if (pp == PostProcessor.SORT) // sort not performed here (see above)
					continue;
				value = pp.process(value, resultSet, varyingValueId, runId);
			} 
		}
		if (statisticsType.unit == Unit.kbyte) {
			value = value / 1024d;
		} else if (statisticsType.unit == Unit.mbyte) {
			value = value / (1024d * 1024d);
		} else if (statisticsType.unit == Unit.gbyte) {
			value = value / (1024d * 1024d * 1024d);
		} else if (statisticsType.unit == Unit.sec) {
			value = value / 1000d;
		}
		return value;
	}

}

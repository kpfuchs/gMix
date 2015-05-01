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
package userGeneratedContent.simulatorPlugIns.plugins.plotType;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import staticContent.evaluation.simulator.core.statistics.ResultSet;
import staticContent.evaluation.simulator.core.statistics.aggregator.Aggregator;
import staticContent.evaluation.simulator.core.statistics.postProcessor.PostProcessor;
import staticContent.framework.config.Paths;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.PlotType;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.StatisticsType;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.StatisticsType.Unit;
import gnu.trove.TDoubleArrayList;

public class TxtWriter extends Plotter {

	private BufferedWriter resultFile;
	private String fileNameOrPath;


	@Override
	public void plot(ResultSet resultSet) {
		for (String plotName:resultSet.getDesiredPlots(PlotType.TXT_ONLY)) { // for each plot (= file to write)
			this.initResultFile(plotName, resultSet);
			StatisticsType[] desiredEvaluations = resultSet.getDesiredEvaluations(plotName);
			for (StatisticsType statisticsType:desiredEvaluations) { // for each StatisticsType
				this.appendLine("Results for StatisticsType " +statisticsType +" in " +statisticsType.unitAsString +":");
				for (int i=0; i<resultSet.ep.values.length; i++) { // for each varying_value
					this.appendLine(" Results for " +resultSet.ep.propertyToVary +"=" +resultSet.ep.values[i] +":");
					for (Aggregator ag: statisticsType.sourceAggregators) {
						this.appendLine("  Results for Aggregator " +ag +":");
						int runs = resultSet.getNumberOfValidationRuns() + 1;
						for (int j=0; j<runs; j++) {
							this.appendLine("   Results for run " +(j+1) +"/"+(runs+1) +":");
							if (!resultSet.containsData(i, statisticsType, j)) {
								System.err.println("    WARNING: no data recorded for " +statisticsType);
								this.appendLine("    no data recorded");
							} else if (resultSet.isSingleValue(i, statisticsType, j)) {
								double res = resultSet.getResultSingleValue(i, statisticsType, j);
								res = this.performPostProcessing(res, statisticsType, resultSet, i, j);
								this.appendLine("    " +decimalFormat.format(res));
							} else {
								TDoubleArrayList res = resultSet.getResultArray(i, statisticsType, j);
								res = this.performPostProcessing(res, statisticsType, resultSet, i, j);
								if (ag == Aggregator.NONE) {
									for (int k=0; k<res.size(); k++) {
										this.appendLine("    " +decimalFormat.format(res.get(k)));
									}
								} else {
									double result = ag.aggregate(res);
									this.appendLine("    " +decimalFormat.format(result));
								}
								this.append("\n");
							}
						}
					}
				}
				this.append("\n\n\n");
			}
			this.closeResultfile();
		}
	}



	public void initResultFile(String plotName, ResultSet resultSet) {
		this.fileNameOrPath = Paths.SIM_OUTPUT_FOLDER_PATH +resultSet.ep.experimentStart +"-" +plotName +"-results.txt";
		try {
			this.resultFile = new BufferedWriter(new OutputStreamWriter(new DataOutputStream(new FileOutputStream(this.fileNameOrPath))));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(	"ERROR: could not write to file " +this.fileNameOrPath +"!");
		}
	}


	public void append(String content) {
		try {
			this.resultFile.write(content);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(	"ERROR: could not write to file " +this.fileNameOrPath +"!");
		}
	}


	public void appendLine(String content) {
		this.append("\n");
		this.append(content);
	}


	public void closeResultfile() {
		try {
			this.resultFile.flush();
			this.resultFile.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(	"ERROR: could not write to file " +this.fileNameOrPath +"!");
		}
	}


	private TDoubleArrayList performPostProcessing(TDoubleArrayList values, StatisticsType statisticsType, ResultSet resultSet, int varyingValueId, int runId) {
		for (int i=0; i<values.size(); i++) {
			values.set(i, this.performPostProcessing(values.get(i), statisticsType, resultSet, varyingValueId, runId));
		}
		for (PostProcessor pp:statisticsType.postProcessors) {
			if (pp == PostProcessor.SORT) {
				values.sort();
			}
		}
		return values;
	}


	private double performPostProcessing(double value, StatisticsType statisticsType, ResultSet resultSet, int varyingValueId, int runId) {
		if (statisticsType.postProcessors[0] != PostProcessor.NONE) {
			for (PostProcessor pp:statisticsType.postProcessors) {
				if (pp == PostProcessor.SORT) {
					continue;
				}
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

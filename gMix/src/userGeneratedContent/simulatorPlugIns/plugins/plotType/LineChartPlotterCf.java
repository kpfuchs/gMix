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

import java.util.Arrays;

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.annotations.property.BoolSimulationProperty;
import staticContent.evaluation.simulator.annotations.property.StringSimulationProperty;
import staticContent.evaluation.simulator.core.statistics.ResultSet;
import staticContent.evaluation.simulator.core.statistics.aggregator.Aggregator;
import staticContent.evaluation.simulator.core.statistics.plotEngine.PlotScript;
import staticContent.evaluation.simulator.core.statistics.postProcessor.PostProcessor;
import staticContent.framework.util.Util;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.PlotType;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.StatisticsType;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.StatisticsType.Unit;

public class LineChartPlotterCf extends Plotter {

	@BoolSimulationProperty(name = "Calculate average",
			key = "CALC_AVG_OF_RUNS",
			inject = "1:PLOTTYPE,Plottype",
			isStatic = true)
	private final boolean CALC_AVG_OF_RUNS;

	@BoolSimulationProperty(name = "Inverse plot",
			key = "IS_INVERSE",
			inject = "2:PLOTTYPE,Plottype",
			isStatic = true)
	private final boolean IS_INVERSE;
	
	@StringSimulationProperty(name = "Overwritable parameters",
			key = "OVERWRITABLE_PARAMETERS",
			inject = "3:PLOTTYPE,Plottype",
			isStatic = true)
	private final String OVERWRITABLE_PARAMETERS = "";
	
	@StringSimulationProperty(name = "None overwritable parameters",
			key = "NONE_OVERWRITABLE_PARAMETERS",
			inject = "4:PLOTTYPE,Plottype",
			isStatic = true)
	private final String NONE_OVERWRITABLE_PARAMETERS = "";

	public LineChartPlotterCf() {
		this.CALC_AVG_OF_RUNS = Simulator.settings.getPropertyAsBoolean("CALC_AVG_OF_RUNS");
		this.IS_INVERSE = Simulator.settings.getPropertyAsBoolean("IS_INVERSE");
	}


	@Override
	public void plot(ResultSet resultSet) {
		for (String plotName:resultSet.getDesiredPlots(PlotType.LINE_CHART_CF)) { // for each plot (= diagram to display)
			StatisticsType[] desiredEvaluations = resultSet.getDesiredEvaluations(plotName);
			this.checkValidity(desiredEvaluations);
			PlotScript plotScript = new PlotScript(plotName, resultSet);
			int runs = resultSet.getNumberOfValidationRuns()+1;
			String aggregatorDescription;
			// create results for each plot line: (StatisticsType->varying_value->run)
			int linesInPlot = ((resultSet.getNumberOfValidationRuns() == 0) || this.CALC_AVG_OF_RUNS) ?
					desiredEvaluations.length * resultSet.ep.values.length :
						desiredEvaluations.length * resultSet.ep.values.length * runs;
			double[][] results = new double[linesInPlot][];
			String[] lineTitles = new String[linesInPlot];
			int plotLineCtr = -1;
			for (StatisticsType statisticsType:desiredEvaluations) { // for each StatisticsType
				aggregatorDescription = (statisticsType.sourceValueAggregator == Aggregator.NONE) ? "" : statisticsType.sourceValueAggregator.name() +" ";
				for (int i=0; i<resultSet.ep.values.length; i++) { // for each varying_value
					this.checkValidity(statisticsType, resultSet, i);
					if (runs == 1) { // no validation runs
						lineTitles[++plotLineCtr] = aggregatorDescription +", " +statisticsType;
						if (resultSet.ep.values.length != 1) {
							lineTitles[plotLineCtr] += " " +"(x=" +resultSet.ep.values[i] +")";
						}
						results[plotLineCtr] = resultSet.getResultArray(i, statisticsType, 0).toNativeArray();
						results[plotLineCtr] = this.performPostProcessing(results[plotLineCtr], statisticsType, resultSet, i);
					} else { // validation runs
						if (this.CALC_AVG_OF_RUNS) { // calc avg of all runs
							lineTitles[++plotLineCtr] = aggregatorDescription +", " +statisticsType;
							if (resultSet.ep.values.length != 1) {
								lineTitles[plotLineCtr] += " " +"(x=" +resultSet.ep.values[i] +")";
							}
							double[][] resultsOfAllRuns = new double[runs][];
							double[] avgResults = new double[resultSet.getResultArray(i, statisticsType, 0).size()];
							for (int j=0; j<runs; j++) {
								resultsOfAllRuns[j] = resultSet.getResultArray(i, statisticsType, j).toNativeArray();
							}
							for (int j=0; j<avgResults.length; j++) { // calc avg
								double sum = 0d;
								for (int k=0; k<runs; k++) {
									sum += resultsOfAllRuns[k][j];
								}
								avgResults[j] = sum / runs;
							}
							results[plotLineCtr] = this.performPostProcessing(avgResults, statisticsType, resultSet, i);
						} else { // !CALC_AVG_OF_RUNS -> plot a line for each run
							for (int j=0; j<runs; j++) {
								lineTitles[++plotLineCtr] = aggregatorDescription +", " +statisticsType;
								if (resultSet.ep.values.length != 1) {
									lineTitles[plotLineCtr] += " " +"(x=" +resultSet.ep.values[i] +")";
								}
								lineTitles[plotLineCtr] += ", RUN " +(j+1);
								results[plotLineCtr] = resultSet.getResultArray(i, statisticsType, j).toNativeArray();
								results[plotLineCtr] = this.performPostProcessing(results[plotLineCtr], statisticsType, resultSet, i);
							}
						}
					}
				}
			}
			// write results to result.txt:
			StringBuffer resultFileContent = new StringBuffer(10000);
			for (int i=0; i<results[0].length; i++) { // note that all arrays are of the same length
				if (this.IS_INVERSE) {
					for (double[] result : results) {
						resultFileContent.append(decimalFormat.format(result[i]) +" " +decimalFormat.format(1d-((double)(i+1)/result.length)) +" ");
					}
				} else {
					for (double[] result : results) {
						resultFileContent.append(decimalFormat.format(result[i]) +" " +decimalFormat.format((double)(i+1)/result.length) +" ");
					}
				}
				resultFileContent.append("\n");
			}
			resultFileContent.append("\n");
			plotScript.writeDataFileToDisk(resultFileContent.toString());
			// create plotscript:
			if (resultSet.ep.values.length != 1) {
				plotName += " - effect of " + resultSet.ep.propertyToVary +"(x)";
			}
			if ((resultSet.getNumberOfValidationRuns() > 0) && this.CALC_AVG_OF_RUNS) {
				plotName += " (AVG of " +runs +" runs)";
			}
			plotScript.setTitle(plotName);
			plotScript.setXlabel(desiredEvaluations[0].unitAsString);
			plotScript.setYlabel("Cumulative fraction");
			plotScript.setScale(desiredEvaluations[0].plotScale);
			String plotCommand = "plot";
			int columnCounter = 0;
			for (String lineTitle : lineTitles) {
				plotCommand += " varInputFile using " +(++columnCounter) +":" + (++columnCounter) + " w l lw 4  title '" +lineTitle + "',";
			}
			plotScript.setPlotCommand(plotCommand);
			plotScript.writePlotScriptToDisk();
			plotScript.plot();
		}
	}


	private void checkValidity(StatisticsType statisticsType, ResultSet resultSet, int varyingValueId) {
		if (statisticsType.sourceAggregators[0] != Aggregator.NONE) {
			System.err.println("WARNING: PlotType.LINE_CHART_CF does not support source aggregators " +
					"(not possible for CF), but a SourceAggregator is specified for " +statisticsType +
					".Will ignore the specified SourceAggregator.");
			statisticsType.sourceAggregators[0] = Aggregator.AVG;
		}
		if (!resultSet.containsData(varyingValueId, statisticsType, 0)) {
			System.err.println("WARNING: No results recorded for " +statisticsType +".\nWill set result to 0.0.");
		}
	}


	private void checkValidity(StatisticsType[] desiredEvaluations) {
		String lastDataType = null;
		for (StatisticsType st: desiredEvaluations) {
			if (lastDataType == null) {
				lastDataType = st.unitAsString;
			} else {
				if (!lastDataType.equals(st.unitAsString)) {
					throw new RuntimeException("ERROR: PlotType.LINE_CHART_CF " +
							"requires all lines of a plot to have the same data " +
							"type (unitAsString). " +st +" in plot "
							+st.destinationPlot +" has a different type. Make " +
							"sure to set it to " +lastDataType +", or select " +
							"another destinationPlot for " +st +" in StatisticsType.java.");
				}
			}
		}
	}

	private int lengthOfLast = Util.NOT_SET;
	private double[] performPostProcessing(double[] values, StatisticsType statisticsType, ResultSet resultSet, int varyingValueId) {
		if (this.lengthOfLast == Util.NOT_SET) {
			this.lengthOfLast = values.length;
		} else if (this.lengthOfLast != values.length) {
			throw new RuntimeException("ERROR: LineChartPlotterAbsCf cannot plot cf-lines " +
					"for StatisticsTypes with result sets of different size. Select another " +
					"destinationPlot for StatisticsType " +statisticsType +" in StatisticsType.java" +
					"\nFurther, make sure that no PostProcessor is selected, that depends on the " +
					"PROPERTY_TO_VARY (e.g. don't vary the number of users when the desired plot " +
					"shall display results PER_CLIENT)");
		}
		Arrays.sort(values); // sort is always required
		for (int k=0; k<values.length; k++) {
			values[k] = this.performPostProcessing(values[k], statisticsType, resultSet, varyingValueId, 0);
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
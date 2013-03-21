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

import java.util.Arrays;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.statistics.ResultSet;
import evaluation.simulator.core.statistics.aggregator.Aggregator;
import evaluation.simulator.core.statistics.plotEngine.PlotScript;
import evaluation.simulator.core.statistics.postProcessor.PostProcessor;
import evaluation.simulator.pluginRegistry.PlotType;
import evaluation.simulator.pluginRegistry.StatisticsType;
import evaluation.simulator.pluginRegistry.StatisticsType.Unit;
import framework.core.util.Util;


public class LineChartPlotterCf extends Plotter {

	private boolean CALC_AVG_OF_RUNS;
	private boolean IS_INVERSE;
	
	
	public LineChartPlotterCf() {
		this.CALC_AVG_OF_RUNS = Simulator.settings.getPropertyAsBoolean("CALC_AVG_OF_RUNS");
		this.IS_INVERSE = Simulator.settings.getPropertyAsBoolean("IS_INVERSE");
	}
	
	
	@Override
	public void plot(ResultSet resultSet) {
		for (String plotName:resultSet.getDesiredPlots(PlotType.LINE_CHART_CF)) { // for each plot (= diagram to display) 
			StatisticsType[] desiredEvaluations = resultSet.getDesiredEvaluations(plotName);
			checkValidity(desiredEvaluations);
			PlotScript plotScript = new PlotScript(plotName, resultSet);
			int runs = resultSet.getNumberOfValidationRuns()+1;
			String aggregatorDescription;
			// create results for each plot line: (StatisticsType->varying_value->run)
			int linesInPlot = (resultSet.getNumberOfValidationRuns() == 0 || CALC_AVG_OF_RUNS) ? 
					desiredEvaluations.length * resultSet.ep.values.length : 
					desiredEvaluations.length * resultSet.ep.values.length * runs; 
			double[][] results = new double[linesInPlot][];
			String[] lineTitles = new String[linesInPlot];
			int plotLineCtr = -1;
			for (StatisticsType statisticsType:desiredEvaluations) { // for each StatisticsType
				aggregatorDescription = (statisticsType.sourceValueAggregator == Aggregator.NONE) ? "" : statisticsType.sourceValueAggregator.name() +" "; 
				for (int i=0; i<resultSet.ep.values.length; i++) { // for each varying_value
					checkValidity(statisticsType, resultSet, i);
					if (runs == 1) { // no validation runs
						lineTitles[++plotLineCtr] = aggregatorDescription +", " +statisticsType;
						if (resultSet.ep.values.length != 1)
							lineTitles[plotLineCtr] += " " +"(x=" +resultSet.ep.values[i] +")";
						results[plotLineCtr] = resultSet.getResultArray(i, statisticsType, 0).toNativeArray();
						results[plotLineCtr] = performPostProcessing(results[plotLineCtr], statisticsType, resultSet, i);
					} else { // validation runs
						if (CALC_AVG_OF_RUNS) { // calc avg of all runs
							lineTitles[++plotLineCtr] = aggregatorDescription +", " +statisticsType;
							if (resultSet.ep.values.length != 1)
								lineTitles[plotLineCtr] += " " +"(x=" +resultSet.ep.values[i] +")";
							double[][] resultsOfAllRuns = new double[runs][];
							double[] avgResults = new double[resultSet.getResultArray(i, statisticsType, 0).size()];
							for (int j=0; j<runs; j++) // gather results
								resultsOfAllRuns[j] = resultSet.getResultArray(i, statisticsType, j).toNativeArray();
							for (int j=0; j<avgResults.length; j++) { // calc avg
								double sum = 0d;
								for (int k=0; k<runs; k++)
									sum += resultsOfAllRuns[k][j];	
								avgResults[j] = sum / (double)runs;
							} 
							results[plotLineCtr] = performPostProcessing(avgResults, statisticsType, resultSet, i);
						} else { // !CALC_AVG_OF_RUNS -> plot a line for each run
							for (int j=0; j<runs; j++) {
								lineTitles[++plotLineCtr] = aggregatorDescription +", " +statisticsType;
								if (resultSet.ep.values.length != 1)
									lineTitles[plotLineCtr] += " " +"(x=" +resultSet.ep.values[i] +")";
								lineTitles[plotLineCtr] += ", RUN " +(j+1);
								results[plotLineCtr] = resultSet.getResultArray(i, statisticsType, j).toNativeArray();
								results[plotLineCtr] = performPostProcessing(results[plotLineCtr], statisticsType, resultSet, i);
							}
						}
					}
				}
			}
			// write results to result.txt:
			StringBuffer resultFileContent = new StringBuffer(10000);
			for (int i=0; i<results[0].length; i++) { // note that all arrays are of the same length
				if (IS_INVERSE)
					for (int j=0; j<results.length; j++)
						resultFileContent.append(decimalFormat.format(results[j][i]) +" " +decimalFormat.format(1d-((double)(i+1)/results[j].length)) +" ");
				else 
					for (int j=0; j<results.length; j++)
						resultFileContent.append(decimalFormat.format(results[j][i]) +" " +decimalFormat.format((double)(i+1)/results[j].length) +" ");
				resultFileContent.append("\n");
			}
			resultFileContent.append("\n");
			plotScript.writeDataFileToDisk(resultFileContent.toString());
			// create plotscript:
			if (resultSet.ep.values.length != 1)
				plotName += " - effect of " + resultSet.ep.propertyToVary +"(x)";
			if (resultSet.getNumberOfValidationRuns() > 0 && CALC_AVG_OF_RUNS)
				plotName += " (AVG of " +runs +" runs)";
			plotScript.setTitle(plotName);
			plotScript.setXlabel(desiredEvaluations[0].unitAsString);
			plotScript.setYlabel("Cumulative fraction");
			plotScript.setScale(desiredEvaluations[0].plotScale);
			String plotCommand = "plot";
			int columnCounter = 0;
			for (int i=0; i<lineTitles.length; i++)
				plotCommand += " varInputFile using " +(++columnCounter) +":" + (++columnCounter) + " w l lw 4  title '" +lineTitles[i] + "',";
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
				if (!lastDataType.equals(st.unitAsString))
					throw new RuntimeException("ERROR: PlotType.LINE_CHART_CF " +
							"requires all lines of a plot to have the same data " +
							"type (unitAsString). " +st +" in plot " 
							+st.destinationPlot +" has a different type. Make " +
							"sure to set it to " +lastDataType +", or select " +
							"another destinationPlot for " +st +" in StatisticsType.java."); 
			}
		}
	}

	
	private int lengthOfLast = Util.NOT_SET;
	private double[] performPostProcessing(double[] values, StatisticsType statisticsType, ResultSet resultSet, int varyingValueId) {
		if (lengthOfLast == Util.NOT_SET) {
			lengthOfLast = values.length;
		} else if (lengthOfLast != values.length) {
			throw new RuntimeException("ERROR: LineChartPlotterAbsCf cannot plot cf-lines " +
					"for StatisticsTypes with result sets of different size. Select another " +
					"destinationPlot for StatisticsType " +statisticsType +" in StatisticsType.java" +
					"\nFurther, make sure that no PostProcessor is selected, that depends on the " +
					"PROPERTY_TO_VARY (e.g. don't vary the number of users when the desired plot " +
					"shall display results PER_CLIENT)"); 
		}
		Arrays.sort(values); // sort is always required
		for (int k=0; k<values.length; k++)
			values[k] = performPostProcessing(values[k], statisticsType, resultSet, varyingValueId, 0);
		return values;
	}
	
	
	private double performPostProcessing(double value, StatisticsType statisticsType, ResultSet resultSet, int varyingValueId, int runId) {
		if (statisticsType.postProcessors[0] != PostProcessor.NONE) {
			for (PostProcessor pp:statisticsType.postProcessors) {
				if (pp == PostProcessor.SORT) // sorting not done here
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
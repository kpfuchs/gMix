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

import java.util.HashMap;

import evaluation.simulator.core.statistics.ResultSet;
import evaluation.simulator.core.statistics.aggregator.Aggregator;
import evaluation.simulator.core.statistics.plotEngine.PlotScript;
import evaluation.simulator.core.statistics.postProcessor.PostProcessor;
import evaluation.simulator.pluginRegistry.PlotType;
import evaluation.simulator.pluginRegistry.StatisticsType;
import evaluation.simulator.pluginRegistry.StatisticsType.Unit;
import gnu.trove.TDoubleArrayList;


public class MultiPlotter extends Plotter {

	public enum PlotStyle {LINE_CHART_ABS, HISTOGRAM};
	
	private PlotStyle plotStyle;
	
	
	public MultiPlotter(PlotStyle plotStyle) {
		this.plotStyle = plotStyle;
	}
	
	
	@Override
	public void plot(ResultSet resultSet) {
		PlotType plotType;
		if (plotStyle == PlotStyle.LINE_CHART_ABS)
			plotType = PlotType.LINE_CHART_ABS;
		else if (plotStyle == PlotStyle.HISTOGRAM)
			plotType = PlotType.HISTOGRAM;
		else
			throw new RuntimeException("ERROR: no case for PlotStyle " +plotStyle +" added yet."); 
		for (String plotName:resultSet.getDesiredPlots(plotType)) { // for each plot (= diagram to display) 
			StatisticsType[] desiredEvaluations = resultSet.getDesiredEvaluations(plotName);
			checkValidity(desiredEvaluations);
			PlotScript plotScript = new PlotScript(plotName, resultSet);
			String plotCommand = "plot";
			String plotstyle = (resultSet.getNumberOfValidationRuns() == 0) ? "linespoints" : "yerrorlines";
			int columnCounter; // for plot command
			if (plotStyle == PlotStyle.LINE_CHART_ABS)
				columnCounter = 1;
			else if (plotStyle == PlotStyle.HISTOGRAM)
				columnCounter = 0;
			else
				throw new RuntimeException("ERROR: no case for PlotStyle " +plotStyle +" added yet."); 
			if (resultSet.ep.values.length > 1)
				plotName +=" - effect of " + resultSet.ep.propertyToVary;
			if (!resultSet.ep.isNumeric) {
				plotName += " (";
				for (int i=0; i<resultSet.ep.values.length ; i++) {
					double id = resultSet.ep.propertyToVaryToId.get(resultSet.ep.values[i]);
					plotName += ""+decimalFormat.format(id) +"=" +resultSet.ep.values[i];
					if (i <(resultSet.ep.values.length-1))
						plotName += ",";
				} 
				plotName += ")";
			}
			plotScript.setTitle(plotName);
			plotScript.setXlabel(resultSet.ep.propertyToVary);
			plotScript.setYlabel(desiredEvaluations[0].unitAsString);
			plotScript.setScale(desiredEvaluations[0].plotScale);
			if (plotStyle == PlotStyle.HISTOGRAM) {
				plotScript.setNoneOverwritableParameter("set style data histogram");
				plotScript.setNoneOverwritableParameter("set style histogram cluster gap 1");
				if (resultSet.getNumberOfValidationRuns() > 0)
					plotScript.setNoneOverwritableParameter("set style histogram errorbars linewidth 1");
				plotScript.setNoneOverwritableParameter("set style fill solid border -1");
				plotScript.setNoneOverwritableParameter("set boxwidth 0.7");
				plotScript.setNoneOverwritableParameter("set bars front");
				plotScript.setNoneOverwritableParameter("set key outside center bottom samplen 0");
			}
			StringBuffer resultFileContent = new StringBuffer(10000);
			String aggregatorDescription = "";
			String plotLineTitle = "";
			for (int i=0; i<resultSet.ep.values.length; i++) { // for each varying_value
				// calculate results:
				if (resultSet.ep.isNumeric)
					resultFileContent.append(resultSet.ep.values[i]);
				else
					resultFileContent.append(resultSet.ep.propertyToVaryToId.get(resultSet.ep.values[i]));
				for (StatisticsType statisticsType:desiredEvaluations) { // for each StatisticsType 
					checkValidity(statisticsType, resultSet, i);
					HashMap<Aggregator, TDoubleArrayList> resultsForAllRuns = resultSet.getResultsForAllRuns(i, statisticsType);
					for (Aggregator ag:resultsForAllRuns.keySet()) { // for each aggregation type (= each line in the plot)
						if (i == 0) {
							aggregatorDescription = (ag.name().equals(statisticsType.sourceValueAggregator.name())) ? ag.name() : ag.name() +" " +statisticsType.sourceValueAggregator.name();
							plotLineTitle = aggregatorDescription +" (" +statisticsType.name() +")";
						}
						TDoubleArrayList resultForAggregator = resultsForAllRuns.get(ag);
						if (resultForAggregator.size() == 1) { // no validation runs
							double result = performPostProcessing(resultForAggregator.get(0), statisticsType, resultSet, i, 0);
							resultFileContent.append(" " +decimalFormat.format(result));
							if (i == 0)
								if (plotStyle == PlotStyle.LINE_CHART_ABS)
									plotCommand += 	" varInputFile using 1:" + (++columnCounter) + " w " +plotstyle +" title '" +plotLineTitle + "',";
								else if (plotStyle == PlotStyle.HISTOGRAM)
									plotCommand +=	" varInputFile using " +(++columnCounter +1) +":xtic(1) title '" +plotLineTitle + "',";
								else
									throw new RuntimeException("ERROR: no case for PlotStyle " +plotStyle +" added yet."); 
						} else { // validation runs
							double[] result = new double[3]; // 0:avg, 1:min, 2:max
							resultForAggregator = performPostProcessing(resultForAggregator, statisticsType, resultSet, i);
							result[0] = Aggregator.AVG.aggregate(resultForAggregator);
							result[1] = Aggregator.MIN.aggregate(resultForAggregator);
							result[2] = Aggregator.MAX.aggregate(resultForAggregator);
							resultFileContent.append(" " +decimalFormat.format(result[0]) +" " +decimalFormat.format(result[1]) +" " +decimalFormat.format(result[2]));
							if (i == 0)
								if (plotStyle == PlotStyle.LINE_CHART_ABS)
									plotCommand +=	" varInputFile using 1:" +(++columnCounter) +":" +(++columnCounter) +":" +(++columnCounter) +" w " +plotstyle +" title '" +plotLineTitle + "',";
								else if (plotStyle == PlotStyle.HISTOGRAM)
									plotCommand +=	" varInputFile using " +(++columnCounter +1) +":" +(++columnCounter +1) +":" +(++columnCounter +1) +":xtic(1) title '" +plotLineTitle + "',";
								else
									throw new RuntimeException("ERROR: no case for PlotStyle " +plotStyle +" added yet."); 
						}
					}
				} 
				resultFileContent.append("\n");
			} 
			resultFileContent.append("\n");
			plotScript.setPlotCommand(plotCommand);
			plotScript.writeDataFileToDisk(resultFileContent.toString());
			plotScript.writePlotScriptToDisk();
			plotScript.plot();
		}	
	}
	
	
	private void checkValidity(StatisticsType[] desiredEvaluations) {
		Unit lastUnit = null;
		for (StatisticsType st: desiredEvaluations) {
			if (lastUnit == null) {
				lastUnit = st.unit;
			} else {
				if (lastUnit != st.unit)
					throw new RuntimeException("ERROR: PlotType." +plotStyle +" " +
							"requires all lines of a plot to have the same unit. " 
							+st +" in plot " +st.destinationPlot +" has a different " +
							"unit. Make " +"sure to set it to " +lastUnit +", or select " +
							"another destinationPlot for " +st +" in StatisticsType.java."); 
			}
		}
	}
	
	
	private void checkValidity(StatisticsType statisticsType, ResultSet resultSet, int varyingValueId) {
		if (statisticsType.sourceAggregators[0] == Aggregator.NONE) {
			System.err.println("WARNING: PlotType." +plotStyle +" requires at least one SourceAggregator, " +
					"but no SourceAggregator is specified for StatisticsType " +statisticsType +".\n" +
					"Will use the default SourceAggregator: AVG"); 
			statisticsType.sourceAggregators[0] = Aggregator.AVG;
		} else {
			for (PostProcessor pp:statisticsType.postProcessors)
				if (pp == PostProcessor.SORT)
					System.err.println("WARNING: PlotType." +plotStyle +" does not support sorting, but SORT is " +
							"selected as PostProcessor for StatisticsType " +statisticsType +".\n" +
							"Will ignore the SORT command.");
		}
		if (!resultSet.containsData(varyingValueId, statisticsType, 0)) {
			System.err.println("WARNING: No results recorded for " +statisticsType +".\nWill set result to 0.0.");
		}
	}
	
	
	private TDoubleArrayList performPostProcessing(TDoubleArrayList values, StatisticsType statisticsType, ResultSet resultSet, int varyingValueId) {
		for (int i=0; i<values.size(); i++)
			values.set(i, performPostProcessing(values.get(i), statisticsType, resultSet, varyingValueId, i));
		return values;
	}
	
	
	private double performPostProcessing(double value, StatisticsType statisticsType, ResultSet resultSet, int varyingValueId, int runId) {
		if (statisticsType.postProcessors[0] != PostProcessor.NONE) {
			for (PostProcessor pp:statisticsType.postProcessors) {
				if (pp == PostProcessor.SORT) // results are already sorted by "varying_value" (see checkValidity() method)
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

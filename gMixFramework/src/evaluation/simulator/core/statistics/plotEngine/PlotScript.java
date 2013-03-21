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
package evaluation.simulator.core.statistics.plotEngine;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.statistics.ResultSet;
import framework.core.config.Paths;
import framework.core.util.Util;


public class PlotScript {

	private String plotScript;
	private String resultFileName;
	private String plotScriptFileName;
	private String resultDiagramFileName;
	private String gnuplotConsoleOutputFileName;
	
	
	public PlotScript(String plotName, ResultSet resultSet) {
		this.plotScript = Util.getFileContent(Paths.SIM_PLOTSCRIPT_FOLDER_PATH +Simulator.settings.getProperty("NAME_OF_PLOT_SCRIPT"));
		this.resultFileName = resultSet.ep.experimentStart +"-" +plotName +"-results.txt";
		this.plotScriptFileName = resultSet.ep.experimentStart +"-" +plotName +"-plotScript.txt";
		this.resultDiagramFileName = resultSet.ep.experimentStart +"-" +plotName +"-diagram.eps";
		this.gnuplotConsoleOutputFileName = resultSet.ep.experimentStart +"-" +plotName +"-gnuPlotConsoleOutput";
		setInputFile(resultFileName);
	}
	
	
	public String getResultFileName() {
		return this.resultFileName;
	}
	
	
	public String getPlotScriptFileName() {
		return this.plotScriptFileName;
	}
	
	
	public void setTitle(String title) {
		plotScript = plotScript.replace(
				"varTitle = \"WILL_BE_SET_AUTOMATICALLY\"", 
				"varTitle = \"" +title +"\""
				);
	}


	public void setXlabel(String xLabel) {
		plotScript = plotScript.replace(
				"varXLabel = \"WILL_BE_SET_AUTOMATICALLY\"", 
				"varXLabel = \"" +xLabel +"\""
				);
	}
	
	
	public void setYlabel(String yLabel) {
		plotScript = plotScript.replace(
				"varYLabel = \"WILL_BE_SET_AUTOMATICALLY\"", 
				"varYLabel = \"" +yLabel +"\""
				);
	}
	
	
	public void setScale(PlotScale scale) {
		if (!scale.getGnuplotCommand().equals(""))
			plotScript = plotScript.replace(
					"#_VAR", 
					"#_VAR\n" +scale.getGnuplotCommand()
					);
	}
	
	
	public void setOverwritableParameter(String parameter) {
		plotScript = plotScript.replace(
				"#_OVERWRITABLE_PARAMETERS", 
				parameter + "\n#_OVERWRITABLE_PARAMETERS"
				);
	}
	
	
	public void setNoneOverwritableParameter(String parameter) {
		plotScript = plotScript.replace(
				"#_NONE_OVERWRITABLE_PARAMETERS", 
				parameter + "\n#_NONE_OVERWRITABLE_PARAMETERS"
				);
	}
	
	
	private void setInputFile(String inputFile) {
		plotScript = plotScript.replace(
				"varInputFile = \"WILL_BE_SET_AUTOMATICALLY\"", 
				"varInputFile = \"" +inputFile +"\""
				);
		plotScript = plotScript.replace(
				"set output \"WILL_BE_SET_AUTOMATICALLY\"",
				"set output \"" +resultDiagramFileName +"\""
				);
	}
	
	
	public void setPlotCommand(String plotCommand) {
		if (plotCommand.endsWith(","))
			plotCommand = plotCommand.substring(0, (plotCommand.length()-1));
		plotScript = plotScript.replace(
				"plot \"WILL_BE_SET_AUTOMATICALLY\"",
				plotCommand
				);
	}

	
	public void writeDataFileToDisk(String content) {
		Util.writeToFile(content, Paths.SIM_OUTPUT_FOLDER_PATH +resultFileName);
	}
	
	
	public void writePlotScriptToDisk() {
		String oParams = Simulator.settings.getProperty("OVERWRITABLE_PARAMETERS");
		if (!oParams.equals(""))
			setOverwritableParameter(oParams);
		String noParams = Simulator.settings.getProperty("NONE_OVERWRITABLE_PARAMETERS");
		if (!noParams.equals(""))
			setNoneOverwritableParameter(noParams);
		Util.writeToFile(plotScript, Paths.SIM_OUTPUT_FOLDER_PATH +this.plotScriptFileName);
	}
	
	
	public void plot() {
		new GnuPlotTask(plotScriptFileName, gnuplotConsoleOutputFileName).start();
	}
	
}

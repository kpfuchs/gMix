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
package staticContent.evaluation.simulator.core.statistics.plotEngine;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.core.binding.gMixBinding;
import staticContent.evaluation.simulator.core.statistics.ResultSet;
import staticContent.evaluation.simulator.gui.launcher.GuiLauncher;
import staticContent.framework.config.Paths;
import staticContent.framework.util.Util;

public class PlotScript {

	private static Logger logger = Logger.getLogger(PlotScript.class);

	private String plotScript;
	private final String resultFileName;
	private final String plotScriptFileName;
	private final String resultDiagramFileName;
	private final String gnuplotConsoleOutputFileName;

	public PlotScript(String plotName, ResultSet resultSet) {
		if (GuiLauncher.guiActive)
			this.plotScript = Util.getFileContent(Paths.SIM_PLOTSCRIPT_FOLDER_PATH + "simguiPlotScript.txt");
		else
			this.plotScript = Util.getFileContent(Paths.SIM_PLOTSCRIPT_FOLDER_PATH + Simulator.settings.getProperty("NAME_OF_PLOT_SCRIPT"));
		this.resultFileName = resultSet.ep.experimentStart + "-" + plotName + "-results.txt";
		this.plotScriptFileName = resultSet.ep.experimentStart + "-" + plotName + "-plotScript.txt";
		this.resultDiagramFileName = resultSet.ep.experimentStart + "-" + plotName + "-diagram";
		this.gnuplotConsoleOutputFileName = resultSet.ep.experimentStart + "-" + plotName + "-gnuPlotConsoleOutput";
		this.setInputFile(this.resultFileName);
	}

	public String getResultFileName() {
		return this.resultFileName;
	}

	public String getPlotScriptFileName() {
		return this.plotScriptFileName;
	}

	public void setTitle(String title) {
		this.plotScript = this.plotScript.replace("varTitle = \"WILL_BE_SET_AUTOMATICALLY\"", "varTitle = \"" + title
				+ "\"");
	}

	public void setXlabel(String xLabel) {
		this.plotScript = this.plotScript.replace("varXLabel = \"WILL_BE_SET_AUTOMATICALLY\"", "varXLabel = \""
				+ xLabel + "\"");
	}

	public void setYlabel(String yLabel) {
		this.plotScript = this.plotScript.replace("varYLabel = \"WILL_BE_SET_AUTOMATICALLY\"", "varYLabel = \""
				+ yLabel + "\"");
	}

	public void setScale(PlotScale scale) {
		if (!scale.getGnuplotCommand().equals("")) {
			this.plotScript = this.plotScript.replace("#_VAR", "#_VAR\n" + scale.getGnuplotCommand());
		}
	}

	public void setOverwritableParameter(String parameter) {
		this.plotScript = this.plotScript.replace("#_OVERWRITABLE_PARAMETERS", parameter
				+ "\n#_OVERWRITABLE_PARAMETERS");
	}

	public void setNoneOverwritableParameter(String parameter) {
		this.plotScript = this.plotScript.replace("#_NONE_OVERWRITABLE_PARAMETERS", parameter
				+ "\n#_NONE_OVERWRITABLE_PARAMETERS");
	}

	private void setInputFile(String inputFile) {
		this.plotScript = this.plotScript.replace("varInputFile = \"WILL_BE_SET_AUTOMATICALLY\"", "varInputFile = \""
				+ inputFile + "\"");
		this.plotScript = this.plotScript.replace("set output \"1WILL_BE_SET_AUTOMATICALLY\"", "set output \""
				+ this.resultDiagramFileName + ".svg\"");
		this.plotScript = this.plotScript.replace("set output \"2WILL_BE_SET_AUTOMATICALLY\"", "set output \""
				+ this.resultDiagramFileName + ".png\"");
		this.plotScript = this.plotScript.replace("set output \"WILL_BE_SET_AUTOMATICALLY\"", "set output \""
				+ this.resultDiagramFileName + ".eps\"");
	}

	public void setPlotCommand(String plotCommand) {
		if (plotCommand.endsWith(",")) {
			plotCommand = plotCommand.substring(0, (plotCommand.length() - 1));
		}
		this.plotScript = this.plotScript.replace("plot \"WILL_BE_SET_AUTOMATICALLY\"", plotCommand);
	}

	public void writeDataFileToDisk(String content) {
		Util.writeToFile(content, Paths.SIM_OUTPUT_FOLDER_PATH + this.resultFileName);
	}

	public void writePlotScriptToDisk() {
		String oParams = Simulator.settings.getProperty("OVERWRITABLE_PARAMETERS");

		if (oParams == null) {
			oParams = "";
		}

		if (!oParams.equals("")) {
			this.setOverwritableParameter(oParams);
		}
		String noParams = Simulator.settings.getProperty("NONE_OVERWRITABLE_PARAMETERS");
		if (!noParams.equals("")) {
			this.setNoneOverwritableParameter(noParams);
		}
		this.plotScript = StringEscapeUtils.unescapeJava(this.plotScript);
		Util.writeToFile(this.plotScript, Paths.SIM_OUTPUT_FOLDER_PATH + this.plotScriptFileName);
	}

	public void plot() {
		logger.log(Level.DEBUG, this.plotScript);
		new GnuPlotTask(this.plotScriptFileName, this.gnuplotConsoleOutputFileName).start();
		if (GuiLauncher.guiActive)
			gMixBinding.createResult(this.resultDiagramFileName + ".svg");
		logger.log(Level.DEBUG, "SVG: " + this.resultDiagramFileName);
	}

}

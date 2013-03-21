/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2012  Karl-Peter Fuchs
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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import framework.core.config.Paths;
import framework.core.config.Settings;
import framework.core.util.Util;


public class GnuPlotTask extends Thread {
	
	private static boolean isFirstExecution = true;
	private static Object monitor = new Object();
	
	private String plotScriptName;
	private String gnuplotConsoleOutputFileName;
	

	public GnuPlotTask(String plotScriptName, String gnuplotConsoleOutputFileName) {
		this.plotScriptName = plotScriptName;
		this.gnuplotConsoleOutputFileName = gnuplotConsoleOutputFileName;
	}
	
	
	@Override
	public void run() {
		synchronized (monitor) {
			if (isFirstExecution == true) { // some plot-viewers don't like to be executed to fast in parallel...
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		String gnuplotOutput = "";
		String gnuplotErrorOutput = ""; 

		try {
			String line;
			String gnuPlotFolder = Settings.getPropertyFromFile(Paths.SIM_PROPERTY_FILE_PATH, "GNUPLOT_FOLDER");
			Process process = Runtime.getRuntime().exec(
					gnuPlotFolder + "gnuplot -persist " +plotScriptName,
					null, 
					new File(Paths.SIM_OUTPUT_FOLDER_PATH)
					);
			
			process.waitFor();
			
			BufferedReader input = new BufferedReader(
					new InputStreamReader(process.getInputStream()));
			BufferedReader errorInput = new BufferedReader(
					new InputStreamReader(process.getErrorStream()));

			while ((line = input.readLine()) != null)
				gnuplotOutput += line;

			while ((line = errorInput.readLine()) != null)
				gnuplotErrorOutput += line;

			input.close();

		} catch (Exception e) {
			synchronized (monitor) {
				if (isFirstExecution == true) {
					isFirstExecution = false;
					System.err.println(
							"\nThe simulator could not plot the results as gnuplot was not found on your system\n" +
							"or a gnuplot error occured (see the more detailed error log below.). Make sure you\n" +
							"have gnuplot installed and set the variable \"GNUPLOT_FOLDER\" in \"./inputOutput/\n" +
							"simulator/config/simulatorConfig.txt\" to point at the folder of the gnuplot executable\n" +
							"(e.g. \"GNUPLOT_FOLDER = /opt/local/bin/\")\n" +
							"Note that the results of this run (plotscripts, recorded statistics as txt and a\n" +
							"config dump) are not lost, but stored in \"./inputOutput/simulator/output/\".\n\n\n" +
							"Detailed error log:");
					e.printStackTrace();
				}	
			}
		}

		if (!gnuplotOutput.equals("") || !gnuplotErrorOutput.equals("")) {
			String gnuplotResult = 
					"\nGnuplot output (stdout):\n" + gnuplotOutput
					+"\nGnuplot output (error):\n" + gnuplotErrorOutput;
			if (!gnuplotErrorOutput.equals(""))
				System.err.println(gnuplotResult); 
			else
				System.out.println(gnuplotResult); 
			
			Util.writeToFile(gnuplotResult, Paths.SIM_OUTPUT_FOLDER_PATH +gnuplotConsoleOutputFileName +"-" +Thread.currentThread().getId() +".txt");
			
		}
	}
	
}

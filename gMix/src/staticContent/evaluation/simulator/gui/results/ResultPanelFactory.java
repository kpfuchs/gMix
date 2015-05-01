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
package staticContent.evaluation.simulator.gui.results;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;

/**
 * @author Infectiou
 * 
 */
public class ResultPanelFactory {

	/**
	 * @return
	 */
	public static JPanel getResultPanel() {
		return new ChartPanel(LineJFreeChartCreator.createAChart());
		// return new TextResult();
	}

	/**
	 * @param gnuplotResultFileName
	 * @return an instance of {@link GnuPlotPanel}
	 */
	public static JPanel getGnuplotResultPanel(String gnuplotResultFileName) {
		return new GnuplotPanel(gnuplotResultFileName);
	}
}

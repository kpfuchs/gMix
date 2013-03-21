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
package evaluation.simulator.pluginRegistry;

import evaluation.simulator.core.statistics.ResultSet;
import evaluation.simulator.plugins.plotType.LineChartPlotterCf;
import evaluation.simulator.plugins.plotType.MultiPlotter;
import evaluation.simulator.plugins.plotType.Plotter;
import evaluation.simulator.plugins.plotType.TxtWriter;
import evaluation.simulator.plugins.plotType.MultiPlotter.PlotStyle;


public enum PlotType {

	TXT_ONLY(new TxtWriter()),
	LINE_CHART_CF(new LineChartPlotterCf()),
	LINE_CHART_ABS(new MultiPlotter(PlotStyle.LINE_CHART_ABS)),
	HISTOGRAM(new MultiPlotter(PlotStyle.HISTOGRAM))
	;
	

	private Plotter plotter;
	
	
	private PlotType(Plotter plotter) {
		this.plotter = plotter;
	}
	
	
	public void plot(ResultSet resultSet) {
		plotter.plot(resultSet);
	}
	
}

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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class LineJFreeChartCreator {

	public static JFreeChart createAChart() {
		final XYDataset dataset = createDataset();
		final JFreeChart chart = createChart(dataset);
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		return chart;
	}

	/**
	 * Creates a chart.
	 * 
	 * @param dataset
	 *            the data for the chart.
	 * 
	 * @return a chart.
	 */
	private static JFreeChart createChart(final XYDataset dataset) {

		final JFreeChart chart = ChartFactory.createXYLineChart(
				"Latency Mix Message", // chart title
				"BATCH_SIZE", // x axis label
				"ms", // y axis label
				dataset, // data
				PlotOrientation.VERTICAL, true, // include legend
				true, // tooltips
				false // urls
				);

		chart.setBackgroundPaint(Color.white);

		// Spezial �berschreiben des Aussehen der Striche im Graphen
		final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer() {
			private static final long serialVersionUID = 1L;
			Stroke soild = new BasicStroke(2.0f);

			@Override
			public Stroke getItemStroke(int row, int column) {
				return this.soild;
			}
		};

		final XYPlot plot = chart.getXYPlot();
		plot.setRenderer(renderer);

		return chart;

	}

	private static XYDataset createDataset() {

		final XYSeries series1 = new XYSeries("AVG");
		series1.add(1.0, 1.0);
		series1.add(2.0, 4.0);
		series1.add(3.0, 3.0);
		series1.add(4.0, 5.0);
		series1.add(5.0, 5.0);
		series1.add(6.0, 7.0);
		series1.add(7.0, 7.0);
		series1.add(8.0, 8.0);

		final XYSeries series2 = new XYSeries("Min AVG");
		series2.add(1.0, 0.5);
		series2.add(2.0, 2.0);
		series2.add(3.0, 1.5);
		series2.add(4.0, 2.5);
		series2.add(5.0, 2.5);
		series2.add(6.0, 3.5);
		series2.add(7.0, 3.5);
		series2.add(8.0, 4.0);

		final XYSeries series3 = new XYSeries("Max AVG");
		series3.add(1.0, 2.0);
		series3.add(2.0, 8.0);
		series3.add(3.0, 6.0);
		series3.add(4.0, 10.0);
		series3.add(5.0, 10.0);
		series3.add(6.0, 14.0);
		series3.add(7.0, 14.0);
		series3.add(8.0, 16.0);

		final XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(series1);
		dataset.addSeries(series2);
		dataset.addSeries(series3);
		return dataset;

	}

}

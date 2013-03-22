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
package evaluation.simulator.pluginRegistry;

import evaluation.simulator.core.statistics.aggregator.Aggregator;
import evaluation.simulator.core.statistics.aggregator.Aggregator.InputDataType;
import evaluation.simulator.core.statistics.plotEngine.PlotScale;
import evaluation.simulator.core.statistics.postProcessor.PostProcessor;


public enum StatisticsType {
	
	AVG_CLIENT_RTT_LAYER5MESSAGE(			"plot_clientLatencyLayer5", 	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.ms,	Aggregator.AVG, 	new Aggregator[]{ Aggregator.MIN, Aggregator.MAX, Aggregator.AVG, Aggregator.MEDIAN },		new PostProcessor[] {PostProcessor.NONE}),	
	AVG_CLIENT_LATENCY_LAYER5MESSAGE(		"plot_clientLatencyLayer5", 	PlotType.HISTOGRAM,	PlotScale.LINEAR,	Unit.ms,	Aggregator.AVG, 	new Aggregator[]{ Aggregator.MIN, Aggregator.MAX, Aggregator.AVG, Aggregator.MEDIAN },		new PostProcessor[] {PostProcessor.NONE}), // used in: example_outputStrategy_synchronous.txt	
	
	AVG_CLIENT_LATENCY_LAYER5MESSAGE_HIST(		"plot_clientLatencyLayer5hist", 	PlotType.HISTOGRAM,	PlotScale.NONE,	Unit.ms,	Aggregator.AVG, 	new Aggregator[]{ Aggregator.MIN, Aggregator.MAX, Aggregator.AVG, Aggregator.MEDIAN },		new PostProcessor[] {PostProcessor.NONE}), // used in: example_plotType_histogram.txt
	
	AVG_CLIENT_LATENCY_REQUESTMIXMESSAGE(	"plot_clientLatencyMixMessage", 	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.ms,	Aggregator.AVG,		new Aggregator[]{ Aggregator.MIN, Aggregator.MAX, Aggregator.AVG, Aggregator.MEDIAN },		new PostProcessor[] {PostProcessor.NONE}), // used in: example_plotType_lineChart.txt, example_outputStrategy_batch.txt, example_outputStrategy_pool.txt, example_outputStrategy_batchWithTimeout.txt ...
	MAX_CLIENT_LATENCY_REQUESTMIXMESSAGE(	"plot_clientLatencyMixMessage", 	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.ms,	Aggregator.MAX,		new Aggregator[]{ Aggregator.MIN, Aggregator.MAX, Aggregator.AVG, Aggregator.MEDIAN },		new PostProcessor[] {PostProcessor.NONE}), // used in: example_plotType_lineChart.txt, example_outputStrategy_batch.txt, example_outputStrategy_pool.txt, example_outputStrategy_batchWithTimeout.txt ...
	MIN_CLIENT_LATENCY_REQUESTMIXMESSAGE(	"plot_clientLatencyMixMessage", 	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.ms,	Aggregator.MIN,		new Aggregator[]{ Aggregator.MIN, Aggregator.MAX, Aggregator.AVG, Aggregator.MEDIAN },		new PostProcessor[] {PostProcessor.NONE}),
	AVG_CLIENT_LATENCY_REPLYMIXMESSAGE(		"plot_clientLatencyMixMessage", 	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.ms, 	Aggregator.AVG,		new Aggregator[]{ Aggregator.MIN, Aggregator.MAX, Aggregator.AVG, Aggregator.MEDIAN },		new PostProcessor[] {PostProcessor.NONE}),
	
	
	SUM_CLIENT_DATAVOLUME_SEND(					"clientPlot_datavolume", PlotType.LINE_CHART_ABS, PlotScale.LINEAR,	Unit.byte_, Aggregator.SUM,  new Aggregator[]{ Aggregator.SUM }, new PostProcessor[] {PostProcessor.NONE}),
	SUM_CLIENT_DATAVOLUME_RECEIVE(				"clientPlot_datavolume", PlotType.LINE_CHART_ABS, PlotScale.LINEAR,	Unit.byte_, Aggregator.SUM,  new Aggregator[]{ Aggregator.SUM }, new PostProcessor[] {PostProcessor.NONE}),
	SUM_CLIENT_DATAVOLUME_SENDANDRECEIVE(		"clientPlot_datavolume", PlotType.LINE_CHART_ABS, PlotScale.LINEAR,	Unit.byte_, Aggregator.SUM,  new Aggregator[]{ Aggregator.SUM }, new PostProcessor[] {PostProcessor.NONE}),
	
	SUM_CLIENT_PAYLOADVOLUME_SEND(				"clientPlot_datavolume", PlotType.LINE_CHART_ABS, PlotScale.LINEAR,	Unit.byte_, Aggregator.SUM,  new Aggregator[]{ Aggregator.SUM }, new PostProcessor[] {PostProcessor.NONE}),
	SUM_CLIENT_PAYLOADVOLUME_RECEIVE(			"clientPlot_datavolume", PlotType.LINE_CHART_ABS, PlotScale.LINEAR,	Unit.byte_, Aggregator.SUM,  new Aggregator[]{ Aggregator.SUM }, new PostProcessor[] {PostProcessor.NONE}),
	SUM_CLIENT_PAYLOADVOLUME_SENDANDRECEIVE(	"clientPlot_datavolume", PlotType.LINE_CHART_ABS, PlotScale.LINEAR,	Unit.byte_, Aggregator.SUM,  new Aggregator[]{ Aggregator.SUM }, new PostProcessor[] {PostProcessor.NONE}),
	
	
	SUM_CLIENT_DATAVOLUME_PER_SECOND_SEND(				"clientPlot_datavolumePerSecond", PlotType.LINE_CHART_ABS, PlotScale.LINEAR,	Unit.byte_, Aggregator.SUM,  new Aggregator[]{ Aggregator.SUM }, new PostProcessor[] {PostProcessor.PER_SECOND}),
	SUM_CLIENT_DATAVOLUME_PER_SECOND_RECEIVE(			"clientPlot_datavolumePerSecond", PlotType.LINE_CHART_ABS, PlotScale.LINEAR,	Unit.byte_, Aggregator.SUM,  new Aggregator[]{ Aggregator.SUM }, new PostProcessor[] {PostProcessor.PER_SECOND}),
	SUM_CLIENT_DATAVOLUME_PER_SECOND_SENDANDRECEIVE(	"clientPlot_datavolumePerSecond", PlotType.LINE_CHART_ABS, PlotScale.LINEAR,	Unit.byte_, Aggregator.SUM,  new Aggregator[]{ Aggregator.SUM }, new PostProcessor[] {PostProcessor.PER_SECOND}),
	
	
	SUM_CLIENT_PAYLOADVOLUME_PER_SECOND_AND_CLIENT_SEND(				"clientPlot_datavolume_per_second_and_client",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.byte_, Aggregator.SUM,  new Aggregator[]{ Aggregator.SUM }, 	new PostProcessor[] {PostProcessor.PER_SECOND, PostProcessor.PER_CLIENT}),
	SUM_CLIENT_PAYLOADVOLUME_PER_SECOND_AND_CLIENT_RECEIVE(				"clientPlot_datavolume_per_second_and_client",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.byte_, Aggregator.SUM,  new Aggregator[]{ Aggregator.SUM }, 	new PostProcessor[] {PostProcessor.PER_SECOND, PostProcessor.PER_CLIENT}),
	SUM_CLIENT_PAYLOADVOLUME_PER_SECOND_AND_CLIENT_SENDANDRECEIVE(		"clientPlot_datavolume_per_second_and_client",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.byte_, Aggregator.SUM,  new Aggregator[]{ Aggregator.SUM }, 	new PostProcessor[] {PostProcessor.PER_SECOND, PostProcessor.PER_CLIENT}), 		
	
	
	AVG_CLIENT_PAYLOADPERCENTAGE_SEND(				"clientPlot_utilization",		PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.percent,	Aggregator.AVG,	new Aggregator[]{ Aggregator.AVG },		new PostProcessor[] {PostProcessor.NONE}),
	AVG_CLIENT_PAYLOADPERCENTAGE_RECEIVE(			"clientPlot_utilization",		PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.percent,	Aggregator.AVG,	new Aggregator[]{ Aggregator.AVG },		new PostProcessor[] {PostProcessor.NONE}),
	AVG_CLIENT_PAYLOADPERCENTAGE_SENDANDRECEIVE(	"clientPlot_utilization",		PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.percent,	Aggregator.AVG,	new Aggregator[]{ Aggregator.AVG },		new PostProcessor[] {PostProcessor.NONE}),
	
	AVG_CLIENT_PADDINGPERCENTAGE_SEND(				"clientPlot_utilization",		PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.percent,	Aggregator.AVG,	new Aggregator[]{ Aggregator.AVG },		new PostProcessor[] {PostProcessor.NONE}),
	AVG_CLIENT_PADDINGPERCENTAGE_RECEIVE(			"clientPlot_utilization",		PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.percent,	Aggregator.AVG,	new Aggregator[]{ Aggregator.AVG },		new PostProcessor[] {PostProcessor.NONE}),
	AVG_CLIENT_PADDINGPERCENTAGE_SENDANDRECEIVE(	"clientPlot_utilization",		PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.percent,	Aggregator.AVG,	new Aggregator[]{ Aggregator.AVG },		new PostProcessor[] {PostProcessor.NONE}),
	
	
	SUM_CLIENT_MIXMESSAGES_SENT(				"clientPlot_messagesSent",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.event,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },		new PostProcessor[] {PostProcessor.NONE}),
	SUM_CLIENT_MIXMESSAGES_RECEIVED(			"clientPlot_messagesSent",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.event,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },		new PostProcessor[] {PostProcessor.NONE}),
	SUM_CLIENT_MIXMESSAGES_SENTANDRECEIVED(		"clientPlot_messagesSent",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.event,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },		new PostProcessor[] {PostProcessor.NONE}),
	SUM_CLIENT_LAYER5MESSAGES_SENT(				"clientPlot_messagesSent",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.event,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },		new PostProcessor[] {PostProcessor.NONE}),
	SUM_CLIENT_LAYER5MESSAGES_RECEIVED(			"clientPlot_messagesSent",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.event,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },		new PostProcessor[] {PostProcessor.NONE}),
	SUM_CLIENT_LAYER5MESSAGES_SENTANDRECEIVED(	"clientPlot_messagesSent",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.event,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },		new PostProcessor[] {PostProcessor.NONE}),
	
	
	ADU_SIZE_SEND(				"display_aduSize",	PlotType.TXT_ONLY,	PlotScale.NONE,	Unit.byte_,		Aggregator.NONE,	new Aggregator[]{ Aggregator.NONE },	new PostProcessor[] {PostProcessor.SORT}),
	ADU_SIZE_RECEIVE(			"display_aduSize",	PlotType.TXT_ONLY,	PlotScale.NONE,	Unit.byte_,		Aggregator.NONE,	new Aggregator[]{ Aggregator.NONE },	new PostProcessor[] {PostProcessor.SORT}),
	ADU_SIZE_SENDANDRECEIVE(	"display_aduSize",	PlotType.TXT_ONLY,	PlotScale.NONE,	Unit.byte_,		Aggregator.NONE,	new Aggregator[]{ Aggregator.NONE },	new PostProcessor[] {PostProcessor.SORT}),
	
	
	CF_ADU_SIZE_SEND(				"plot_cfAduSize",		PlotType.LINE_CHART_CF,	PlotScale.LINEAR,	Unit.byte_,		Aggregator.NONE,	new Aggregator[]{ Aggregator.NONE },	new PostProcessor[] {PostProcessor.NONE}),
	CF_ADU_SIZE_RECEIVE(			"plot_cfAduSize",		PlotType.LINE_CHART_CF,	PlotScale.LINEAR,	Unit.byte_,		Aggregator.NONE,	new Aggregator[]{ Aggregator.NONE },	new PostProcessor[] {PostProcessor.NONE}),
	CF_ADU_SIZE_SENDANDRECEIVE(		"plot_cfAduSize",		PlotType.LINE_CHART_CF,	PlotScale.LINEAR,	Unit.byte_,		Aggregator.NONE,	new Aggregator[]{ Aggregator.NONE },	new PostProcessor[] {PostProcessor.NONE}), // used in: lgpaper_aduSizeAU10.txt, lgpaper_aduSizeAU8.txt
	
	CF_AVG_THROUGHPUT_PER_CLIENT_SEND(				"plot_cfThroughputPerClient",		PlotType.LINE_CHART_CF,	PlotScale.LINEAR,	Unit.kbyte,		Aggregator.SUM,	new Aggregator[]{ Aggregator.NONE },	new PostProcessor[] {PostProcessor.PER_SECOND}), 
	CF_AVG_THROUGHPUT_PER_CLIENT_RECEIVE(			"plot_cfThroughputPerClient",		PlotType.LINE_CHART_CF,	PlotScale.LINEAR,	Unit.kbyte,		Aggregator.SUM,	new Aggregator[]{ Aggregator.NONE },	new PostProcessor[] {PostProcessor.PER_SECOND}),
	CF_AVG_THROUGHPUT_PER_CLIENT_SENDANDRECEIVE(	"plot_cfThroughputPerClient",		PlotType.LINE_CHART_CF,	PlotScale.LINEAR,	Unit.kbyte,		Aggregator.SUM,	new Aggregator[]{ Aggregator.NONE },	new PostProcessor[] {PostProcessor.PER_SECOND}), // used in: lgpaper_mixMessageSendingRateClients.txt, lgpaper_throughputZero.txt
	
	CF_AVG_LATENCY_PER_CLIENT_SEND(				"plot_cfLatencyPerClient",		PlotType.LINE_CHART_CF,	PlotScale.LINEAR,	Unit.ms,		Aggregator.AVG,	new Aggregator[]{ Aggregator.NONE },	new PostProcessor[] {PostProcessor.NONE}), // used in: example_plotType_cumulativeFraction.txt
	
	SUM_DATAVOLUME_PER_MIX_SEND(			"mixPlot_datavolume",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.byte_,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },	new PostProcessor[] {PostProcessor.PER_MIX}),
	SUM_DATAVOLUME_PER_MIX_RECEIVE(			"mixPlot_datavolume",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.byte_,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },	new PostProcessor[] {PostProcessor.PER_MIX}),
	SUM_DATAVOLUME_PER_MIX_SENDANDRECEIVE(	"mixPlot_datavolume",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.byte_,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },	new PostProcessor[] {PostProcessor.PER_MIX}),
	
	
	DLPA_REQUEST_SENDING_RATE_PER_MIX(				"dlpaPlot_events_per_second",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.event,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },	new PostProcessor[] {PostProcessor.PER_SECOND, PostProcessor.PER_MIX}),
	DLPA_REPLY_SENDING_RATE_PER_MIX(				"dlpaPlot_events_per_second",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.event,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },	new PostProcessor[] {PostProcessor.PER_SECOND, PostProcessor.PER_MIX}),
	DLPA_REQUEST_AND_REPLY_SENDING_RATE_PER_MIX(	"dlpaPlot_events_per_second",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.event,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },	new PostProcessor[] {PostProcessor.PER_SECOND, PostProcessor.PER_MIX}),
	
	DLPA_REQUEST_SENDING_RATE_PER_MIX_AND_CLIENT(				"dlpaPlot_events_per_second",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.event,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },	new PostProcessor[] {PostProcessor.PER_SECOND, PostProcessor.PER_MIX, PostProcessor.PER_CLIENT}), // used in: exampleExperiment.txt, example_outputStrategy_DLPA.txt
	DLPA_REPLY_SENDING_RATE_PER_MIX_AND_CLIENT(					"dlpaPlot_events_per_second",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.event,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },	new PostProcessor[] {PostProcessor.PER_SECOND, PostProcessor.PER_MIX, PostProcessor.PER_CLIENT}),
	DLPA_REQUEST_AND_REPLY_SENDING_RATE_PER_MIX_AND_CLIENT(		"dlpaPlot_events_per_second",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.event,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },	new PostProcessor[] {PostProcessor.PER_SECOND, PostProcessor.PER_MIX, PostProcessor.PER_CLIENT}),
	
	
	DLPA_REQUEST_DUMMY_PERCENTAGE_PER_MIX(	"dlpaPlot_dummyPercentage",		PlotType.LINE_CHART_ABS,	PlotScale.LOG_X,	Unit.percent,		Aggregator.AVG,		new Aggregator[]{ Aggregator.AVG },		new PostProcessor[] {PostProcessor.NONE}),	// used in: lgpaper_dlpaDummyPercentagePoi.txt, lgpaper_dlpaDummyPercentageDPE.txt, lgpaper_dlpaDummyPercentageDLPAE.txt
	
	
	DLPA_REQUEST_MESSAGE_DROP_PERCENTAGE(				"dlpaPlot_dropPercentage",		PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.percent,		Aggregator.NONE,		new Aggregator[]{ Aggregator.BERNOULLI },		new PostProcessor[] {PostProcessor.NONE}),
	DLPA_REPLY_MESSAGE_DROP_PERCENTAGE(					"dlpaPlot_dropPercentage",		PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.percent,		Aggregator.NONE,		new Aggregator[]{ Aggregator.BERNOULLI },		new PostProcessor[] {PostProcessor.NONE}),
	DLPA_MESSAGE_DROP_PERCENTAGE(						"dlpaPlot_dropPercentage",		PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.percent,		Aggregator.NONE,		new Aggregator[]{ Aggregator.BERNOULLI },		new PostProcessor[] {PostProcessor.NONE}),
	
	DLPA_REQUEST_MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES(	"dlpaPlot_dropPercentage",		PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.percent,		Aggregator.NONE,		new Aggregator[]{ Aggregator.BERNOULLI },		new PostProcessor[] {PostProcessor.NONE}),
	DLPA_REPLY_MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES(	"dlpaPlot_dropPercentage",		PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.percent,		Aggregator.NONE,		new Aggregator[]{ Aggregator.BERNOULLI },		new PostProcessor[] {PostProcessor.NONE}),
	DLPA_MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES(			"dlpaPlot_dropPercentage",		PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.percent,		Aggregator.NONE,		new Aggregator[]{ Aggregator.BERNOULLI },		new PostProcessor[] {PostProcessor.NONE}),
	
	
	SUM_DISTANTPROXY_DATAVOLUME_SENDANDRECEIVE(	"dpPlot_datavolume",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.byte_,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },	new PostProcessor[] {PostProcessor.NONE}),
	
	
	SUM_DISTANTPROXY_MIXMESSAGES_RECEIVED(	"clientPlot_messagesSent",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.event,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },	new PostProcessor[] {PostProcessor.NONE}),
	
	AVG_TRAFFICSOURCE_SENDING_RATE_PER_CLIENT(	"dlpaPlot_events_per_second",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.event,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },	new PostProcessor[] {PostProcessor.PER_SECOND, PostProcessor.PER_CLIENT}),
	AVG_MIXMESSAGE_SENDING_RATE_PER_CLIENT(		"dlpaPlot_events_per_second",	PlotType.LINE_CHART_ABS,	PlotScale.LINEAR,	Unit.event,	Aggregator.SUM,	new Aggregator[]{ Aggregator.SUM },	new PostProcessor[] {PostProcessor.PER_SECOND, PostProcessor.PER_CLIENT}) // used in: exampleExperiment.txt, example_outputStrategy_DLPA.txt
	;

	
	public String destinationPlot;
	public PlotType plotType;
	public PlotScale plotScale;
	public Unit unit;
	public String unitAsString;
	public Aggregator sourceValueAggregator;
	public Aggregator[] sourceAggregators;
	public PostProcessor[] postProcessors;
	public boolean isActivated = false;
	
	public enum Unit { sec, ms, byte_, kbyte, mbyte, gbyte, percent, event };
	
	
	private StatisticsType(	String destinationPlot, 
							PlotType plotType,
							PlotScale plotScale,
							Unit unit, 
							Aggregator sourceValueAggregator, 
							Aggregator[] sourceAggregators, 
							PostProcessor[] postProcessors
							) {
		this.destinationPlot = destinationPlot;
		this.plotType = plotType;
		this.plotScale = plotScale;
		this.unit = unit;
		this.unitAsString = unit.name();
		for (PostProcessor pp:postProcessors)
			if (pp != PostProcessor.NONE && !pp.getUnit().equals(""))
				unitAsString += "_per_" + pp.getUnit();
		this.sourceValueAggregator = sourceValueAggregator;
		if (sourceValueAggregator == Aggregator.NONE) { // all sourceAggregators must support the same data input format if no sourceValueAggregator is selected
			InputDataType idt = sourceAggregators[0].getInputDataType();
			for (Aggregator agg: sourceAggregators)
				if (agg.getInputDataType() != idt)
					throw new RuntimeException("ERROR: the selected Aggregator " +agg +" cannot be combined with the Aggregator " +sourceAggregators[0] +". All sourceAggregators must support the same InputDataType if no SourceValueAggregator is selected!"); 
		}
		this.sourceAggregators = sourceAggregators;
		this.postProcessors = postProcessors;
	}
	
	
	public static void reset() {
		
		for (StatisticsType st:StatisticsType.values())
			st.isActivated = false;
		
	}
	
}
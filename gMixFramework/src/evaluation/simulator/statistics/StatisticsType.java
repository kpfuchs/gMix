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
package evaluation.simulator.statistics;

import java.util.ArrayList;


public enum StatisticsType {
	
	CLIENT_RTT_NONEMIXMESSAGE("latency", new EvaluationType[]{ EvaluationType.MIN, EvaluationType.MAX, EvaluationType.AVG, EvaluationType.MEDIAN }),
	CLIENT_LATENCY_REQUESTMIXMESSAGE("latency", new EvaluationType[]{ EvaluationType.MIN, EvaluationType.MAX, EvaluationType.AVG, EvaluationType.MEDIAN }),
	CLIENT_LATENCY_REPLYMIXMESSAGE("latency", new EvaluationType[]{ EvaluationType.MIN, EvaluationType.MAX, EvaluationType.AVG, EvaluationType.MEDIAN }),
	CLIENT_DATAVOLUME_SEND("datavolume", new EvaluationType[]{ EvaluationType.SUM }),
	CLIENT_DATAVOLUME_RECEIVE("datavolume", new EvaluationType[]{ EvaluationType.SUM }),
	CLIENT_DATAVOLUME_SENDANDRECEIVE("datavolume", new EvaluationType[]{ EvaluationType.SUM }),
	CLIENT_DATAVOLUME_PER_SECOND_SEND("datavolume_per_second", new EvaluationType[]{ EvaluationType.VOLUME_PER_SECOND }), // per client!
	CLIENT_DATAVOLUME_PER_SECOND_RECEIVE("datavolume_per_second", new EvaluationType[]{ EvaluationType.VOLUME_PER_SECOND }), // per client!
	CLIENT_DATAVOLUME_PER_SECOND_SENDANDRECEIVE("datavolume_per_second", new EvaluationType[]{ EvaluationType.VOLUME_PER_SECOND }), // per client!
	CLIENT_PAYLOADVOLUME_SEND("datavolume", new EvaluationType[]{ EvaluationType.SUM }),
	CLIENT_PAYLOADVOLUME_RECEIVE("datavolume", new EvaluationType[]{ EvaluationType.SUM }),
	CLIENT_PAYLOADVOLUME_SENDANDRECEIVE("datavolume", new EvaluationType[]{ EvaluationType.SUM }),
	CLIENT_PAYLOADVOLUME_PER_SECOND_SEND("datavolume_per_second", new EvaluationType[]{ EvaluationType.VOLUME_PER_SECOND }), // per client!
	CLIENT_PAYLOADVOLUME_PER_SECOND_RECEIVE("datavolume_per_second", new EvaluationType[]{ EvaluationType.VOLUME_PER_SECOND }), // per client!
	CLIENT_PAYLOADVOLUME_PER_SECOND_SENDANDRECEIVE("datavolume_per_second", new EvaluationType[]{ EvaluationType.VOLUME_PER_SECOND }), // per client!
	CLIENT_PAYLOADPERCENTAGE_SEND("utilization", new EvaluationType[]{ EvaluationType.AVG }),
	CLIENT_PAYLOADPERCENTAGE_RECEIVE("utilization", new EvaluationType[]{ EvaluationType.AVG }),
	CLIENT_PAYLOADPERCENTAGE_SENDANDRECEIVE("utilization", new EvaluationType[]{ EvaluationType.AVG }),
	CLIENT_PADDINGPERCENTAGE_SEND("utilization", new EvaluationType[]{ EvaluationType.AVG }),
	CLIENT_PADDINGPERCENTAGE_RECEIVE("utilization", new EvaluationType[]{ EvaluationType.AVG }),
	CLIENT_PADDINGPERCENTAGE_SENDANDRECEIVE("utilization", new EvaluationType[]{ EvaluationType.AVG }),
	//CLIENT_PADDINGOVERHEAD_SEND("overhead", new EvaluationType[]{ EvaluationType.AVG }),
	//CLIENT_PADDINGOVERHEAD_RECEIVE("overhead", new EvaluationType[]{ EvaluationType.AVG }),
	//CLIENT_PADDINGOVERHEAD_SENDANDRECEIVE("overhead", new EvaluationType[]{ EvaluationType.AVG }),
	CLIENT_MIXMESSAGES_SENT("MessagesSent", new EvaluationType[]{ EvaluationType.SUM }),
	CLIENT_MIXMESSAGES_RECEIVED("MessagesSent", new EvaluationType[]{ EvaluationType.SUM }),
	CLIENT_MIXMESSAGES_SENTANDRECEIVED("MessagesSent", new EvaluationType[]{ EvaluationType.SUM }),
	CLIENT_NONEMIXMESSAGES_SENT("MessagesSent", new EvaluationType[]{ EvaluationType.SUM }),
	CLIENT_NONEMIXMESSAGES_RECEIVED("MessagesSent", new EvaluationType[]{ EvaluationType.SUM }),
	CLIENT_NONEMIXMESSAGES_SENTANDRECEIVED("MessagesSent", new EvaluationType[]{ EvaluationType.SUM }),
	
	ADU_SIZE_SEND("aduSize", new EvaluationType[]{ EvaluationType.VALUE_LIST }),
	ADU_SIZE_RECEIVE("aduSize", new EvaluationType[]{ EvaluationType.VALUE_LIST }),
	ADU_SIZE_SENDANDRECEIVE("aduSize", new EvaluationType[]{ EvaluationType.VALUE_LIST }),
	
	MIX_DATAVOLUME_SEND("datavolume", new EvaluationType[]{ EvaluationType.SUM }),
	MIX_DATAVOLUME_RECEIVE("datavolume", new EvaluationType[]{ EvaluationType.SUM }),
	MIX_DATAVOLUME_SENDANDRECEIVE("datavolume", new EvaluationType[]{ EvaluationType.SUM }),
	MIX_MIXMESSAGES_SENT("MessagesSent", new EvaluationType[]{ EvaluationType.SUM }),
	
	DLPA_REQUEST_SENDING_RATE("events_per_second", new EvaluationType[]{ EvaluationType.EVENTS_PER_SECOND }),
	DLPA_REPLY_SENDING_RATE("events_per_second", new EvaluationType[]{ EvaluationType.EVENTS_PER_SECOND }),
	DLPA_REQUEST_AND_REPLY_SENDING_RATE("events_per_second", new EvaluationType[]{ EvaluationType.EVENTS_PER_SECOND }),
	
	DLPA_REQUEST_SENDING_RATE_PER_CLIENT("events_per_second", new EvaluationType[]{ EvaluationType.EVENTS_PER_SECOND_AND_CLIENT }),
	
	DLPA_REQUEST_DUMMY_PERCENTAGE("utilization", new EvaluationType[]{ EvaluationType.AVG }),

	REQUEST_MESSAGE_DROP_PERCENTAGE("packetDrop", new EvaluationType[]{ EvaluationType.PERCENTAGE }),
	REPLY_MESSAGE_DROP_PERCENTAGE("packetDrop", new EvaluationType[]{ EvaluationType.PERCENTAGE }),
	MESSAGE_DROP_PERCENTAGE("packetDrop", new EvaluationType[]{ EvaluationType.PERCENTAGE }),
	
	REQUEST_MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES("packetDrop", new EvaluationType[]{ EvaluationType.PERCENTAGE }),
	REPLY_MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES("packetDrop", new EvaluationType[]{ EvaluationType.PERCENTAGE }),
	MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES("packetDrop", new EvaluationType[]{ EvaluationType.PERCENTAGE }),
	
	DISTANTPROXY_DATAVOLUME_SENDANDRECEIVE("datavolume", new EvaluationType[]{ EvaluationType.SUM }),
	DISTANTPROXY_MIXMESSAGES_RECEIVED("MessagesSent", new EvaluationType[]{ EvaluationType.SUM }),
	
	TRAFFICSOURCE_SENDING_RATE_PER_CLIENT("events_per_second", new EvaluationType[]{ EvaluationType.EVENTS_PER_SECOND_AND_CLIENT }),
	MIXMESSAGE_SENDING_RATE_PER_CLIENT("events_per_second", new EvaluationType[]{ EvaluationType.EVENTS_PER_SECOND_AND_CLIENT })
	
	;
	
/*	public static final int DATA_TYPE_DOUBLE = 1;
	public static final int DATA_TYPE_COUNTER = 2;
	public static final int DATA_TYPE_BIG_DECIMAL = 3;*/
	
	public String measurand;
	public String unit;
	public EvaluationType[] intendedEvaluations;
	public boolean isActivated = false;
	
	
	// allte StatisticsTypes mit dem selben "measurand" werden in das selbe Diagramm geplottet
	// -> für jeden measurand-typ ein Diagramm
	private StatisticsType(String measurand, EvaluationType[] intendedEvaluations) {
		
		this.measurand = measurand;
		
		if (measurand.equals("latency"))
			unit = "ms";
		else if (measurand.equals("datavolume"))
			unit = "byte";
		else if (measurand.equals("utilization"))
			unit = "%";
		else if (measurand.equals("MessagesSent"))
			unit = "NumberOfMessages";
		else if (measurand.equals("overhead"))
			unit = "percent";
		else if (measurand.equals("events_per_second"))
			unit = "events/sec";
		else if (measurand.equals("datavolume_per_second"))
			unit = "datavolume_per_second";
		else if (measurand.equals("packetDrop"))
			unit = "%";
		else if (measurand.equals("aduSize"))
			unit = "byte";
		else
			throw new RuntimeException("ERROR: unknown measurand " +measurand);
		
		this.intendedEvaluations = intendedEvaluations;
		
		
	}
	
	
	public static String getUnit(String measurand) {
		
		if (measurand.equals("latency"))
			return "ms";
		else if (measurand.equals("datavolume"))
			return "byte";
		else if (measurand.equals("utilization"))
			return "%";
		else if (measurand.equals("MessagesSent"))
			return "NumberOfMessages";
		else if (measurand.equals("overhead"))
			return "percent";
		else if (measurand.equals("events_per_second"))
			return "events/sec";
		else if (measurand.equals("datavolume_per_second"))
			return "kbyte/sec";
		else if (measurand.equals("packetDrop"))
			return "% (1=100%)";
		else if (measurand.equals("aduSize"))
			return "byte";
		else  
			throw new RuntimeException("ERROR: unknown measurand " +measurand);
		
	}
	
	
	public static String[] getDistinctMeasurands() {
		
		return getDistinctMeasurandsFromList(StatisticsType.values());
		
	}
	
	
	public static String[] getDistinctMeasurandsFromList(StatisticsType[] list) {
		
		ArrayList<String> measurands = new ArrayList<String>();
		
		for (StatisticsType st:list)
			if (!measurands.contains(st.measurand))
				measurands.add(st.measurand);
		
		return measurands.toArray(new String[0]);
		
	}
	
	
	public static void reset() {
		
		for (StatisticsType st:StatisticsType.values())
			st.isActivated = false;
		
	}
	
}
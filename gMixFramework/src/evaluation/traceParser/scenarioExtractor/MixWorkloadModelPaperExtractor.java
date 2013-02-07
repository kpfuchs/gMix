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
package evaluation.traceParser.scenarioExtractor;

public class MixWorkloadModelPaperExtractor {

	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		// TODO: create 1000*1000 set and replay subsets with replay engine (prevent multiple runs to obtain results...)
		new FixedLengthExtractorDPE("./inputOutput/global/traces/auck8/", 1, 1000*60*1000, "auck8_1000min_1user_dpe");
		new FixedLengthExtractorDPE("./inputOutput/global/traces/auck8/", 2, 500*60*1000, "auck8_500min_2user_dpe");
		new FixedLengthExtractorDPE("./inputOutput/global/traces/auck8/", 5, 300*60*1000, "auck8_300min_5user_dpe");
		new FixedLengthExtractorDPE("./inputOutput/global/traces/auck8/", 10, 120*60*1000, "auck8_120min_10user_dpe");
		new FixedLengthExtractorDPE("./inputOutput/global/traces/auck8/", 20, 60*60*1000, "auck8_60min_20user_dpe");
		new FixedLengthExtractorDPE("./inputOutput/global/traces/auck8/", 50, 50*60*1000, "auck8_50min_50user_dpe");
		new FixedLengthExtractorDPE("./inputOutput/global/traces/auck8/", 100, 45*60*1000, "auck8_45min_100user_dpe");
		new FixedLengthExtractorDPE("./inputOutput/global/traces/auck8/", 200, 20*60*1000, "auck8_20min_200user_dpe");
		new FixedLengthExtractorDPE("./inputOutput/global/traces/auck8/", 500, 5*60*1000, "auck8_5min_500user_dpe");
		new FixedLengthExtractorDPE("./inputOutput/global/traces/auck8/", 1000, 5*60*1000, "auck8_5min_1000user_dpe");
		new FixedLengthExtractorDPE("./inputOutput/global/traces/auck10/", 1000, 5*60*1000, "auck10_5min_1000user_dpe");
		
		new FixedLengthExtractorDLPA("./inputOutput/global/traces/auck8/", 1, 1000*60*1000, "auck8_1000min_1user_dlpa");
		new FixedLengthExtractorDLPA("./inputOutput/global/traces/auck8/", 2, 500*60*1000, "auck8_500min_2user_dlpa");
		new FixedLengthExtractorDLPA("./inputOutput/global/traces/auck8/", 5, 300*60*1000, "auck8_300min_5user_dlpa");
		new FixedLengthExtractorDLPA("./inputOutput/global/traces/auck8/", 10, 120*60*1000, "auck8_120min_10user_dlpa");
		new FixedLengthExtractorDLPA("./inputOutput/global/traces/auck8/", 20, 60*60*1000, "auck8_60min_20user_dlpa");
		new FixedLengthExtractorDLPA("./inputOutput/global/traces/auck8/", 50, 50*60*1000, "auck8_50min_50user_dlpa");
		new FixedLengthExtractorDLPA("./inputOutput/global/traces/auck8/", 100, 45*60*1000, "auck8_45min_100user_dlpa");
		new FixedLengthExtractorDLPA("./inputOutput/global/traces/auck8/", 200, 20*60*1000, "auck8_20min_200user_dlpa");
		new FixedLengthExtractorDLPA("./inputOutput/global/traces/auck8/", 500, 5*60*1000, "auck8_5min_500user_dlpa");
		new FixedLengthExtractorDLPA("./inputOutput/global/traces/auck8/", 1000, 5*60*1000, "auck8_5min_1000user_dlpa");
		new FixedLengthExtractorDLPA("./inputOutput/global/traces/auck10/", 1000, 5*60*1000, "auck10_5min_1000user_dlpa");

		System.out.println("FINISHED CREATING ALL DATA SETS (SUCCESS)"); 
	} 

}

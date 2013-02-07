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
package evaluation.traceParser.statistics;

import evaluation.traceParser.engine.Protocol;
import evaluation.traceParser.engine.dataStructure.Flow;
import evaluation.traceParser.engine.dataStructure.Transaction;
import evaluation.traceParser.engine.dataStructure.Flow.FlowDirection;
import evaluation.traceParser.engine.filter.TerminatingFlowFilter;
import evaluation.traceParser.engine.filter.TerminatingFlowFilterTester;
import framework.core.util.Util;


public class GeneralFlowStatistics implements TerminatingFlowFilter {

	private Flow lastFlow;
	private long flowCounter = 0;
	private long[] protocolDistribution = new long[Protocol.values().length];
	private long requestBytes = 0;
	private long replyBytes = 0;
	private long totalTransactions = 0;
	private long fromWan = 0;
	private long toWan = 0;
	private long unknownWan = 0;
	
	
	@Override
	public Flow newRecord(Flow flow) {
		flowCounter++;
		if (flowCounter % 5000 == 0)
			System.out.println("read " +flowCounter +" flows so far"); 
		protocolDistribution[flow.layer4protocol.ordinal()]++;
		if (flow.flowDirection == FlowDirection.TO_WAN)
			toWan++;
		else if (flow.flowDirection == FlowDirection.FROM_WAN)
			fromWan++;
		else
			unknownWan++;
		for (Transaction message:flow.transactions) {
			totalTransactions++;
			requestBytes += message.getRequestSize();
			replyBytes += message.getTotalReplySize();
		} 
		lastFlow = flow;
		return flow;
	}
	
	
	@Override
	public void finished() {
		double totalSeconds = lastFlow.startOfFlow * 1000d; // ignores additional delay of the transactions of this last flow...
		System.out.println("finished reading trace file\n"); 
		System.out.println("\nSTATISTICS: "); 
		System.out.println("file covers " +totalSeconds +" seconds "); 
		System.out.println("total flows: " +flowCounter);
		System.out.println("total bytes transferred: " +Util.humanReadableByteCount(replyBytes+requestBytes, false));
		System.out.println("request bytes transferred: " +Util.humanReadableByteCount(requestBytes, false));
		System.out.println("reply bytes transferred: " +Util.humanReadableByteCount(replyBytes, false));
		System.out.println("avg bytes/sec (total): " +Util.humanReadableByteCount((replyBytes+requestBytes)/(long)totalSeconds, false));
		System.out.println("avg bytes/sec (req): " +Util.humanReadableByteCount(requestBytes/(long)totalSeconds, false));
		System.out.println("avg bytes/sec (rep): " +Util.humanReadableByteCount(replyBytes/(long)totalSeconds, false));
		System.out.println("total transactions: " +totalTransactions);
		System.out.println("transactions/sec: " +((double)(totalTransactions))/totalSeconds);
		System.out.println("avg transactions/flow: " +((double)(totalTransactions))/(double)flowCounter);
		System.out.println("\nprotocol distribution:");
		System.out.println("wan status: " +toWan +"x to wan, " +fromWan +"x from wan, " +unknownWan +"x unknown"); 
		for (int i=0; i<protocolDistribution.length; i++)
			if (protocolDistribution[i] != 0)
				System.out.println(protocolDistribution[i] +"x\t" +Protocol.getProtocol(i)); 
	}
	

	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		new TerminatingFlowFilterTester(new GeneralFlowStatistics(), "./inputOutput/global/traces/erfTests/auckland10sample/1000HostSampleAuck10MinusTopFlop5.gmf", null);
	}
}

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
package evaluation.traceParser.engine.converter;

import java.util.Vector;

import evaluation.traceParser.engine.dataStructure.Flow;
import framework.core.util.Util;


public class EndOfFlow {
	
	public int flowId;
	public long endOfFlow;
	public long[][] endsOfReplies;
	public int pointerOne;
	public int pointerTwo;
	
	
	public EndOfFlow(Flow flow) {
		this.flowId = flow.flowId;
		this.endOfFlow = flow.endOfFlow;
		this.endsOfReplies = new long[flow.transactions.size()][];
		for (int i=0; i<flow.transactions.size(); i++)
			if (flow.transactions.get(i).containsReplies())
				endsOfReplies[i] = flow.transactions.get(i).getEndReplyOffsets();
	}
	
	
	// returns null if no suiting transaction found
	public static TransactionInfo getTransactionWithLatestReplyBefore(long upperBound, Vector<EndOfFlow> openFlows) {
		long lastReplyTime = Long.MIN_VALUE;
		int flowId = Util.NOT_SET;
		int transactionId = Util.NOT_SET;
		int replyId = Util.NOT_SET;
		for (EndOfFlow currentFlow: openFlows) { // TODO use pointers to avoid reading from 0 every time... (-> upperBound will always increment from call to call)
			for (int i=0; i<currentFlow.endsOfReplies.length; i++) { // for each transaction (of the current flow)
				if (currentFlow.endsOfReplies[i] == null)
					continue;
				for (int j=0; j<currentFlow.endsOfReplies[i].length; j++) { // for each (end of) reply (of the current transaction)
					if (currentFlow.endsOfReplies[i][j] <= upperBound && currentFlow.endsOfReplies[i][j] > lastReplyTime) {
						lastReplyTime = currentFlow.endsOfReplies[i][j];
						flowId = currentFlow.flowId;
						transactionId = i;
						replyId = j;
					}
				} 
			} 
		} 
		return (flowId == Util.NOT_SET) ? null : new TransactionInfo(flowId, transactionId, replyId, lastReplyTime);
	}

}
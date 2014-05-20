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
package evaluation.simulator.plugins.trafficSource;

import evaluation.traceParser.engine.dataStructure.ExtendedTransaction;
import evaluation.traceParser.engine.dataStructure.Flow;
import framework.core.util.Util;


public class ActiveFlow {
	
	private Flow flow;
	private int transactionCounter;
	private int transactions;
	private ExtendedTransaction currentTransaction;
	private int expectedReplies;
	private int replyCounter;
	private int idOfLatestReply;
	
	
	public ActiveFlow(Flow flow) {
		this.flow = flow;
		this.transactionCounter = 0;
		this.transactions = flow.transactions.size();
		this.idOfLatestReply = Util.NOT_SET;
	}
	
	
	public boolean hasNextTransaction() {
		return transactionCounter < transactions;
	}
	
	
	public ExtendedTransaction getNextTransaction() {
		currentTransaction = flow.transactions.get(transactionCounter);
		transactionCounter++;
		expectedReplies = currentTransaction.getTotalReplySize() == 0 ? 0 : currentTransaction.getDistinctReplySizes().length;
		replyCounter = 0;
		idOfLatestReply = Util.NOT_SET;
		return currentTransaction;
	}
	
	public ExtendedTransaction getCurrentTransaction() {
		return currentTransaction;
	}
	
	
	public void replyReceived() {
		idOfLatestReply = replyCounter;
		replyCounter++;
		//System.out.println("(" +replyCounter +" of " +expectedReplies +")"); 
		assert replyCounter <= expectedReplies;
	}
	
	
	public int getArrayOffsetOfCurrentTransaction() {
		return transactionCounter - 1;
	}
	
	
	public int getIdOfLatestFinishedReply() {
		return idOfLatestReply;
	}
	
	
	public boolean allRepliesForCurrentTransactionReceived() {
		return replyCounter == expectedReplies;
	}
	
	
	public Flow getFlow() {
		return this.flow;
	}
	
}

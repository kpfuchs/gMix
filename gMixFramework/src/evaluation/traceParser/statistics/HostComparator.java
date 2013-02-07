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

import java.util.Comparator;

import evaluation.traceParser.engine.dataStructure.Host;

public class HostComparator {

	public static class HostIdComparator implements Comparator<Host> {

		@Override
		public int compare(Host h1, Host h2) {
	        return (h1.hostId == h2.hostId ?  0 :	// Values are equal
	            (h1.hostId < h2.hostId ? -1 :		// (-0.0, 0.0) or (!NaN, NaN)
	             1));
		}
		
	}
	
	
	public static class NumberOfFlowsComparator implements Comparator<Host> {

		@Override
		public int compare(Host h1, Host h2) {
	        return (h1.stat_numberOfFlows == h2.stat_numberOfFlows ?  0 :	// Values are equal
	            (h1.stat_numberOfFlows < h2.stat_numberOfFlows ? -1 :		// (-0.0, 0.0) or (!NaN, NaN)
	             1));
		}
		
	}
	
	
	public static class NumberOfFlowGroupsComparator implements Comparator<Host> {

		@Override
		public int compare(Host h1, Host h2) {
	        return (h1.stat_numberOfFlowGroups == h2.stat_numberOfFlowGroups ?  0 :	// Values are equal
	            (h1.stat_numberOfFlowGroups < h2.stat_numberOfFlowGroups ? -1 :		// (-0.0, 0.0) or (!NaN, NaN)
	             1));
		}
		
	}
	
	
	public static class OnlineTimeComparator implements Comparator<Host> {

		@Override
		public int compare(Host h1, Host h2) {
	        return (h1.stat_onlineTime == h2.stat_onlineTime ?  0 :	// Values are equal
	            (h1.stat_onlineTime < h2.stat_onlineTime ? -1 :		// (-0.0, 0.0) or (!NaN, NaN)
	             1));
		}
		
	}
	
	
	public static class AvgBytesPerSecComparator implements Comparator<Host> {

		@Override
		public int compare(Host h1, Host h2) {
	        return ((h1.stat_avgRequestBytesPerSec + h1.stat_avgReplyBytesPerSec) == (h2.stat_avgRequestBytesPerSec + h2.stat_avgReplyBytesPerSec) ?  0 :	// Values are equal
	            ((h1.stat_avgRequestBytesPerSec + h1.stat_avgReplyBytesPerSec) < (h2.stat_avgRequestBytesPerSec + h2.stat_avgReplyBytesPerSec) ? -1 :		// (-0.0, 0.0) or (!NaN, NaN)
	             1));
		}
		
	}
	
	
	public static class AvgRequestBytesPerSecComparator implements Comparator<Host> {

		@Override
		public int compare(Host h1, Host h2) {
	        return (h1.stat_avgRequestBytesPerSec == h2.stat_avgRequestBytesPerSec ?  0 :	// Values are equal
	            (h1.stat_avgRequestBytesPerSec < h2.stat_avgRequestBytesPerSec ? -1 :		// (-0.0, 0.0) or (!NaN, NaN)
	             1));
		}
		
	}
	
	
	public static class AvgReplyBytesPerSecComparator implements Comparator<Host> {

		@Override
		public int compare(Host h1, Host h2) {
	        return (h1.stat_avgReplyBytesPerSec == h2.stat_avgReplyBytesPerSec ?  0 :	// Values are equal
	            (h1.stat_avgReplyBytesPerSec < h2.stat_avgReplyBytesPerSec ? -1 :		// (-0.0, 0.0) or (!NaN, NaN)
	             1));
		}
		
	}
	
	
	public static class TotalBytesTransferredComparator implements Comparator<Host> {

		@Override
		public int compare(Host h1, Host h2) {
	        return ((h1.stat_requestBytesTransferred + h1.stat_replyBytesTransferred) == (h2.stat_requestBytesTransferred + h2.stat_replyBytesTransferred) ?  0 :	// Values are equal
	            ((h1.stat_requestBytesTransferred + h1.stat_replyBytesTransferred) < (h2.stat_requestBytesTransferred + h2.stat_replyBytesTransferred) ? -1 :		// (-0.0, 0.0) or (!NaN, NaN)
	             1));
		}
		
	}
	
	
	public static class RequestBytesTransferredComparator implements Comparator<Host> {

		@Override
		public int compare(Host h1, Host h2) {
	        return (h1.stat_requestBytesTransferred == h2.stat_requestBytesTransferred ?  0 :	// Values are equal
	            (h1.stat_requestBytesTransferred < h2.stat_requestBytesTransferred ? -1 :		// (-0.0, 0.0) or (!NaN, NaN)
	             1));
		}
		
	}
	
	
	public static class ReplyBytesTransferredComparator implements Comparator<Host> {

		@Override
		public int compare(Host h1, Host h2) {
	        return (h1.stat_replyBytesTransferred == h2.stat_replyBytesTransferred ?  0 :	// Values are equal
	            (h1.stat_replyBytesTransferred < h2.stat_replyBytesTransferred ? -1 :		// (-0.0, 0.0) or (!NaN, NaN)
	             1));
		}
		
	}
	
	
	public static class AvgNewFlowsPerSecComparator implements Comparator<Host> {

		@Override
		public int compare(Host h1, Host h2) {
	        return (h1.stat_avgNewFlowsPerSec == h2.stat_avgNewFlowsPerSec ?  0 :	// Values are equal
	            (h1.stat_avgNewFlowsPerSec < h2.stat_avgNewFlowsPerSec ? -1 :		// (-0.0, 0.0) or (!NaN, NaN)
	             1));
		}
		
	}
	
}

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.security.SecureRandom;
import java.util.Vector;

import evaluation.traceParser.engine.TraceInfo;
import evaluation.traceParser.engine.dataStructure.Flow;
import evaluation.traceParser.engine.dataStructure.Host;
import evaluation.traceParser.engine.dataStructure.ModifiableHost;
import evaluation.traceParser.engine.fileReader.FlowReader;
import evaluation.traceParser.scenarioExtractor.flowFilter.FlowFilter;


public class HostSampleSource {

	public enum UrnModel {DRAW_WITH_REPLACEMENT, DRAW_WITHOUT_REPLACEMENT};
	
	private final static String URN_EMPTY_ERROR_MESSAGE = "no more samples available (urn is empty).\nconsider using a larger trace file or UrnModel.DRAW_WITH_REPLACEMENT.";
	private SecureRandom secureRandom;
	public RandomAccessFile sourceTrace;
	public TraceInfo traceInfo;
	public Host[] hosts;
	
	public FlowFilter filter;
	public int[] blacklist;
	public final UrnModel urnModel;
	public Vector<Host> urn_host;
	public Vector<Integer> urn_numberOfFlows;
	public Vector<Integer> urn_numberOfFlowGroups;
	public Vector<Integer> urn_onlineTime;
	public Vector<Long> urn_requestBytesTransferred;
	public Vector<Long> urn_replyBytesTransferred;
	public Vector<Integer> urn_userThinkTime;
	public int numberOfFlows = 0;
	
	
	/*public HostSampleSource(UrnModel urnModel, String pathToTraceFolder, FlowFilter filter) {
		this(urnModel, pathToTraceFolder, filter, new SecureRandom());

	}
	
	
	public HostSampleSource(UrnModel urnModel, TraceInfo traceInfo, FlowFilter filter) {
		this(urnModel, traceInfo, filter, new SecureRandom());
	}
	
	
	public HostSampleSource(UrnModel urnModel, Host[] hosts, RandomAccessFile sourceTrace, FlowFilter filter) {
		this(urnModel, hosts, sourceTrace, filter, new SecureRandom());
	}*/
	

	public HostSampleSource(UrnModel urnModel, String pathToTraceFolder, FlowFilter filter, SecureRandom secureRandom, int[] blacklist) {
		this(urnModel, new TraceInfo(pathToTraceFolder), filter, secureRandom, blacklist);
		//System.err.println("WARNING: HostSampleSource was initialized without a blacklist. all connections of all hosts will be used for creating samples.");
	}
	
	
	public HostSampleSource(UrnModel urnModel, TraceInfo traceInfo, FlowFilter filter, SecureRandom secureRandom, int[] blacklist) {
		this(urnModel, Host.getHostIndex(traceInfo, filter), traceInfo, initRandomAccessFile(traceInfo), filter, secureRandom, blacklist);
		//System.err.println("WARNING: HostSampleSource was initialized without a blacklist. all connections of all hosts will be used for creating samples.");
	}
	
	
	public HostSampleSource(UrnModel urnModel, Host[] hosts, TraceInfo traceInfo, RandomAccessFile sourceTrace, FlowFilter filter, SecureRandom secureRandom, int[] blacklist) {
		this.filter = filter;
		this.urnModel = urnModel;
		this.urn_host = new Vector<Host>((int) (hosts.length * 1.3));
		this.hosts = hosts;
		for (Host h: hosts)
			this.urn_host.add(h);
		this.secureRandom = secureRandom; 
		this.sourceTrace = sourceTrace;
		this.blacklist = blacklist;
		this.traceInfo = traceInfo;
		//this.pathToTraceFile = Util.removeFileExtension(traceInfo.getPathToTraceFile()) +".gmf";
		if (urnModel == UrnModel.DRAW_WITHOUT_REPLACEMENT) {
			this.urn_numberOfFlows = new Vector<Integer>((int) (hosts.length * 1.3));
			this.urn_numberOfFlowGroups = new Vector<Integer>((int) (hosts.length * 1.3));
			this.urn_onlineTime = new Vector<Integer>((int) (hosts.length * 1.3));
			this.urn_requestBytesTransferred = new Vector<Long>((int) (hosts.length * 1.3));
			this.urn_replyBytesTransferred = new Vector<Long>((int) (hosts.length * 1.3));
			this.urn_userThinkTime = new Vector<Integer>((int) (hosts.length * 3));
			for (Host h: hosts) {
				numberOfFlows += h.stat_numberOfFlows;
				this.urn_numberOfFlows.add(h.stat_numberOfFlows);
				this.urn_numberOfFlowGroups.add(h.stat_numberOfFlowGroups);
				this.urn_onlineTime.add(h.stat_onlineTime);
				this.urn_requestBytesTransferred.add(h.stat_requestBytesTransferred);
				this.urn_replyBytesTransferred.add(h.stat_replyBytesTransferred);
				for (int i=0; i<h.stat_userThinkTimes.length; i++)
					this.urn_userThinkTime.add(h.stat_userThinkTimes[i]);
			} 
		} else {
			for (Host h: hosts)
				numberOfFlows += h.stat_numberOfFlows;
		}
	}
	
	
	private static RandomAccessFile initRandomAccessFile(TraceInfo traceInfo) {
		String filePath = traceInfo.getPathToGmf();
		RandomAccessFile file;
		try {
			file = new RandomAccessFile(filePath, "r");
		} catch (IOException e) {
			throw new RuntimeException("ERROR: could not open file "  +filePath);
		}
		return file;
	}
	
	
	public int remainingHosts() {
		return urn_host.size();
	}
	
	
	public ModifiableHost drawRandomHost() {
		Host h = drawRandomHostSample();
		h.loadFlowGroups(sourceTrace);
		return new ModifiableHost(h);
	}

	
	private Host drawRandomHostSample() {
		if (urnModel == UrnModel.DRAW_WITH_REPLACEMENT) {
			return urn_host.get(secureRandom.nextInt(urn_host.size()));
		} else if (urnModel == UrnModel.DRAW_WITHOUT_REPLACEMENT) {
			if (urn_host.size() == 0)
				throw new RuntimeException("no more hosts available (urn is empty).\nconsider using a larger trace file or UrnModel.DRAW_WITH_REPLACEMENT.\nto check the number of remaining hosts with \"remainingHosts()\""); 
			return urn_host.remove(secureRandom.nextInt(urn_host.size()));
		} else {
			throw new InternalError("no implementation for the urn model " +urnModel +" yet");
		}
	}

	
	private long[] flowIndex;
	
	
	public Flow drawRandomFlow() { // TODO: without replacement (store already drawn ids)
		if (flowIndex == null) { // create flow index
			System.out.println("creating flow index"); 
			long start = System.currentTimeMillis();
			flowIndex = new long[numberOfFlows];
			try {
				FlowReader fr = new FlowReader(traceInfo, filter, false);
				Flow flow;
				int indexCounter = 0;
				int blackListCounter = 0;
				int currentBlacklistedHost = -1;
				while (true) {
					flow = fr.readFlow();
					if (flow == null)
						break;
					while (currentBlacklistedHost < flow.senderId && blackListCounter < blacklist.length) // both blacklist and trace are ordered by senderid
						currentBlacklistedHost = blacklist[blackListCounter++];
					if (flow.senderId == currentBlacklistedHost)
						continue;
					try {flowIndex[indexCounter++] = fr.getOffsetOfLastFlow();} catch (ArrayIndexOutOfBoundsException e) {System.err.println("indexCounter: " +indexCounter); System.err.println("currentBlacklistedHost: " +currentBlacklistedHost); System.err.println("flow.senderId: " +flow.senderId); throw new RuntimeException("");  }
				}
				assert indexCounter == numberOfFlows: "indexCounter: " +indexCounter +", numberOfFlows: " +numberOfFlows;
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("flow index created (duration: " +(System.currentTimeMillis() - start) +" ms)");
		}
		try {
			int flowNumber = secureRandom.nextInt(numberOfFlows);
			sourceTrace.seek(flowIndex[flowNumber]);
			BufferedReader reader = new BufferedReader(Channels.newReader(sourceTrace.getChannel(), "ISO-8859-1"));
			FlowReader fr = new FlowReader(reader, filter);
			return fr.readFlow();
		} catch (IOException e) {
			throw new RuntimeException("ERROR: " +e.getLocalizedMessage()); 
		}
		/*try {
			int flowNumber = secureRandom.nextInt(numberOfFlows);
			int ctr = 0;
			for (Host host: urn_host) {
				if (flowNumber < (ctr + host.stat_numberOfFlows)) {
					FlowReader fr = host.getFlowReader(sourceTrace);
					for (int i=0; i<(flowNumber - ctr); i++)
						fr.readLine(); // skip flow
					return fr.readFlow();
				} else {
					ctr += host.stat_numberOfFlows;
				}
			} 
			throw new RuntimeException("implementation error"); 
		} catch (IOException e) {
			throw new RuntimeException("ERROR: " +e.getLocalizedMessage()); 
		}*/
	}
	
	
	public int drawRandomSample_numberOfFlows() {
		if (urnModel == UrnModel.DRAW_WITH_REPLACEMENT) {
			return drawRandomHostSample().stat_numberOfFlows;
		} else if (urnModel == UrnModel.DRAW_WITHOUT_REPLACEMENT) {
			if (urn_numberOfFlows.size() == 0)
				throw new RuntimeException(URN_EMPTY_ERROR_MESSAGE); 
			return urn_numberOfFlows.remove(secureRandom.nextInt(urn_numberOfFlows.size()));
		} else {
			throw new InternalError("no implementation for the urn model " +urnModel +" yet");
		}
	}
	
	
	public int drawRandomSample_numberOfFlowGroups() {
		if (urnModel == UrnModel.DRAW_WITH_REPLACEMENT) {
			return drawRandomHostSample().stat_numberOfFlowGroups;
		} else if (urnModel == UrnModel.DRAW_WITHOUT_REPLACEMENT) {
			if (urn_numberOfFlowGroups.size() == 0)
				throw new RuntimeException(URN_EMPTY_ERROR_MESSAGE); 
			return urn_numberOfFlowGroups.remove(secureRandom.nextInt(urn_numberOfFlowGroups.size()));
		} else {
			throw new InternalError("no implementation for the urn model " +urnModel +" yet");
		}
	}
	
	
	public int drawRandomSample_onlineTime() {
		if (urnModel == UrnModel.DRAW_WITH_REPLACEMENT) {
			return drawRandomHostSample().stat_onlineTime;
		} else if (urnModel == UrnModel.DRAW_WITHOUT_REPLACEMENT) {
			if (urn_onlineTime.size() == 0)
				throw new RuntimeException(URN_EMPTY_ERROR_MESSAGE); 
			return urn_onlineTime.remove(secureRandom.nextInt(urn_onlineTime.size()));
		} else {
			throw new InternalError("no implementation for the urn model " +urnModel +" yet");
		}
	}
	
	
	public long drawRandomSample_requestBytesTransferred() {
		if (urnModel == UrnModel.DRAW_WITH_REPLACEMENT) {
			return drawRandomHostSample().stat_requestBytesTransferred;
		} else if (urnModel == UrnModel.DRAW_WITHOUT_REPLACEMENT) {
			if (urn_requestBytesTransferred.size() == 0)
				throw new RuntimeException(URN_EMPTY_ERROR_MESSAGE); 
			return urn_requestBytesTransferred.remove(secureRandom.nextInt(urn_requestBytesTransferred.size()));
		} else {
			throw new InternalError("no implementation for the urn model " +urnModel +" yet");
		}
	}
	
	
	public long drawRandomSample_replyBytesTransferred() {
		if (urnModel == UrnModel.DRAW_WITH_REPLACEMENT) {
			return drawRandomHostSample().stat_replyBytesTransferred;
		} else if (urnModel == UrnModel.DRAW_WITHOUT_REPLACEMENT) {
			if (urn_replyBytesTransferred.size() == 0)
				throw new RuntimeException(URN_EMPTY_ERROR_MESSAGE); 
			return urn_replyBytesTransferred.remove(secureRandom.nextInt(urn_replyBytesTransferred.size()));
		} else {
			throw new InternalError("no implementation for the urn model " +urnModel +" yet");
		}
	}

	
	public int drawRandomSample_userThinkTime() {
		if (urnModel == UrnModel.DRAW_WITH_REPLACEMENT) {
			Host h;
			do {
				h = drawRandomHostSample();
			} while (h.stat_userThinkTimes == null);
			return h.stat_userThinkTimes[secureRandom.nextInt(h.stat_userThinkTimes.length)];
		} else if (urnModel == UrnModel.DRAW_WITHOUT_REPLACEMENT) {
			if (urn_userThinkTime.size() == 0)
				throw new RuntimeException(URN_EMPTY_ERROR_MESSAGE); 
			return urn_userThinkTime.remove(secureRandom.nextInt(urn_userThinkTime.size()));
		} else {
			throw new InternalError("no implementation for the urn model " +urnModel +" yet");
		}
	}
	
	
	public int drawRandomSample_userThinkTime(int exclusiveUpperBound) {
		int result;
		do {
			result = drawRandomSample_userThinkTime();
		} while (result >= exclusiveUpperBound);
		return result;
	}
	
}

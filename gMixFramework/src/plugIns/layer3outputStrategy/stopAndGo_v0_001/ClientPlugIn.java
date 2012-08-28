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
package plugIns.layer3outputStrategy.stopAndGo_v0_001;

import java.security.SecureRandom;
import java.util.Vector;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ExponentialDistributionImpl;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer1NetworkClient;
import framework.core.interfaces.Layer2RecodingSchemeClient;
import framework.core.interfaces.Layer3OutputStrategyClient;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.MixList;
import framework.core.routing.RoutingMode;
import framework.core.util.Util;


//Kesdogan et. al. 1998: Stop-and-Go MIXes: Providing Probabilistic Anonymity in an Open System
public class ClientPlugIn extends Implementation implements Layer3OutputStrategyClient {

	private Layer1NetworkClient layer1;
	private Layer2RecodingSchemeClient layer2;
	private ExponentialDistributionImpl expDist;
	private static SecureRandom secureRandom = new SecureRandom();
	private double SECURITY_PARAMETER_MU;
	private boolean USE_TIMESTAMPS;
	private int MIN_INTER_MIX_DELAY;
	private int MAX_INTER_MIX_DELAY;
	private int MIN_CLIENT_MIX_DELAY;
	private int MAX_CLIENT_MIX_DELAY;
	private int MAX_CLOCK_DEVITION;
	private Vector<Reply> replyCache;
	private int availableReplyPayload = 0;
	private MixList route;
	
	
	@Override
	public void constructor() {
		this.SECURITY_PARAMETER_MU = settings.getPropertyAsDouble("STOP_AND_GO_SECURITY_PARAMETER_MU");
		this.USE_TIMESTAMPS = settings.getPropertyAsBoolean("STOP_AND_GO_USE_TIMESTAMPS");
		this.MIN_INTER_MIX_DELAY = settings.getPropertyAsInt("STOP_AND_GO_MIN_INTER_MIX_DELAY");
		this.MAX_INTER_MIX_DELAY = settings.getPropertyAsInt("STOP_AND_GO_MAX_INTER_MIX_DELAY");
		this.MAX_INTER_MIX_DELAY = settings.getPropertyAsInt("STOP_AND_GO_MAX_INTER_MIX_DELAY");
		this.MIN_CLIENT_MIX_DELAY = settings.getPropertyAsInt("STOP_AND_GO_MIN_CLIENT_MIX_DELAY");
		this.MAX_CLIENT_MIX_DELAY = settings.getPropertyAsInt("STOP_AND_GO_MAX_CLIENT_MIX_DELAY");
		this.MAX_CLOCK_DEVITION = settings.getPropertyAsInt("STOP_AND_GO_MAX_CLOCK_DEVITION");
		this.expDist = new ExponentialDistributionImpl(1d/SECURITY_PARAMETER_MU);
	}

	
	@Override
	public void initialize() {
		if (anonNode.IS_DUPLEX)
			this.replyCache = new Vector<Reply>();
	}

	
	@Override
	public void begin() {
		
	}

	
	@Override
	public void setReferences(
			Layer1NetworkClient layer1,
			Layer2RecodingSchemeClient layer2, 
			Layer3OutputStrategyClient layer3) {
		this.layer2 = layer2;
		this.layer1 = layer1;
		assert this == layer3;
	}
	
	
	@Override
	public void connect() {
		if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING)
			this.route = anonNode.mixList.getRandomRoute(anonNode.FREE_ROUTE_LENGTH);
		this.layer1.connect();
	}

	
	@Override
	public void connect(int destPseudonym) {
		if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) {
			this.route = anonNode.mixList.getRandomRoute(anonNode.FREE_ROUTE_LENGTH, destPseudonym);
			if (anonNode.DISPLAY_ROUTE_INFO)
				System.out.println(""+anonNode +" generated random route: " +this.route); 
			this.layer1.connect(this.route);
		} else {
			this.layer1.connect();
		}
	}
	
	
	@Override
	public void disconnect() {
		layer1.disconnect();
	}

	
	@Override
	public synchronized void sendMessage(Request request) {
		int numberOfMixes;
		if (anonNode.ROUTING_MODE == RoutingMode.CASCADE) {
			numberOfMixes = anonNode.mixList.numberOfMixes;
		} else { // free route
			if (!anonNode.IS_CONNECTION_BASED) { // new route for every message
				if (request.destinationPseudonym == Util.NOT_SET)
					this.route = anonNode.mixList.getRandomRoute(anonNode.FREE_ROUTE_LENGTH);
				else
					this.route = anonNode.mixList.getRandomRoute(anonNode.FREE_ROUTE_LENGTH, request.destinationPseudonym);
			} 
			request.destinationPseudonym = this.route.mixIDs[route.mixIDs.length-1];
			request.nextHopAddress = this.route.mixIDs[0];
			request.route = this.route.mixIDs;
			numberOfMixes = request.route.length;
		}
		request.headers = new byte[numberOfMixes][];
		this.expDist.reseedRandomGenerator(secureRandom.nextLong());
		// draw delays from sample
		int[] delays = new int[numberOfMixes];
		for (int i=0; i<numberOfMixes; i++) {
			try {
				delays[i] = (int)Math.round(expDist.sample() * 1000d);
			} catch (MathException e) {
				e.printStackTrace();
				throw new RuntimeException("ERROR: could not draw sample from exponential distribution!"); 
			}
		}
		// store delays and create timestamps if needed
		for (int i=0; i<numberOfMixes; i++) {
			if (USE_TIMESTAMPS) {
				request.headers[i] = new TimestampHeader(
						getMinTimestamp(i, delays), 
						getMaxTimestamp(i, delays), 
						delays[i]
						).headerAsArray;
			} else {
				request.headers[i] = new TimestampHeader(delays[i]).headerAsArray;
			}
		}
		request = layer2.applyLayeredEncryption(request);
		layer1.sendMessage(request);
	}

	
	// mixNumer: 0,1,...,numberOfMixes
	public long getMinTimestamp(int mixNumber, int[] sgDelays) {
		int sumOfSgDelays = 0;
		for (int i=0; i<mixNumber; i++)
			sumOfSgDelays += sgDelays[i];
		int minDelay = MIN_CLIENT_MIX_DELAY + mixNumber * MIN_INTER_MIX_DELAY;
		int maxClockDev = (mixNumber + 1) * MAX_CLOCK_DEVITION;
		return clock.getTime() + sumOfSgDelays + minDelay - maxClockDev;
	}
	
	
	public long getMaxTimestamp(int mixNumber, int[] sgDelays) {
		int sumOfSgDelays = 0;
		for (int i=0; i<mixNumber; i++)
			sumOfSgDelays += sgDelays[i];
		int maxDelay = MAX_CLIENT_MIX_DELAY + mixNumber * MAX_INTER_MIX_DELAY;
		int maxClockDev = (mixNumber + 1) * MAX_CLOCK_DEVITION;
		return clock.getTime() + sumOfSgDelays + maxDelay + maxClockDev;
	}


	@Override
	public Reply receiveReply() {
		if (replyCache.size() > 0) {
			Reply result = replyCache.remove(0);
			availableReplyPayload -= result.getByteMessage().length;
			return result;
		} else {
			Reply reply = layer1.receiveReply();
			return layer2.extractPayload(reply);
		}
	}


	@Override
	public int getMaxSizeOfNextRequest() {
		return layer2.getMaxPayloadForNextMessage();
	}


	@Override
	public int getMaxSizeOfNextReply() {
		return layer2.getMaxPayloadForNextReply();
	}


	@Override
	public int availableReplies() {
		for (int i=0; i<layer1.availableReplies(); i++) {
			Reply reply = layer1.receiveReply();
			replyCache.add(layer2.extractPayload(reply));
			availableReplyPayload += reply.getByteMessage().length;
		} 
		return replyCache.size();
	}


	@Override
	public int availableReplyPayload() {
		for (int i=0; i<layer1.availableReplies(); i++) {
			Reply reply = layer1.receiveReply();
			replyCache.add(layer2.extractPayload(reply));
			availableReplyPayload += reply.getByteMessage().length;
		} 
		return availableReplyPayload;
	}
}

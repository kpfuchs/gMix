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
package plugIns.layer3outputStrategy.constantRate_v0_001;

import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer1NetworkClient;
import framework.core.interfaces.Layer2RecodingSchemeClient;
import framework.core.interfaces.Layer3OutputStrategyClient;
import framework.core.logger.OutputCap;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.MixList;
import framework.core.routing.RoutingMode;
import framework.core.util.Util;


public class ClientPlugIn extends Implementation implements Layer3OutputStrategyClient, Runnable {

	private Layer1NetworkClient layer1;
	private Layer2RecodingSchemeClient layer2;
	private AtomicBoolean isSending = new AtomicBoolean(false);
	private AtomicBoolean isStopped = new AtomicBoolean(false);
	
	private int rate; // in ns
	private ScheduledThreadPoolExecutor scheduler;
	private ScheduledFuture<?> currentTimer;
	private ArrayBlockingQueue<Request> requestQueue;
	private static OutputCap warning;
	private Vector<Reply> replyCache;
	private int availableReplyPayload = 0;
	private MixList route;
	
	
	@Override
	public void constructor() {
		this.rate = settings.getPropertyAsInt("CONSTANT_RATE_SEND_RATE");
		this.requestQueue = new ArrayBlockingQueue<Request>(settings.getPropertyAsInt("CONSTANT_RATE_QUEUE_SIZE"));
		this.scheduler = new ScheduledThreadPoolExecutor(1);
		ClientPlugIn.warning = new OutputCap(this.getClass().getCanonicalName() +" cannont send messages as fast as specified (" +rate +")", 2000);
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
		this.currentTimer = scheduler.scheduleAtFixedRate(this, rate, rate, TimeUnit.MILLISECONDS);
		this.isStopped.set(false);
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
		this.currentTimer = scheduler.scheduleAtFixedRate(this, rate, rate, TimeUnit.MILLISECONDS);
		this.isStopped.set(false);
	}
	
	
	@Override
	public void disconnect() {
		currentTimer.cancel(false);
		scheduler.shutdownNow();
		synchronized (this) {
			isStopped.set(true);
		}
		layer1.disconnect();
	}

	
	@Override
	public void sendMessage(Request request) {
		if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) {
			if (!anonNode.IS_CONNECTION_BASED) { // new route for every message
				if (request.destinationPseudonym == Util.NOT_SET)
					this.route = anonNode.mixList.getRandomRoute(anonNode.FREE_ROUTE_LENGTH);
				else
					this.route = anonNode.mixList.getRandomRoute(anonNode.FREE_ROUTE_LENGTH, request.destinationPseudonym);
			} 
			request.destinationPseudonym = this.route.mixIDs[route.mixIDs.length-1];
			request.nextHopAddress = this.route.mixIDs[0];
			request.route = this.route.mixIDs;
		}
		request = layer2.applyLayeredEncryption(request);
		forcePutInQueue(request);
	}
	
	
	private void forcePutInQueue(Request request) {
		try {
			requestQueue.put(request);
		} catch (InterruptedException e) {
			e.printStackTrace();
			forcePutInQueue(request);
		}
	}
	

	@Override
	public void run() {
		if (isSending.get())
			warning.putOut();
		isSending.set(true);
		synchronized (this) {
			if (isStopped.get() == false) {
				sendMessage();
				isSending.set(false);
			}
		}
	}
	
	
	private void sendMessage() {
		if (requestQueue.size() == 0) { // send dummy
			Request dummy = MixMessage.getInstanceRequest(new byte[0]);
			layer1.sendMessage(dummy);
		} else {
			try {
				layer1.sendMessage(requestQueue.take());
			} catch (InterruptedException e) {
				e.printStackTrace();
				sendMessage();
			}
		}
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

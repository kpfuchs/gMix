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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer3outputStrategy.constantRate_v0_001;

import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer1NetworkClient;
import staticContent.framework.interfaces.Layer2RecodingSchemeClient;
import staticContent.framework.interfaces.Layer3OutputStrategyClient;
import staticContent.framework.interfaces.Layer4TransportClient;
import staticContent.framework.logger.OutputCap;
import staticContent.framework.message.MixMessage;
import staticContent.framework.message.Reply;
import staticContent.framework.message.Request;
import staticContent.framework.message.ExternalMessage.DummyStatus;
import staticContent.framework.routing.MixList;
import staticContent.framework.routing.RoutingMode;
import staticContent.framework.util.Util;


public class ClientPlugIn extends Implementation implements Layer3OutputStrategyClient, Runnable {

	private Layer1NetworkClient layer1;
	private Layer2RecodingSchemeClient layer2;
	private AtomicBoolean isSending = new AtomicBoolean(false);
	private AtomicBoolean isStopped = new AtomicBoolean(false);
	
	private int rate; // in ns
	private ScheduledThreadPoolExecutor scheduler;
	private ScheduledFuture<?> currentTimer;
	private ArrayBlockingQueue<byte[]> requestQueue;
	private static OutputCap warning;
	private Vector<byte[]> replyCache;
	private int availableReplyPayload = 0;
	private MixList route;
	private int dstPseudonym = Util.NOT_SET;
	
	
	@Override
	public void constructor() {
		this.rate = settings.getPropertyAsInt("CONSTANT_RATE_SEND_RATE");
		this.requestQueue = new ArrayBlockingQueue<byte[]>(settings.getPropertyAsInt("CONSTANT_RATE_QUEUE_SIZE"));
		this.scheduler = new ScheduledThreadPoolExecutor(1);
		ClientPlugIn.warning = new OutputCap(this.getClass().getCanonicalName() +" cannont send messages as fast as specified (" +rate +")", 2000);
	}

	
	@Override
	public void initialize() {
		if (anonNode.IS_DUPLEX)
			this.replyCache = new Vector<byte[]>();
	}

	
	@Override
	public void begin() {
		
	}

	
	@Override
	public void setReferences(
			Layer1NetworkClient layer1,
			Layer2RecodingSchemeClient layer2, 
			Layer3OutputStrategyClient layer3,
			Layer4TransportClient layer4) {
		this.layer2 = layer2;
		this.layer1 = layer1;
		assert this == layer3;
	}
	
	
	@Override
	public void connect() {
		if (anonNode.ROUTING_MODE == RoutingMode.SOURCE_ROUTING)
			this.route = sourceRoutingPlugInClient.choseRoute();
		this.layer1.connect();
		this.currentTimer = scheduler.scheduleAtFixedRate(this, rate, rate, TimeUnit.MILLISECONDS);
		this.isStopped.set(false);
	}

	
	@Override
	public void connect(int destPseudonym) {
		if (anonNode.ROUTING_MODE == RoutingMode.SOURCE_ROUTING) {
			this.route = sourceRoutingPlugInClient.choseRoute(destPseudonym);
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
	public void write(byte[] data) {
		if (data == null || data.length == 0)
			throw new RuntimeException("write(null) and write(byte[0]) are not allowed"); 
		forcePutInQueue(data);
	}
	
	
	@Override
	public void write(byte[] data, int destPseudonym) {
		this.dstPseudonym = destPseudonym;
		write(data);
	}
	
	
	private void forcePutInQueue(byte[] data) {
		try {
			requestQueue.put(data);
		} catch (InterruptedException e) {
			e.printStackTrace();
			forcePutInQueue(data);
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
	
	
	// TODO: might be prone to timing attacks (currently no fixed schedule for sending, but only for creating messages...)
	private void sendMessage() {
		Request request = null;
		if (requestQueue.size() == 0) { // send dummy
			request = MixMessage.getInstanceRequest(new byte[0]);
			if (this.dstPseudonym != Util.NOT_SET)
				request.destinationPseudonym = this.dstPseudonym;
		} else {
			try {
				request = MixMessage.getInstanceRequest(requestQueue.take());
				if (this.dstPseudonym != Util.NOT_SET)
					request.destinationPseudonym = this.dstPseudonym;
			} catch (InterruptedException e) {
				e.printStackTrace();
				sendMessage();
			}
		}
		if (anonNode.ROUTING_MODE == RoutingMode.SOURCE_ROUTING) {
			if (!anonNode.IS_CONNECTION_BASED) { // new route for every message
				if (request.destinationPseudonym == Util.NOT_SET)
					this.route = sourceRoutingPlugInClient.choseRoute();
				else
					this.route = sourceRoutingPlugInClient.choseRoute(request.destinationPseudonym);
			} 
			request.destinationPseudonym = this.route.mixIDs[route.mixIDs.length-1];
			request.nextHopAddress = this.route.mixIDs[0];
			request.route = this.route.mixIDs;
		}
		request = layer2.applyLayeredEncryption(request);
		layer1.sendMessage(request);
	}
	
	
	
	@Override
	public byte[] receive() {
		if (replyCache.size() > 0) {
			byte[] result = replyCache.remove(0);
			availableReplyPayload -= result.length;
			return result;
		} else {
			Reply reply = layer1.receiveReply();
			reply = layer2.extractPayload(reply);
			assert reply.getDummyStatus() != DummyStatus.UNKNOWN;
			while (reply.getDummyStatus() == DummyStatus.DUMMY) {
				reply = layer1.receiveReply();
				reply = layer2.extractPayload(reply);
			}
			assert reply.getByteMessage() != null && reply.getByteMessage().length != 0;
			return reply.getByteMessage();
		}
	}


	@Override
	public int getMaxSizeOfNextWrite() {
		return layer2.getMaxPayloadForNextMessage();
	}


	@Override
	public int getMaxSizeOfNextReceive() {
		return layer2.getMaxPayloadForNextReply();
	}


	@Override
	public int availableReplies() {
		tryFillCache();
		return replyCache.size();
	}


	@Override
	public int availableReplyData() {
		tryFillCache();
		return availableReplyPayload;
	}
	
	
	private void tryFillCache() {
		for (int i=0; i<layer1.availableReplies(); i++) {
			Reply reply = layer1.receiveReply();
			reply = layer2.extractPayload(reply);
			assert reply.getDummyStatus() != DummyStatus.UNKNOWN;
			if (reply.getDummyStatus() == DummyStatus.NO_DUMMY) {
				replyCache.add(reply.getByteMessage());
				availableReplyPayload += reply.getByteMessage().length;
				assert reply.getByteMessage() != null && reply.getByteMessage().length != 0;
			}
		}
	}
	
}

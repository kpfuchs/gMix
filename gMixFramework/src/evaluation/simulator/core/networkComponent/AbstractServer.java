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
package evaluation.simulator.core.networkComponent;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.event.EventExecutor;
import evaluation.simulator.core.event.SimulationEvent;
import evaluation.simulator.core.message.EndToEndMessage;
import evaluation.simulator.pluginRegistry.StatisticsType;


public abstract class AbstractServer implements EventExecutor {

	private DistantProxy distantProxy;
	protected boolean SIMULATE_REPLY_CHANNEL;
	protected boolean UNLIMITED_BANDWIDTH;
	protected Simulator simulator;
	
	
	// TODO: update comments
	protected AbstractServer(DistantProxy distantProxy) {
		this.distantProxy = distantProxy;
		this.SIMULATE_REPLY_CHANNEL = Simulator.settings.getProperty("COMMUNICATION_MODE").equals("SIMPLEX_REPLY") || Simulator.settings.getProperty("COMMUNICATION_MODE").equals("DUPLEX");
		this.UNLIMITED_BANDWIDTH = Simulator.settings.getProperty("TYPE_OF_DELAY_BOX").equals("NO_DELAY");
		this.simulator = Simulator.getSimulator();
	}
	
	
	public static AbstractServer getInstance(DistantProxy distantProxy) {
		return new Server(distantProxy);
	}
	
	
	/**
	 * called when a message from a client has reached this server.
	 * subclasses can use the method sendReplyIn(long delay, EndToEndMessage 
	 * reply) in their implementation of this method, to send one or more 
	 * replies to the client.
	 * 
	 * example:
	 * if (simulateReplyChannel) {
	 *   Transaction at = message.payload;
	 *   float[] replyDelays = at.getDistinctReplyDelays();
	 *   int[] replySizes = at.getDistinctReplySizes();
	 *   if (replySizes == null || replySizes.length == 0) { // no reply
	 *     return;
	 *   } else { // create and schedule replies:
	 *     for (int i=0; i<replySizes.length; i++) {
	 *       long delay = Transaction.delayToMilliSec(replyDelays[i]);
	 *       EndToEndMessage reply = message.createReplyForThisMessage(at, replySizes[i]);
	 *       sendReplyIn(delay, reply);
	 *     }
	 *   }
	 * }
	 * 
	 * @param message
	 */
	public abstract void incomingMessage(EndToEndMessage message);


	/**
	 * can be used by subclasses to send a reply to a client (the subclass must 
	 * have received a message via incomingMessage(EndToEndMessage message) 
	 * before this method can be called)
	 * 
	 * example:
	 * EndToEndMessage reply = message.createReplyForThisMessage(at, 100);
	 * sendReplyIn(10, reply);
	 * @param message
	 */
	public void sendReplyIn(long delay, EndToEndMessage reply) {
		Event sendReplyEvent = new Event(this, Simulator.getNow() + delay, ServerEvent.TIMEOUT, reply);
		//System.out.println("scheduling sendMessage() for transaction " +((EndToEndMessage)sendReplyEvent.getAttachment()).getPayload().getTransactionId() +" (now: " +Simulator.getNow() +")"); 
		simulator.scheduleEvent(sendReplyEvent, this);
	}
	
	
	
	private void sendMessage(EndToEndMessage eteMessage) {
		if (SIMULATE_REPLY_CHANNEL) {
			distantProxy.statistics.increment(eteMessage.transportMessage.getLength(), StatisticsType.SUM_DISTANTPROXY_DATAVOLUME_SENDANDRECEIVE);
			distantProxy.requestAnswered(eteMessage.transportMessage);
		}
	}
	
	
	@Override
	public void executeEvent(Event event) {
		if (event.getEventType() instanceof ServerEvent && event.getEventType() == ServerEvent.TIMEOUT) {
			//System.out.println("calling sendMessage() for transaction " +((EndToEndMessage)event.getAttachment()).getPayload().getTransactionId() +" (now: " +Simulator.getNow() +", object-ref: " +event.getAttachment() +"), size: " +((EndToEndMessage)event.getAttachment()).transportMessage.getLength()); 
			sendMessage((EndToEndMessage)event.getAttachment());
		} else {
			throw new RuntimeException("ERROR: ServerSimulator received wrong Event: " +event.getEventType().toString()); 
		}
	}
	
	
	public enum ServerEvent implements SimulationEvent {
		TIMEOUT;
	}
	
}

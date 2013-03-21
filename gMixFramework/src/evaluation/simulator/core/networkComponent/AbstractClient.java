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
package evaluation.simulator.core.networkComponent;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.event.ClientEvent;
import evaluation.simulator.core.event.DistantProxyEvent;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.event.MixEvent;
import evaluation.simulator.core.message.EndToEndMessage;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.message.NetworkMessage;
import evaluation.simulator.core.message.TransportMessage;
import evaluation.simulator.pluginRegistry.StatisticsType;
import evaluation.simulator.plugins.clientSendStyle.ClientSendStyleImpl;


public abstract class AbstractClient extends NetworkNode {
	
	protected Simulator simulator;
	protected ClientSendStyleImpl clientSendStyle;
	private int messageCreationTime;
	private int messageDecryptionTime;
	protected int clientId;
	private static int idCounter = 0;
	protected final boolean simulateReplyChannel;
	protected final boolean closedLoopSending;
	public int latest;
	
	
	public AbstractClient(String identifier, Simulator simulator) {
		super(identifier, simulator);
		this.simulator = simulator;
		this.messageCreationTime = Simulator.settings.getPropertyAsInt("MIX_REQUEST_CREATION_TIME"); // in ms
		this.messageDecryptionTime = Simulator.settings.getPropertyAsInt("MIX_REPLY_DECRYPTION_TIME"); // in ms
		this.clientId = idCounter++;
		this.simulateReplyChannel = Simulator.settings.getProperty("COMMUNICATION_MODE").equals("SIMPLEX_REPLY") || Simulator.settings.getProperty("COMMUNICATION_MODE").equals("DUPLEX");
		this.closedLoopSending = Simulator.settings.getProperty("COMMUNICATION_MODE").equals("SIMPLEX_WITH_FEEDBACK");
	}
	
	
	/**
	 * called when a message (reply from a server to a message previously sent 
	 * with the method sendMessage(EndToEndMessage message)) has reached this 
	 * client.
	 * @param message
	 */
	public abstract void incomingMessage(EndToEndMessage message);
	
	
	/**
	 * called when a message (that was previously sent with the method 
	 * sendMessage(EndToEndMessage message)) has reached the server simulator.
	 * can for example be used to assert that messages aren't replayed too fast 
	 * (i.e. before previous messages are transmitted) in simplex mode
	 * @param message
	 */
	public abstract void messageReachedServer(EndToEndMessage message);
	
	
	/**
	 * subclasses may use this method to send messages to a server (messages 
	 * will be routed via mixes if mixes are specified in the simulation 
	 * script).
	 * @param message
	 */
	public TransportMessage sendMessage(EndToEndMessage message) {
		TransportMessage toSend = new TransportMessage(true, this, simulator.getDistantProxy(), Simulator.getNow(), this, message.getPayload().getRequestSize(), message);
		Simulator.trafficSourceStatistics.increment(1, StatisticsType.AVG_TRAFFICSOURCE_SENDING_RATE_PER_CLIENT);
		statistics.addValue(toSend.getLength(), StatisticsType.ADU_SIZE_SEND);
		statistics.addValue(toSend.getLength(), StatisticsType.ADU_SIZE_SENDANDRECEIVE);
		statistics.addValue(toSend.getLength(), StatisticsType.CF_ADU_SIZE_SENDANDRECEIVE);
		statistics.increment(toSend.getLength(), StatisticsType.CF_AVG_THROUGHPUT_PER_CLIENT_SEND);
		statistics.increment(toSend.getLength(), StatisticsType.CF_AVG_THROUGHPUT_PER_CLIENT_SENDANDRECEIVE);
		if (Simulator.settings.getProperty("COMMUNICATION_MODE").equals("SIMPLEX_REPLY")) {
			simulator.getDistantProxy().incomingRequest(toSend);
		} else {
			clientSendStyle.incomingRequestFromUser(toSend);
		}
		return toSend;
	}
	
	
	private MixMessage generateMixRequest() {
		return MixMessage.getInstance(true, this, simulator.getDistantProxy(), this, Simulator.getNow(), false);
	}

	
	private int getMixRequestEncryptionTime() {
		return messageCreationTime;
	}
	
	
	private int getMixReplyDecryptionTime() {
		return messageDecryptionTime;
	}
	
	
	@Override
	public void executeEvent(Event event) {
		
		if (event.getEventType() instanceof MixEvent && event.getEventType() == MixEvent.INCOMING_REPLY_FROM_DISTANT_PROXY) {
			
			incomingDecryptedReply((NetworkMessage)event.getAttachment());
			
		} else if (!(event.getEventType() instanceof ClientEvent)) {
			
			throw new RuntimeException("ERROR: " +super.getIdentifier() +" received wrong Event: " +event.toString());

		} else {
			
			switch ((ClientEvent)event.getEventType()) {
			
				case SEND_MIX_REQUEST:
					sendToNextHop(generateMixRequest(), getMixRequestEncryptionTime(), MixEvent.INCOMING_MIX_MESSAGE_OF_TYPE_REQUEST);
					break;
					
				case REQUEST_FROM_USER:
					EndToEndMessage etem = (EndToEndMessage)event.getAttachment();
					sendMessage(etem);
					break;
					
				case REPLY_FROM_MIX:
					//System.out.println("in1: " +event.getAttachment()); 
					event.reuse(this, Simulator.getNow() + getMixReplyDecryptionTime(), ClientEvent.REPLY_DECRYPTED);
					simulator.scheduleEvent(event, this);
					break;
					
				case REPLY_DECRYPTED:
					//System.out.println("in2: " +event.getAttachment()); 
					incomingDecryptedReply((NetworkMessage)event.getAttachment());
					break;
				
				/*case INVITATION_TO_SEND_NEXT_MIX_MESSAGE:
					((ClientBasicSynchronous)clientCommunicationBehaviour).executeEvent(event);
					break;*/
					
				default:
					throw new RuntimeException("ERROR: " +super.getIdentifier() +" received unknown Event: " +event.toString());
					
			}
			
		}
		
	}
	
	
	private void incomingDecryptedReply(NetworkMessage networkMessage) {
		
		if (networkMessage instanceof TransportMessage) {
			
			TransportMessage tm = (TransportMessage)networkMessage;
			statistics.addValue(Simulator.getNow() - tm.getCreationTime(), StatisticsType.AVG_CLIENT_RTT_LAYER5MESSAGE);
			statistics.addValue(tm.getLength(), StatisticsType.ADU_SIZE_RECEIVE);
			statistics.addValue(tm.getLength(), StatisticsType.ADU_SIZE_SENDANDRECEIVE);
			statistics.addValue(tm.getLength(), StatisticsType.CF_ADU_SIZE_SENDANDRECEIVE);
			incomingMessage(tm.reltedEndToEndMessage);
			clientSendStyle.incomingDecryptedReply(tm);
			
		} else if (networkMessage instanceof MixMessage) {
			MixMessage mixMessage = (MixMessage)networkMessage;
			
			statistics.addValue(Simulator.getNow() - mixMessage.getCreationTime(), StatisticsType.AVG_CLIENT_LATENCY_REPLYMIXMESSAGE);
			
			if (!mixMessage.isDummy())
				for (TransportMessage tm: mixMessage.getTransportMessagesContained()) {
					statistics.addValue(tm.getLength(), StatisticsType.ADU_SIZE_RECEIVE);
					statistics.addValue(tm.getLength(), StatisticsType.ADU_SIZE_SENDANDRECEIVE);
					statistics.addValue(tm.getLength(), StatisticsType.CF_ADU_SIZE_SENDANDRECEIVE);
					statistics.addValue(Simulator.getNow() - tm.getCreationTime(), StatisticsType.AVG_CLIENT_RTT_LAYER5MESSAGE);
					statistics.increment(tm.getLength(), StatisticsType.CF_AVG_THROUGHPUT_PER_CLIENT_RECEIVE);
					statistics.increment(tm.getLength(), StatisticsType.CF_AVG_THROUGHPUT_PER_CLIENT_SENDANDRECEIVE);
					
					incomingMessage(tm.reltedEndToEndMessage);
				}
			clientSendStyle.incomingDecryptedReply(mixMessage);
			
		} else 
			throw new RuntimeException("ERROR: unknown MESSAGE_FORMAT! " +networkMessage);

	}
	
	
	// called by associated ClientCommunicationBehaviour (hides implementation-details)
	public void sendRequest(NetworkMessage networkMessage) {
		
		if (networkMessage instanceof MixMessage) {
			Simulator.trafficSourceStatistics.increment(1, StatisticsType.AVG_MIXMESSAGE_SENDING_RATE_PER_CLIENT);
			sendToNextHop(networkMessage, getMixRequestEncryptionTime(), MixEvent.INCOMING_MIX_MESSAGE_OF_TYPE_REQUEST);
		} else if (networkMessage instanceof TransportMessage) {
			sendToNextHop(networkMessage, getMixRequestEncryptionTime(), DistantProxyEvent.INCOMING_REQUEST);
		} else
			throw new RuntimeException("ERROR: Client does not support messages of type " +networkMessage +"!"); 
	}
	
	
	public int getClientId() {
		return clientId;
	}
	
	
	public static void reset() {
		idCounter = 0;
	}
	
	
	public void setSendStyle(ClientSendStyleImpl clientSendStyle) {
		if (this.clientSendStyle != null)
			throw new RuntimeException("ERROR: this method may only be envoked once!"); 
		this.clientSendStyle = clientSendStyle;
	}
	
}

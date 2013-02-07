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
package evaluation.simulator.networkComponent;

import evaluation.simulator.communicationBehaviour.ClientCommunicationBehaviour;
import evaluation.simulator.core.ClientEvent;
import evaluation.simulator.core.DistantProxyEvent;
import evaluation.simulator.core.Event;
import evaluation.simulator.core.MixEvent;
import evaluation.simulator.core.Simulator;
import evaluation.simulator.delayBox.DelayBox;
import evaluation.simulator.message.EndToEndMessage;
import evaluation.simulator.message.MixMessage;
import evaluation.simulator.message.NetworkMessage;
import evaluation.simulator.message.TransportMessage;
import evaluation.simulator.statistics.StatisticsType;
import evaluation.simulator.trafficSource.TrafficSource;


public abstract class AbstractClient extends NetworkNode {
	
	protected Simulator simulator;
	protected ClientCommunicationBehaviour clientCommunicationBehaviour;
	private int messageCreationTime;
	private int messageDecryptionTime;
	protected int clientId;
	private static int idCounter = 0;
	protected final boolean simulateReplyChannel; 
	public int latest;
	
	
	public AbstractClient(	String identifier, 
					Simulator simulator, 
					DelayBox delayBox 
					) {
		super(identifier, simulator, delayBox);
		this.simulator = simulator;
		this.clientCommunicationBehaviour = ClientCommunicationBehaviour.getInstance(this, simulator);
		this.messageCreationTime = Simulator.settings.getPropertyAsInt("MIX_REQUEST_CREATION_TIME"); // in ms
		this.messageDecryptionTime = Simulator.settings.getPropertyAsInt("MIX_REPLY_DECRYPTION_TIME"); // in ms
		//this.timeToWaitForFurtherRequests = new Integer(Settings.getProperty("TIME_TO_WAIT_FOR_FURTHER_REQUESTS")); // in ms
		this.clientId = idCounter++;
		this.simulateReplyChannel = Simulator.settings.getPropertyAsBoolean("SIMULATE_REPLY_CHANNEL");
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
		TrafficSource.statistics.addValue(1, StatisticsType.TRAFFICSOURCE_SENDING_RATE_PER_CLIENT);
		statistics.addValue(toSend.getLength(), StatisticsType.ADU_SIZE_SEND);
		statistics.addValue(toSend.getLength(), StatisticsType.ADU_SIZE_SENDANDRECEIVE);
		if (!Simulator.settings.getPropertyAsBoolean("SIMULATE_REQUEST_CHANNEL"))
			simulator.getDistantProxy().incomingRequest(toSend);
		else 
			clientCommunicationBehaviour.incomingRequestFromUser(toSend);
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
			statistics.addValue(Simulator.getNow() - tm.getCreationTime(), StatisticsType.CLIENT_RTT_NONEMIXMESSAGE);
			statistics.addValue(tm.getLength(), StatisticsType.ADU_SIZE_RECEIVE);
			statistics.addValue(tm.getLength(), StatisticsType.ADU_SIZE_SENDANDRECEIVE);
			incomingMessage(tm.reltedEndToEndMessage);
			clientCommunicationBehaviour.incomingDecryptedReply(tm);
			
		} else if (networkMessage instanceof MixMessage) {
			MixMessage mixMessage = (MixMessage)networkMessage;
			
			statistics.addValue(Simulator.getNow() - mixMessage.getCreationTime(), StatisticsType.CLIENT_LATENCY_REPLYMIXMESSAGE);
			
			if (!mixMessage.isDummy())
				for (TransportMessage tm: mixMessage.getTransportMessagesContained()) {
					statistics.addValue(tm.getLength(), StatisticsType.ADU_SIZE_RECEIVE);
					statistics.addValue(tm.getLength(), StatisticsType.ADU_SIZE_SENDANDRECEIVE);
					statistics.addValue(Simulator.getNow() - tm.getCreationTime(), StatisticsType.CLIENT_RTT_NONEMIXMESSAGE);
					incomingMessage(tm.reltedEndToEndMessage);
				}
			clientCommunicationBehaviour.incomingDecryptedReply(mixMessage);
			
		} else 
			throw new RuntimeException("ERROR: unknown MESSAGE_FORMAT! " +networkMessage);

	}
	
	
	// called by associated ClientCommunicationBehaviour (hides implementation-details)
	public void sendRequest(NetworkMessage networkMessage) {
		
		if (networkMessage instanceof MixMessage) {
			TrafficSource.statistics.addValue(1, StatisticsType.MIXMESSAGE_SENDING_RATE_PER_CLIENT);
			sendToNextHop(networkMessage, getMixRequestEncryptionTime(), MixEvent.INCOMING_MIX_MESSAGE_OF_TYPE_REQUEST);
		} else if (networkMessage instanceof TransportMessage)
			sendToNextHop(networkMessage, getMixRequestEncryptionTime(), DistantProxyEvent.INCOMING_REQUEST);
		else
			throw new RuntimeException("ERROR: Client does not support messages of type " +networkMessage +"!"); 
	}

	
	public ClientCommunicationBehaviour getClientCommunicationBehaviour() {
		return this.clientCommunicationBehaviour;
	}
	

	public int getClientId() {
		return clientId;
	}
	
	
	public static void reset() {
		idCounter = 0;
	}
	
}

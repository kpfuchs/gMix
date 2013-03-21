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
import evaluation.simulator.core.event.DistantProxyEvent;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.event.EventExecutor;
import evaluation.simulator.core.message.*;
import evaluation.simulator.core.statistics.Statistics;
import evaluation.simulator.pluginRegistry.StatisticsType;


public class NetworkConnection implements EventExecutor, Identifiable {

	private int numericIdentifier;
	private NetworkNode source;
	private NetworkNode destination;
	private Simulator simulator;
	protected Statistics statistics;
	
	
	public NetworkConnection(NetworkNode source, NetworkNode destination, Simulator simulator) {
		
		this.source = source;
		this.destination = destination;
		this.simulator = simulator;
		this.statistics = new Statistics(this);
		this.numericIdentifier = IdGenerator.getId();
		
		// register connected network nodes at each other
		source.setConnectionToNextHop(this);
		destination.setConnectionToPreviousHop(this);

	}


	@Override
	public void executeEvent(Event event) {
		
		if (event.getAttachment() == null) {
			throw new RuntimeException("ERROR: NetworkConnection received wrong Event");

		} else {
			
			NetworkMessage networkMessage = (NetworkMessage)event.getAttachment();
			NetworkNode source = networkMessage.isRequest() ? this.source : this.destination;
			NetworkNode destination = networkMessage.isRequest() ? this.destination : this.source;
			
			if (networkMessage.hasPassedSecondDelayBox()) {
				
				event.setTarget(destination);
				networkMessage.setHasPassedFirstDelayBox(false);
				networkMessage.setHasPassedSecondDelayBox(false);
				networkNodeHasReceivedMessage(networkMessage, destination);
				
			} else if (networkMessage.hasPassedFirstDelayBox()) {
				
				event.setExecutionTime(Simulator.getNow() + destination.getDelayBox().getSendDelay(networkMessage.getLength()));
				event.setTarget(this);
				networkMessage.setHasPassedSecondDelayBox(true);
				networkNodeHasSentMessage(networkMessage, source);
				
			} else {
				
				event.setExecutionTime(Simulator.getNow() + source.getDelayBox().getSendDelay(networkMessage.getLength()));
				event.setTarget(this);
				networkMessage.setHasPassedFirstDelayBox(true);
				
			}
			
			/*
			if (networkMessage.isRequest()) {
				
				
				if (networkMessage.hasPassedSecondDelayBox()) {
					
					event.setTarget(destination);
					networkMessage.setHasPassedFirstDelayBox(false);
					networkMessage.setHasPassedSecondDelayBox(false);
					networkNodeHasReceivedMessage(networkMessage, destination);
					
				} else if (networkMessage.hasPassedFirstDelayBox()) {
					
					event.setExecutionTime(Simulator.getNow() + destination.getDelayBox().getSendDelay(networkMessage.getLength()));
					event.setTarget(this);
					networkMessage.setHasPassedSecondDelayBox(true);
					networkNodeHasSentMessage(networkMessage, source);
					
				} else {
					
					event.setExecutionTime(Simulator.getNow() + source.getDelayBox().getSendDelay(networkMessage.getLength()));
					event.setTarget(this);
					networkMessage.setHasPassedFirstDelayBox(true);
					
				}
				
				if (!networkMessage.hasPassedFirstDelayBox() && !networkMessage.isDelivered()) {
					// TODO: hier übertragenes datenvolumen speichern für senderichtung bei sender
					//recordStatisticsMessageSent(networkMessage, source);
					event.setExecutionTime(Simulator.getNow() + source.getDelayBox().getSendDelay(networkMessage.getLength()));
					event.setTarget(this);
					networkMessage.setHasPassedFirstDelayBox(true);
					
				} else if (!networkMessage.hasPassedSecondDelayBox()) {
					
					// TODO: hier übertragenes datenvolumen speichern für senderichtung bei empfänger
					event.setExecutionTime(Simulator.getNow() + destination.getDelayBox().getSendDelay(networkMessage.getLength()));
					event.setTarget(this);
					networkMessage.setHasPassedFirstDelayBox(false); // TODO: noch nicht getestet!
					networkMessage.setHasPassedSecondDelayBox(true);
					
				} else { // isDelivered() == true
					
					event.setTarget(destination);
					networkMessage.setHasPassedSecondDelayBox(false);
					//recordStatisticsMessageReceived(networkMessage, source);
				}
				
			} else {
				
				if (!networkMessage.hasPassedFirstDelayBox()) {
					// TODO: hier übertragenes datenvolumen speichern für empfangsrichtung bei sender
					event.setExecutionTime(Simulator.getNow() + destination.getDelayBox().getReceiveDelay(networkMessage.getLength()));
					event.setTarget(this);
					networkMessage.setHasPassedFirstDelayBox(true);
				} else {
					// TODO: hier übertragenes datenvolumen speichern für empfangsrichtung bei empfänger
					event.setExecutionTime(Simulator.getNow() + source.getDelayBox().getReceiveDelay(networkMessage.getLength()));
					event.setTarget(source);
					networkMessage.setHasPassedFirstDelayBox(false); // TODO: noch nicht getestet!
				}
				
			}
			*/
			
			if (event.getAttachment() instanceof TransportMessage && !((TransportMessage)event.getAttachment()).isRequest() && event.getEventType() == DistantProxyEvent.INCOMING_REQUEST && event.getTarget() instanceof Mix) {
				System.out.println(event + " -- " +event.getAttachment()); 
				System.out.println(event.getTarget()); 
				System.out.println(source); 
				System.out.println(destination); 
			}
			simulator.scheduleEvent(event, this);
			
		}
		
	}
	
	
	private void networkNodeHasSentMessage(NetworkMessage messageSent, NetworkNode sender) {
		
		if (sender instanceof AbstractClient) {
			
			sender.getStatistics().increment(messageSent.getLength(), StatisticsType.SUM_CLIENT_DATAVOLUME_SEND);
			sender.getStatistics().increment(messageSent.getLength(), StatisticsType.SUM_CLIENT_DATAVOLUME_SENDANDRECEIVE);
			sender.getStatistics().increment(messageSent.getLength(), StatisticsType.SUM_CLIENT_DATAVOLUME_PER_SECOND_SEND);
			sender.getStatistics().increment(messageSent.getLength(), StatisticsType.SUM_CLIENT_DATAVOLUME_PER_SECOND_SENDANDRECEIVE);
			
			if (messageSent instanceof MixMessage) {
				
				sender.getStatistics().increment(((MixMessage)messageSent).getPayloadLength(), StatisticsType.SUM_CLIENT_PAYLOADVOLUME_SEND);
				sender.getStatistics().increment(((MixMessage)messageSent).getPayloadLength(), StatisticsType.SUM_CLIENT_PAYLOADVOLUME_SENDANDRECEIVE);
				sender.getStatistics().increment(((MixMessage)messageSent).getPayloadLength(), StatisticsType.SUM_CLIENT_PAYLOADVOLUME_PER_SECOND_AND_CLIENT_SEND);
				sender.getStatistics().increment(((MixMessage)messageSent).getPayloadLength(), StatisticsType.SUM_CLIENT_PAYLOADVOLUME_PER_SECOND_AND_CLIENT_SENDANDRECEIVE);
				sender.getStatistics().addValue(((MixMessage)messageSent).getPayloadPercentage(), StatisticsType.AVG_CLIENT_PAYLOADPERCENTAGE_SEND);
				sender.getStatistics().addValue(((MixMessage)messageSent).getPayloadPercentage(), StatisticsType.AVG_CLIENT_PAYLOADPERCENTAGE_SENDANDRECEIVE);
				sender.getStatistics().addValue(((MixMessage)messageSent).getPaddingPercentage(), StatisticsType.AVG_CLIENT_PADDINGPERCENTAGE_SEND);
				sender.getStatistics().addValue(((MixMessage)messageSent).getPaddingPercentage(), StatisticsType.AVG_CLIENT_PADDINGPERCENTAGE_SENDANDRECEIVE);
				sender.getStatistics().increment(((MixMessage)messageSent).getNumberOfMessagesContained(), StatisticsType.SUM_CLIENT_LAYER5MESSAGES_SENT);
				sender.getStatistics().increment(((MixMessage)messageSent).getNumberOfMessagesContained(), StatisticsType.SUM_CLIENT_LAYER5MESSAGES_SENTANDRECEIVED);
				sender.getStatistics().increment(1, StatisticsType.SUM_CLIENT_MIXMESSAGES_SENT);
				sender.getStatistics().increment(1, StatisticsType.SUM_CLIENT_MIXMESSAGES_SENTANDRECEIVED);
				
			} else if (messageSent instanceof TransportMessage) {
				
				sender.getStatistics().increment(1, StatisticsType.SUM_CLIENT_LAYER5MESSAGES_SENT);
				sender.getStatistics().increment(1, StatisticsType.SUM_CLIENT_LAYER5MESSAGES_SENTANDRECEIVED);
				
			}
			
		} else if (sender instanceof Mix) {
			
			sender.getStatistics().increment(messageSent.getLength(), StatisticsType.SUM_DATAVOLUME_PER_MIX_SEND);
			sender.getStatistics().increment(messageSent.getLength(), StatisticsType.SUM_DATAVOLUME_PER_MIX_SENDANDRECEIVE);
			
		} else if (sender instanceof DistantProxy) {
			
			sender.getStatistics().increment(messageSent.getLength(), StatisticsType.SUM_DISTANTPROXY_DATAVOLUME_SENDANDRECEIVE);
			
		}
			
	}
	
	
	private void networkNodeHasReceivedMessage(NetworkMessage messageReceived, NetworkNode receiver) {
		
		if (receiver instanceof AbstractClient) {
			
			receiver.getStatistics().increment(messageReceived.getLength(), StatisticsType.SUM_CLIENT_DATAVOLUME_RECEIVE);
			receiver.getStatistics().increment(messageReceived.getLength(), StatisticsType.SUM_CLIENT_DATAVOLUME_SENDANDRECEIVE);
			receiver.getStatistics().increment(messageReceived.getLength(), StatisticsType.SUM_CLIENT_DATAVOLUME_PER_SECOND_RECEIVE);
			receiver.getStatistics().increment(messageReceived.getLength(), StatisticsType.SUM_CLIENT_DATAVOLUME_PER_SECOND_SENDANDRECEIVE);
			
			if (messageReceived instanceof MixMessage) {
				
				receiver.getStatistics().increment(((MixMessage)messageReceived).getPayloadLength(), StatisticsType.SUM_CLIENT_PAYLOADVOLUME_RECEIVE);
				receiver.getStatistics().increment(((MixMessage)messageReceived).getPayloadLength(), StatisticsType.SUM_CLIENT_PAYLOADVOLUME_SENDANDRECEIVE);
				receiver.getStatistics().increment(((MixMessage)messageReceived).getPayloadLength(), StatisticsType.SUM_CLIENT_PAYLOADVOLUME_PER_SECOND_AND_CLIENT_RECEIVE);
				receiver.getStatistics().increment(((MixMessage)messageReceived).getPayloadLength(), StatisticsType.SUM_CLIENT_PAYLOADVOLUME_PER_SECOND_AND_CLIENT_SENDANDRECEIVE);
				receiver.getStatistics().addValue(((MixMessage)messageReceived).getPayloadPercentage(), StatisticsType.AVG_CLIENT_PAYLOADPERCENTAGE_RECEIVE);
				receiver.getStatistics().addValue(((MixMessage)messageReceived).getPayloadPercentage(), StatisticsType.AVG_CLIENT_PAYLOADPERCENTAGE_SENDANDRECEIVE);
				receiver.getStatistics().addValue(((MixMessage)messageReceived).getPaddingPercentage(), StatisticsType.AVG_CLIENT_PADDINGPERCENTAGE_RECEIVE);
				receiver.getStatistics().addValue(((MixMessage)messageReceived).getPaddingPercentage(), StatisticsType.AVG_CLIENT_PADDINGPERCENTAGE_SENDANDRECEIVE);
				receiver.getStatistics().increment(((MixMessage)messageReceived).getNumberOfMessagesContained(), StatisticsType.SUM_CLIENT_LAYER5MESSAGES_RECEIVED);
				receiver.getStatistics().increment(((MixMessage)messageReceived).getNumberOfMessagesContained(), StatisticsType.SUM_CLIENT_LAYER5MESSAGES_SENTANDRECEIVED);
				receiver.getStatistics().increment(1, StatisticsType.SUM_CLIENT_MIXMESSAGES_RECEIVED);
				receiver.getStatistics().increment(1, StatisticsType.SUM_CLIENT_MIXMESSAGES_SENTANDRECEIVED);
				
			} else if (messageReceived instanceof TransportMessage) {
				
				receiver.getStatistics().increment(1, StatisticsType.SUM_CLIENT_LAYER5MESSAGES_RECEIVED);
				
			}
			
		} else if (receiver instanceof Mix) {
			
			receiver.getStatistics().increment(messageReceived.getLength(), StatisticsType.SUM_DATAVOLUME_PER_MIX_RECEIVE);
			receiver.getStatistics().increment(messageReceived.getLength(), StatisticsType.SUM_DATAVOLUME_PER_MIX_SENDANDRECEIVE);
			
			if (((Mix)receiver).isFirstMix()) { // notify client about arrival
				
			}
		} else if (receiver instanceof DistantProxy) {
			
			receiver.getStatistics().increment(messageReceived.getLength(), StatisticsType.SUM_DISTANTPROXY_DATAVOLUME_SENDANDRECEIVE);
			
		}

	}
	

	/**
	 * @return the source
	 */
	public NetworkNode getSource() {
		return source;
	}


	/**
	 * @param source the source to set
	 */
	public void setSource(NetworkNode source) {
		this.source = source;
	}


	/**
	 * @return the destination
	 */
	public NetworkNode getDestination() {
		return destination;
	}


	/**
	 * @param destination the destination to set
	 */
	public void setDestination(NetworkNode destination) {
		this.destination = destination;
	}

	
	/**
	 * @return the simulator
	 */
	public Simulator getSimulator() {
		return simulator;
	}

	
	/**
	 * @param simulator the simulator to set
	 */
	public void setSimulator(Simulator simulator) {
		this.simulator = simulator;
	}


	@Override
	public int getGlobalId() {
		return numericIdentifier;
	}
}

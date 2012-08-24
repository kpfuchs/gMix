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


import evaluation.simulator.core.*;
import evaluation.simulator.message.*;
import evaluation.simulator.statistics.Statistics;
import evaluation.simulator.statistics.StatisticsType;


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
			
			if (event.getAttachment() instanceof NoneMixMessage && !((NoneMixMessage)event.getAttachment()).isRequest() && event.getEventType() == DistantProxyEvent.INCOMING_REQUEST && event.getTarget() instanceof Mix) {
				System.out.println(event + " -- " +event.getAttachment()); 
				System.out.println(event.getTarget()); 
				System.out.println(source); 
				System.out.println(destination); 
			}
			simulator.scheduleEvent(event, this);
			
		}
		
	}
	
	
	private void networkNodeHasSentMessage(NetworkMessage messageSent, NetworkNode sender) {
		
		if (sender instanceof Client) {
			
			sender.getStatistics().addValue(messageSent.getLength(), StatisticsType.CLIENT_DATAVOLUME_SEND);
			sender.getStatistics().addValue(messageSent.getLength(), StatisticsType.CLIENT_DATAVOLUME_SENDANDRECEIVE);
			sender.getStatistics().addValue(messageSent.getLength(), StatisticsType.CLIENT_DATAVOLUME_PER_SECOND_SEND);
			sender.getStatistics().addValue(messageSent.getLength(), StatisticsType.CLIENT_DATAVOLUME_PER_SECOND_SENDANDRECEIVE);
			
			if (messageSent instanceof MixMessage) {
				
				sender.getStatistics().addValue(((MixMessage)messageSent).getPayloadLength(), StatisticsType.CLIENT_PAYLOADVOLUME_SEND);
				sender.getStatistics().addValue(((MixMessage)messageSent).getPayloadLength(), StatisticsType.CLIENT_PAYLOADVOLUME_SENDANDRECEIVE);
				sender.getStatistics().addValue(((MixMessage)messageSent).getPayloadLength(), StatisticsType.CLIENT_PAYLOADVOLUME_PER_SECOND_SEND);
				sender.getStatistics().addValue(((MixMessage)messageSent).getPayloadLength(), StatisticsType.CLIENT_PAYLOADVOLUME_PER_SECOND_SENDANDRECEIVE);
				sender.getStatistics().addValue(((MixMessage)messageSent).getPayloadPercentage(), StatisticsType.CLIENT_PAYLOADPERCENTAGE_SEND);
				sender.getStatistics().addValue(((MixMessage)messageSent).getPayloadPercentage(), StatisticsType.CLIENT_PAYLOADPERCENTAGE_SENDANDRECEIVE);
				sender.getStatistics().addValue(((MixMessage)messageSent).getPaddingPercentage(), StatisticsType.CLIENT_PADDINGPERCENTAGE_SEND);
				sender.getStatistics().addValue(((MixMessage)messageSent).getPaddingPercentage(), StatisticsType.CLIENT_PADDINGPERCENTAGE_SENDANDRECEIVE);
				sender.getStatistics().addValue(((MixMessage)messageSent).getNumberOfMessagesContained(), StatisticsType.CLIENT_NONEMIXMESSAGES_SENT);
				sender.getStatistics().addValue(((MixMessage)messageSent).getNumberOfMessagesContained(), StatisticsType.CLIENT_NONEMIXMESSAGES_SENTANDRECEIVED);
				sender.getStatistics().addValue(1, StatisticsType.CLIENT_MIXMESSAGES_SENT);
				sender.getStatistics().addValue(1, StatisticsType.CLIENT_MIXMESSAGES_SENTANDRECEIVED);
				
			} else if (messageSent instanceof NoneMixMessage) {
				
				sender.getStatistics().addValue(1, StatisticsType.CLIENT_NONEMIXMESSAGES_SENT);
				sender.getStatistics().addValue(1, StatisticsType.CLIENT_NONEMIXMESSAGES_SENTANDRECEIVED);
				
			}
			
		} else if (sender instanceof Mix) {
			
			sender.getStatistics().addValue(messageSent.getLength(), StatisticsType.MIX_DATAVOLUME_SEND);
			sender.getStatistics().addValue(messageSent.getLength(), StatisticsType.MIX_DATAVOLUME_SENDANDRECEIVE);
			sender.getStatistics().addValue(1, StatisticsType.MIX_MIXMESSAGES_SENT);
			
		} else if (sender instanceof DistantProxy) {
			
			sender.getStatistics().addValue(messageSent.getLength(), StatisticsType.DISTANTPROXY_DATAVOLUME_SENDANDRECEIVE);
			
		}
			
	}
	
	
	private void networkNodeHasReceivedMessage(NetworkMessage messageReceived, NetworkNode receiver) {
		
		if (receiver instanceof Client) {
			
			receiver.getStatistics().addValue(messageReceived.getLength(), StatisticsType.CLIENT_DATAVOLUME_RECEIVE);
			receiver.getStatistics().addValue(messageReceived.getLength(), StatisticsType.CLIENT_DATAVOLUME_SENDANDRECEIVE);
			receiver.getStatistics().addValue(messageReceived.getLength(), StatisticsType.CLIENT_DATAVOLUME_PER_SECOND_RECEIVE);
			receiver.getStatistics().addValue(messageReceived.getLength(), StatisticsType.CLIENT_DATAVOLUME_PER_SECOND_SENDANDRECEIVE);
			
			if (messageReceived instanceof MixMessage) {
				
				receiver.getStatistics().addValue(((MixMessage)messageReceived).getPayloadLength(), StatisticsType.CLIENT_PAYLOADVOLUME_RECEIVE);
				receiver.getStatistics().addValue(((MixMessage)messageReceived).getPayloadLength(), StatisticsType.CLIENT_PAYLOADVOLUME_SENDANDRECEIVE);
				receiver.getStatistics().addValue(((MixMessage)messageReceived).getPayloadLength(), StatisticsType.CLIENT_PAYLOADVOLUME_PER_SECOND_RECEIVE);
				receiver.getStatistics().addValue(((MixMessage)messageReceived).getPayloadLength(), StatisticsType.CLIENT_PAYLOADVOLUME_PER_SECOND_SENDANDRECEIVE);
				receiver.getStatistics().addValue(((MixMessage)messageReceived).getPayloadPercentage(), StatisticsType.CLIENT_PAYLOADPERCENTAGE_RECEIVE);
				receiver.getStatistics().addValue(((MixMessage)messageReceived).getPayloadPercentage(), StatisticsType.CLIENT_PAYLOADPERCENTAGE_SENDANDRECEIVE);
				receiver.getStatistics().addValue(((MixMessage)messageReceived).getPaddingPercentage(), StatisticsType.CLIENT_PADDINGPERCENTAGE_RECEIVE);
				receiver.getStatistics().addValue(((MixMessage)messageReceived).getPaddingPercentage(), StatisticsType.CLIENT_PADDINGPERCENTAGE_SENDANDRECEIVE);
				receiver.getStatistics().addValue(((MixMessage)messageReceived).getNumberOfMessagesContained(), StatisticsType.CLIENT_NONEMIXMESSAGES_RECEIVED);
				receiver.getStatistics().addValue(((MixMessage)messageReceived).getNumberOfMessagesContained(), StatisticsType.CLIENT_NONEMIXMESSAGES_SENTANDRECEIVED);
				receiver.getStatistics().addValue(1, StatisticsType.CLIENT_MIXMESSAGES_RECEIVED);
				receiver.getStatistics().addValue(1, StatisticsType.CLIENT_MIXMESSAGES_SENTANDRECEIVED);
				
			} else if (messageReceived instanceof NoneMixMessage) {
				
				receiver.getStatistics().addValue(1, StatisticsType.CLIENT_NONEMIXMESSAGES_RECEIVED);
				
			}
			
		} else if (receiver instanceof Mix) {
			
			receiver.getStatistics().addValue(messageReceived.getLength(), StatisticsType.MIX_DATAVOLUME_RECEIVE);
			receiver.getStatistics().addValue(messageReceived.getLength(), StatisticsType.MIX_DATAVOLUME_SENDANDRECEIVE);
			
		} else if (receiver instanceof DistantProxy) {
			
			receiver.getStatistics().addValue(messageReceived.getLength(), StatisticsType.DISTANTPROXY_DATAVOLUME_SENDANDRECEIVE);
			
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

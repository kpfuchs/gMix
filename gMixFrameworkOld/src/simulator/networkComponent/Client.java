/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2011  Karl-Peter Fuchs
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

package simulator.networkComponent;


import simulator.communicationBehaviour.ClientCommunicationBehaviour;
import simulator.core.ClientEvent;
import simulator.core.DistantProxyEvent;
import simulator.core.Event;
import simulator.core.MixEvent;
import simulator.core.Settings;
import simulator.core.Simulator;
import simulator.delayBox.DelayBox;
import simulator.message.MixMessage;
import simulator.message.NetworkMessage;
import simulator.message.NoneMixMessage;
import simulator.statistics.StatisticsType;


public class Client extends NetworkNode {
	
	protected Simulator simulator;
	protected ClientCommunicationBehaviour clientCommunicationBehaviour;
	private int messageCreationTime;
	private int messageDecryptionTime;
	private int clientId;
	private static int idCounter = 0;
	
	
	public Client(	String identifier, 
					Simulator simulator, 
					DelayBox delayBox 
					) {
		
		super(identifier, simulator, delayBox);
		this.simulator = simulator;
		this.clientCommunicationBehaviour = ClientCommunicationBehaviour.getInstance(this, simulator);
		this.messageCreationTime = new Integer(Settings.getProperty("MIX_REQUEST_CREATION_TIME")); // in ms
		this.messageDecryptionTime = new Integer(Settings.getProperty("MIX_REPLY_DECRYPTION_TIME")); // in ms
		//this.timeToWaitForFurtherRequests = new Integer(Settings.getProperty("TIME_TO_WAIT_FOR_FURTHER_REQUESTS")); // in ms
		this.clientId = idCounter++;
		
	}
	
	
	public MixMessage generateMixRequest() {
		return MixMessage.getInstance(true, this, simulator.getDistantProxy(), this, Simulator.getNow(), false);
	}

	
	public int getMixRequestEncryptionTime() {
		return messageCreationTime;
	}
	
	
	public int getMixReplyDecryptionTime() {
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
					if (Settings.getProperty("SIMULATE_REQUEST_CHANNEL").equals("FALSE"))
						simulator.getDistantProxy().incomingRequest((NoneMixMessage) event.getAttachment());
					else 
						clientCommunicationBehaviour.incomingRequestFromUser((NoneMixMessage)event.getAttachment());
					break;
					
				case REPLY_FROM_MIX:
					event.reuse(this, Simulator.getNow() + getMixReplyDecryptionTime(), ClientEvent.REPLY_DECRYPTED);
					simulator.scheduleEvent(event, this);
					break;
					
				case REPLY_DECRYPTED:
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
		
		if (networkMessage instanceof NoneMixMessage) {
			
			NoneMixMessage nmm = (NoneMixMessage)networkMessage;
			statistics.addValue(Simulator.getNow() - nmm.getCreationTime(), StatisticsType.CLIENT_RTT_NONEMIXMESSAGE);
			clientCommunicationBehaviour.incomingDecryptedReply(nmm);
			
		} else if (networkMessage instanceof MixMessage) {
			MixMessage mixMessage = (MixMessage)networkMessage;
			statistics.addValue(Simulator.getNow() - mixMessage.getCreationTime(), StatisticsType.CLIENT_LATENCY_REPLYMIXMESSAGE);
			
			if (!mixMessage.isDummy())
				for (NoneMixMessage nmm: mixMessage.getNoneMixMessagesContained())
					statistics.addValue(Simulator.getNow() - nmm.getCreationTime(), StatisticsType.CLIENT_RTT_NONEMIXMESSAGE);

			clientCommunicationBehaviour.incomingDecryptedReply(mixMessage);
			
		} else 
			throw new RuntimeException("ERROR: unknown MESSAGE_FORMAT! " +networkMessage);

	}
	
	
	// called by associated ClientCommunicationBehaviour (hides implementation-details)
	public void sendRequest(NetworkMessage networkMessage) {
		
		if (networkMessage instanceof MixMessage)
			sendToNextHop(networkMessage, getMixRequestEncryptionTime(), MixEvent.INCOMING_MIX_MESSAGE_OF_TYPE_REQUEST);
		else if (networkMessage instanceof NoneMixMessage)
			sendToNextHop(networkMessage, getMixRequestEncryptionTime(), DistantProxyEvent.INCOMING_REQUEST);
		else
			throw new RuntimeException("ERROR: Client does not support messages of type " +networkMessage +"!"); 
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

	
	public ClientCommunicationBehaviour getClientCommunicationBehaviour() {
		return this.clientCommunicationBehaviour;
	}
	

	public int getClientId() {
		return clientId;
	}
	
	
	public static int getNumberOfClients() {
		return idCounter;
	}
	
	
	public static void reset() {
		idCounter = 0;
	}
	
}

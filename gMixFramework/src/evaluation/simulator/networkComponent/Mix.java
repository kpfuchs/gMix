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

package evaluation.simulator.networkComponent;

import evaluation.simulator.core.*;
import evaluation.simulator.delayBox.DelayBox;
import evaluation.simulator.message.*;
import evaluation.simulator.outputStrategy.OutputStrategy;
import evaluation.simulator.statistics.StatisticsType;


public class Mix extends NetworkNode {

	private boolean isFirstMix;
	private boolean isLastMix;
	protected Simulator simulator;
	//protected LastMixCommunicationBehaviour lastMixCommunicationBehaviour;
	private OutputStrategy outputStrategy;
	private static double PROCESSING_TIME_FOR_1000_REQUESTS;
	private static double PROCESSING_TIME_FOR_1000_REPLIES;
	private static int requestProcessingTime;
	private static int replyProcessingTime;
	
	
	
	public Mix(String identifier, Simulator simulator, DelayBox delayBox, boolean isFirstMix, boolean isLastMix) {
		
		super(identifier, simulator, delayBox);
		this.isFirstMix = isFirstMix;
		this.isLastMix = isLastMix;
		this.simulator = simulator;
		
		PROCESSING_TIME_FOR_1000_REQUESTS = Simulator.settings.getPropertyAsDouble("PROCESSING_TIME_FOR_1000_REQUESTS"); // ms
		PROCESSING_TIME_FOR_1000_REPLIES = Simulator.settings.getPropertyAsDouble("PROCESSING_TIME_FOR_1000_REPLIES"); // ms
		requestProcessingTime = (int)Math.round(PROCESSING_TIME_FOR_1000_REQUESTS / 1000d);
		replyProcessingTime = (int)Math.round(PROCESSING_TIME_FOR_1000_REPLIES / 1000d);
		
		outputStrategy = OutputStrategy.getInstance(this, simulator);

		//if (isLastMix)
		//	lastMixCommunicationBehaviour = LastMixCommunicationBehaviour.getInstance(this, simulator);
		
	}
	
	
	public void putOutRequest(NetworkMessage networkMessage) {
		
		if(!networkMessage.isRequest())
			throw new RuntimeException("ERROR: Received reply while expecting request");
		
		if (networkMessage instanceof NoneMixMessage) {
			
			sendToNextHop(networkMessage, 0, DistantProxyEvent.INCOMING_REQUEST);
			
		} else if (networkMessage instanceof MixMessage) {
			
			if (!isLastMix) {
				
				sendToNextHop(networkMessage, 0, MixEvent.INCOMING_MIX_MESSAGE_OF_TYPE_REQUEST);
			
			} else {
				
				statistics.addValue(Simulator.getNow() - ((MixMessage)networkMessage).getCreationTime(), StatisticsType.CLIENT_LATENCY_REQUESTMIXMESSAGE);
				
				if (simulator.getDistantProxy().supportsMixMessages())
					sendToNextHop(networkMessage, 0, DistantProxyEvent.INCOMING_REQUEST);
				else
					for (NoneMixMessage nmm: ((MixMessage)networkMessage).getNoneMixMessagesContained())
						sendToNextHop(nmm, 0, DistantProxyEvent.INCOMING_REQUEST);
			}
			
		} else 
			throw new RuntimeException("ERROR: unknown message type: " +networkMessage);

	}
	
	
	public void putOutReply(MixMessage mixMessage) {
		
		if(mixMessage.isRequest())
			throw new RuntimeException("ERROR: Received request while expecting reply");

		if (isFirstMix)
			sendToPreviousHop(mixMessage, 0, ClientEvent.REPLY_FROM_MIX);
		else
			sendToPreviousHop(mixMessage, 0, MixEvent.INCOMING_MIX_MESSAGE_OF_TYPE_REPLY);

	}
	
	
	@Override
	public void executeEvent(Event event) {
		
		if (!(event.getEventType() instanceof MixEvent)) {
			
			throw new RuntimeException("ERROR: " +super.getIdentifier() +" received wrong Event: "+event +" "+event.getAttachment()); 
			
		} else {				
				
			switch ((MixEvent)event.getEventType()) {
			
				case INCOMING_MIX_MESSAGE_OF_TYPE_REQUEST:
					simulator.scheduleEvent(event.reuse(this, (Simulator.getNow() + getMixRequestProcessingTime()), MixEvent.INCOMING_PROCESSED_REQUEST, event.getAttachment()), this);
					break;
				
				case INCOMING_PROCESSED_REQUEST:
					outputStrategy.incomingRequest((MixMessage)event.getAttachment());
					break;
				
				case INCOMING_MIX_MESSAGE_OF_TYPE_REPLY:
					simulator.scheduleEvent(event.reuse(this, (Simulator.getNow() + getMixReplyProcessingTime()), MixEvent.INCOMING_PROCESSED_REPLY, event.getAttachment()), this);
					break;
				
				case INCOMING_PROCESSED_REPLY:
					outputStrategy.incomingReply((MixMessage)event.getAttachment());
					break;
				
				case INCOMING_REPLY_FROM_DISTANT_PROXY:
					outputStrategy.incomingReply((NoneMixMessage)event.getAttachment());
					break;
					
			}
			
		}

	}

	
	/**
	 * @param isFirstMix the isFirstMix to set
	 */
	public void setFirstMix(boolean isFirstMix) {
		this.isFirstMix = isFirstMix;
	}


	/**
	 * @param isLastMix the isLastMix to set
	 */
	public void setLastMix(boolean isLastMix) {
		this.isLastMix = isLastMix;
	}


	/**
	 * @return the isFirstMix
	 */
	public boolean isFirstMix() {
		return isFirstMix;
	}


	/**
	 * @return the isLastMix
	 */
	public boolean isLastMix() {
		return isLastMix;
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
	
	
	/**
	 * @return the outputStrategy
	 */
	public OutputStrategy getOutputStrategy() {
		return outputStrategy;
	}
	
	
	public int getMixRequestProcessingTime() {
		return requestProcessingTime;
	}

	
	public int getMixReplyProcessingTime() {
		return replyProcessingTime;
	}
	
}

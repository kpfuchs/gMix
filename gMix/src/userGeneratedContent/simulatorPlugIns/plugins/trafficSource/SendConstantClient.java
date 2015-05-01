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
package userGeneratedContent.simulatorPlugIns.plugins.trafficSource;

import java.security.SecureRandom;

import org.apache.commons.math.random.RandomDataImpl;

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.annotations.plugin.Plugin;
import staticContent.evaluation.simulator.annotations.property.IntSimulationProperty;
import staticContent.evaluation.simulator.core.event.Event;
import staticContent.evaluation.simulator.core.event.SimulationEvent;
import staticContent.evaluation.simulator.core.message.EndToEndMessage;
import staticContent.evaluation.simulator.core.networkComponent.AbstractClient;
import staticContent.evaluation.traceParser.engine.dataStructure.ExtendedTransaction;

@Plugin(pluginKey = "CONSTANT",  pluginName="Constant")
public class SendConstantClient extends AbstractClient {

	@IntSimulationProperty( name = "Request size (byte)",
			key = "CONSTANT_REQUEST_SIZE",
			position = 3, 
			enableAuto = true,
			min = 0,
			max = 9000)
	private int REQUEST_SIZE;
	
	@IntSimulationProperty( name = "Reply size (byte)",
			key = "CONSTANT_REPLY_SIZE",
			position = 4, 
			enableAuto = true,
			min = 0,
			max = 9000)
	private int REPLY_SIZE;
	
	@IntSimulationProperty( name = "Resolve time (ms)",
			key = "CONSTANT_RESOLVE_TIME",
			position = 5,
			min = 0)
	private int RESOLVE_TIME; // in ms
	
	@IntSimulationProperty( name = "Requests per second (requests)",
			key = "CONSTANT_AVERAGE_REQUESTS_PER_SECOND_AND_CLIENT", 
			min = 1,
			position = 2 )
	private long TIME_BETWEEN_SENDS;
	
	private RandomDataImpl randomDataImpl;
	private static SecureRandom secureRandom = new SecureRandom();
	
	
	public SendConstantClient(String identifier, Simulator simulator, int clientId) {
		super(identifier, simulator);

		if (Simulator.settings.getProperty("CONSTANT_REQUEST_SIZE").equals("AUTO"))
			this.REQUEST_SIZE = Simulator.settings.getPropertyAsInt("MIX_REQUEST_PAYLOAD_SIZE");
		else
			this.REQUEST_SIZE = Simulator.settings.getPropertyAsInt("CONSTANT_REQUEST_SIZE");

		if (Simulator.settings.getProperty("CONSTANT_REPLY_SIZE").equals("AUTO"))
			this.REPLY_SIZE = Simulator.settings.getPropertyAsInt("MIX_REPLY_PAYLOAD_SIZE");
		else
			this.REPLY_SIZE = Simulator.settings.getPropertyAsInt("CONSTANT_REPLY_SIZE");
		this.RESOLVE_TIME = Simulator.settings.getPropertyAsInt("CONSTANT_RESOLVE_TIME");
		this.clientId = clientId;
		this.TIME_BETWEEN_SENDS = Math.round(1000d/Simulator.settings.getPropertyAsDouble("CONSTANT_AVERAGE_REQUESTS_PER_SECOND_AND_CLIENT"));
		if (TIME_BETWEEN_SENDS == 0)
			throw new RuntimeException("ERROR: TIME_BETWEEN_SENDS in experiment config files must be 1000 at max."); 
		this.randomDataImpl = new RandomDataImpl();
		this.randomDataImpl.reSeed(secureRandom.nextLong());
	}

	
	public void startSending() {
		long whenToSendFirstMessage = (long) randomDataImpl.nextUniform(Simulator.getNow(), Simulator.getNow() + TIME_BETWEEN_SENDS);
		simulator.scheduleEvent(new Event(this, whenToSendFirstMessage, SendConstantClientEvent.SEND_MESSAGE), this);	
	}

	
	@Override
	public void incomingMessage(EndToEndMessage message) {
		// SendConstantClient does not take feedback into account...
	}

	
	@Override
	public void messageReachedServer(EndToEndMessage message) {
		// SendConstantClient does not take feedback into account...
	}
	
	
	private void sendMessage() {
		ExtendedTransaction et = new ExtendedTransaction(0, 0l, 0l, REQUEST_SIZE, 0, new long[] {0}, new long[] {(0+RESOLVE_TIME)}, new int[] {REPLY_SIZE});
		EndToEndMessage eteMessage = new EndToEndMessage(0, et, true);
		sendMessage(eteMessage);
		long whenToSendNextMessage = Simulator.getNow() + TIME_BETWEEN_SENDS;
		simulator.scheduleEvent(new Event(this, whenToSendNextMessage, SendConstantClientEvent.SEND_MESSAGE), this);	
	}

	
	public enum SendConstantClientEvent implements SimulationEvent {
		SEND_MESSAGE;
	}
	
	
	@Override
	public void executeEvent(Event event) {
		if (event.getEventType() instanceof SendConstantClientEvent) {
			if (event.getEventType() == SendConstantClientEvent.SEND_MESSAGE) {
				sendMessage();
			} else
				throw new RuntimeException("ERROR: received unknown Event: " +event.toString()); 
		} else {
			super.executeEvent(event);
		}
	}


	@Override
	public void close() {
		// nothing to do here
	}

}
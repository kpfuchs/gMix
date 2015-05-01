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

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.annotations.plugin.Plugin;
import staticContent.evaluation.simulator.annotations.property.IntSimulationProperty;
import staticContent.evaluation.simulator.core.message.EndToEndMessage;
import staticContent.evaluation.simulator.core.networkComponent.AbstractClient;
import staticContent.evaluation.traceParser.engine.dataStructure.ExtendedTransaction;

@Plugin(pluginKey = "REQUEST_REPLY", pluginName="Request Reply")
public class RequestReplyClient extends AbstractClient {

	@IntSimulationProperty( name = "Request size (byte)", 
			key = "REQUEST_REPLY_REQUEST_SIZE", 
			enableAuto = true,
			min = 0,
			max = 9000)
	private int REQUEST_SIZE;
	
	@IntSimulationProperty( name = "Reply size (byte)",
			key = "REQUEST_REPLY_REPLY_SIZE", 
			enableAuto = true,
			min = 0,
			max = 9000)
	private int REPLY_SIZE;
	
	@IntSimulationProperty( name = "Resolve time (ms)", 
			key = "REQUEST_REPLY_RESOLVE_TIME",
			min = 0)
	private int RESOLVE_TIME; // in ms
	
	
	public RequestReplyClient(String identifier, Simulator simulator, int clientId) {
		super(identifier, simulator);
		if (Simulator.settings.getProperty("REQUEST_REPLY_REQUEST_SIZE").equals("AUTO"))
			this.REQUEST_SIZE = Simulator.settings.getPropertyAsInt("MIX_REQUEST_PAYLOAD_SIZE");
		else
			this.REQUEST_SIZE = Simulator.settings.getPropertyAsInt("REQUEST_REPLY_REQUEST_SIZE");
		if (Simulator.settings.getProperty("REQUEST_REPLY_REPLY_SIZE").equals("AUTO"))
			this.REPLY_SIZE = Simulator.settings.getPropertyAsInt("MIX_REPLY_PAYLOAD_SIZE");
		else
			this.REPLY_SIZE = Simulator.settings.getPropertyAsInt("REQUEST_REPLY_REPLY_SIZE");
		this.RESOLVE_TIME = Simulator.settings.getPropertyAsInt("REQUEST_REPLY_RESOLVE_TIME");
		this.clientId = clientId;
	}

	
	public void startSending() {
		System.out.println("start"); 
		sendNextMessage();
	}

	
	@Override
	public void incomingMessage(EndToEndMessage message) {
		System.out.println(Simulator.getNow()); 
		sendNextMessage();
	}

	
	@Override
	public void messageReachedServer(EndToEndMessage message) {
		// RequestReplyClient uses arrival of reply as feedback...
	}
	
	
	private void sendNextMessage() {
		ExtendedTransaction et = new ExtendedTransaction(0, 0l, 0l, REQUEST_SIZE, 0, new long[] {0}, new long[] {(0+RESOLVE_TIME)}, new int[] {REPLY_SIZE});
		EndToEndMessage eteMessage = new EndToEndMessage(0, et, true);
		sendMessage(eteMessage);
	}
	
	
	@Override
	public void close() {
		// nothing to do here
	}
	
}
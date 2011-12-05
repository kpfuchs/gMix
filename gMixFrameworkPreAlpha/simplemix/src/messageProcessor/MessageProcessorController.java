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

package messageProcessor;

import framework.Controller;
import framework.LocalClassLoader;
import framework.Mix;
import inputOutputHandler.InputOutputHandlerController;
import java.security.KeyPair;
import architectureInterface.MessageProcessorInterface;
import outputStrategy.OutputStrategyController;


/**
 * Controller class of component <code>MessageProcessor</code>. 
 * <p>
 * Takes messages from component <code>InputOutputHandler</code>, processes 
 * them (recoding, checking for replays, removing/adding padding, 
 * initiating message authentication) and bypasses them to component
 * <code>OutputStrategy</code>.
 * <p>
 * Can handle <code>Request</code>s and <code>Replies</code> in parallel.
 * <p>
 * The functions mentioned above can be performed in parallel as well, except 
 * for detecting replays (for security reasons).
 * 
 * @author Karl-Peter Fuchs
 */
public class MessageProcessorController extends Controller implements MessageProcessorInterface {

	private MessageProcessorInterface messageProcessorImplementation;
	
	
	/**
	 * Generates a new <code>MessageProcessor</code> component, which takes 
	 * messages from component <code>InputOutputHandler</code>, processes 
	 * them (recoding, checking for replays, removing/adding padding, 
	 * initiating message authentication) and bypasses them to the 
	 * <code>OutputStrategy</code> component.
	 * <p>
	 * Can handle <code>Request</code>s and <code>Replies</code> in parallel.
	 * <p>
	 * The functions mentioned above can be performed in parallel as well, 
	 * except for detecting replays (for security reasons).
	 * <p>
	 * Component can't be used before calling <code>initialize()</code>.
	 * 
	 * @see #initialize(KeyPair)
	 * 
	 * @see #initialize(	KeyPair, 
	 * 						InputOutputHandlerController, 
	 * 						AccessControlController, 
	 * 						OutputStrategyController
	 * 						)
	 */
	public MessageProcessorController(Mix mix) {
		super(mix);
	}
	
	
	@Override
	public void instantiateSubclass() {
		this.messageProcessorImplementation = LocalClassLoader.instantiateMessageProcessorImplementation(this);
	}


	//initialize with a new keypair
	@Override
	public void initialize(KeyPair keyPair) {
		this.messageProcessorImplementation.initialize(keyPair);
	}
	
}

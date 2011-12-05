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


import inputOutputHandler.InputOutputHandlerController;

import internalInformationPort.InternalInformationPortController;

import java.security.KeyPair;

import outputStrategy.OutputStrategyController;

import accessControl.AccessControlController;

import message.ChannelEstablishMessage;
import message.Message;
import message.Reply;
import message.Request;


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
public class MessageProcessorController {

	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** 
	 * Reference on component <code>InputOutputHandler</code> (used to get 
	 * unprocessed messages from).
	 */
	private InputOutputHandlerController inputOutputHandler;
	
	/** 
	 * Reference on component <code>AccessControl</code> (used to check if 
	 * messages have integrity).
	 */
	private AccessControlController accessControl;
	
	/** 
	 * Reference on component <code>OutputStrategy</code> (used to hand over 
	 * processed messages).
	 */
	private OutputStrategyController outputStrategy;
	
	/** 
	 * Reference on <code>Recoder</code>, used to decrypt data.
	 * 
	 *  @see #decrypt(byte[], String)
	 *  @see #initialize(KeyPair)
	 */
	private Recoder recoder;
	
	
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
	public MessageProcessorController() {
		
	}
	
	
	/**
	 * Initialization method for this component. Makes this component process 
	 * messages (= take messages from component 
	 * <code>InputOutputHandler</code>, processes them (recoding, checking for 
	 * replays, removing/adding padding, initiating message authentication) 
	 * and bypass them to the <code>OutputStrategy</code> component.
	 * 
	 * @param keyPair					Reference on this mix' 
	 * 									<code>KeyPair</code>. Used for 
	 * 									asymmetric cryptography.
	 * @param inputOutputHandler		Reference on component 
	 * 									<code>InputOutputHandler</code> (used 
	 * 									to get unprocessed messages from).
	 * @param accessControl				Reference on component 
	 * 									<code>AccessControl</code> (used to 
	 * 									check if messages have integrity).
	 * @param outputStrategy			Reference on component 
	 * 									<code>OutputStrategy</code> (used to 
	 * 									hand over processed messages).
	 * 
	 * @see #initialize(KeyPair)
	 */
	public void initialize(
				KeyPair keyPair, 
				InputOutputHandlerController inputOutputHandler,
				AccessControlController accessControl,
				OutputStrategyController outputStrategy
				) {
		
		this.inputOutputHandler = inputOutputHandler;
		this.accessControl = accessControl;
		this.outputStrategy = outputStrategy;
		
		ReplayDetection replayDetection = new ReplayDetection();
		
		for (int i=0; i<getNumberOfThreads(); i++) {

			new RequestMixThread(new Recoder(keyPair), replayDetection);
			/* 
			 * Note: Each Thread gets its own "Recoder". Therefore, recoding is 
			 * performed in parallel. In contrast, both Threads get (a reference
			 * on) the same "ReplayDetection", since detecting replays can't be 
			 * parallelized. 
			 */
			
			new ReplyMixThread(new Recoder());
			
		}
		
	}
	
	
	/**
	 * Initialization method for this component. Should be called, when 
	 * component shall solely be used to decrypt data and not process messages 
	 * (see <code>initialize(KeyPair, InternalInformationPortController, 
	 * InputOutputHandlerController, AccessControlController, 
	 * OutputStrategyController)</code> for an initializer causing this 
	 * component to process messages, too).
	 * 
	 * @param keyPair			Reference on this mix' <code>KeyPair</code>. 
	 * 							Used to decrypt data.
	 * 
	 * @see #initialize(	KeyPair,
	 * 						InputOutputHandlerController, 
	 *						AccessControlController, 
	 *						OutputStrategyController)
	 * 						
	 */
	public void initialize(KeyPair keyPair) {
		
		this.recoder = new Recoder(keyPair);
		
	}
	
	
	/**
	 * Returns the number of message that shall be processed in parallel 
	 * (as specified in property file).
	 * 
	 * @return Number of message that shall be processed in parallel.
	 */
	private int getNumberOfThreads() {
		
		int numberOfThreads = 
			new Integer(internalInformationPort.getProperty(
					"NUMBER_OF_THREADS")
				);
		
		// -1 means "automatic detection"
		return	(numberOfThreads == -1)
				?  Runtime.getRuntime().availableProcessors()
				: numberOfThreads;
								 
	}
	
	
	/**
	 * Decrypts the bypassed data using the internal asymmetric cipher (and 
	 * private key) and the specified transformation. 
	 * 
	 * @param data				Data to be decrypted.
	 * @param transformation	Transformation that shall be used for 
	 * 							decryption.
	 * @return					Decrypted data.
	 * 
	 * @throws Exception	Any type of error preventing the data from being 
	 * 						decrypted.
	 */
	public byte[] decrypt(byte[] data, String transformation) throws Exception {
		
		return recoder.decrypt(data, transformation);
		
	}
	
	
	/**
	 * Thread, which coordinates the mixing process of request. Can be 
	 * instantiated several times, to increase mix performance on systems with 
	 * several processing units.
	 * <p>
	 * Included functionality:
	 * <ul>
	 * <li> taking (unprocessed) requests from <code>InputOutputHandler</code>
	 * <li> decrypting requests
	 * <li> validating integrity
	 * <li> detecting replays
	 * <li> passing (the now processed) requests to the 
	 * <code>OutputStrategy</code> component.
	 * </ul>
	 * <p>
	 * See <code>ReplyMixThread</code> for the thread coordinating replies.
	 * 
	 * @author Karl-Peter Fuchs
	 */
	private final class RequestMixThread extends Thread {
		
		/** The <code>Recoder</code> used to decrypt messages. */
		private Recoder recoder;
		
		/** <code>ReplayDetection</code> used to detect replays. */
		private ReplayDetection replayDetection;
		
		
		/**
		 * Saves references on the bypassed objects and calls <code>start()
		 * </code>.
		 * 
		 * @param recoder			The <code>Recoder</code> that shall be used 
		 * 							to decrypt messages.
		 * @param replayDetection	<code>ReplayDetection</code> used to 
		 * 							detect replays.
		 */
		private RequestMixThread(	Recoder recoder, 
									ReplayDetection replayDetection
									) {

			this.recoder = recoder;
			this.replayDetection = replayDetection;
			start();
			
		}


		/**
		 * Coordinates the mixing process of request.
		 * <p>
		 * Included functionality:
		 * <ul>
		 * <li> taking (unprocessed) requests from <code>InputOutputHandler</code>
		 * <li> decrypting requests
		 * <li> validating integrity
		 * <li> detecting replays
		 * <li> passing (the now processed) requests to the 
		 * <code>OutputStrategy</code> component.
		 * </ul>
		 */
		@Override
		public void run() {
			
			while (true) { // process messages

				Request request = inputOutputHandler.getRequest();
					// blocks until request is available
				
				request = (Request)recoder.recode(request);

				if (	request != null 
						&& 
						accessControl.isMACCorrect((Message)request)
						) {
						
					if (request instanceof ChannelEstablishMessage) {
						
						if(replayDetection.isReplay(request)) {
							
							continue;
								
						}
						
					}
					
					outputStrategy.addRequest(request);
						
				}

			}
			
		}
		
	}
	
	
	/**
	 * Thread, which coordinates the mixing process of replies. Can be 
	 * instantiated several times, to increase mix performance on systems with 
	 * several processing units.
	 * <p>
	 * Included functionality:
	 * <ul>
	 * <li> taking (unprocessed) replies from <code>InputOutputHandler</code>
	 * <li> encrypting replies
	 * <li> passing (the now processed) replies to the 
	 * <code>OutputStrategy</code> component.
	 * </ul>
	 * <p>
	 * See <code>RequestMixThread</code> for the thread coordinating requests.
	 * 
	 * @author Karl-Peter Fuchs
	 */
	private final class ReplyMixThread extends Thread {
		
		/** The <code>Recoder</code> used to encrypt messages. */
		private Recoder recoder;
		
		
		/**
		 * Saves references on the bypassed object and calls <code>start()
		 * </code>.
		 * 
		 * @param recoder	The <code>Recoder</code> that shall be used to 
		 * 					encrypt messages.
		 */
		private ReplyMixThread(Recoder recoder) {

			this.recoder = recoder;
			start();
			
		}


		/**
		 * Coordinates the mixing process of replies.
		 * <p>
		 * Included functionality:
		 * <ul>
		 * <li> taking (unprocessed) replies from 
		 * <code>InputOutputHandler</code>
		 * <li> encrypting replies
		 * <li> passing (the now processed) replies to the 
		 * <code>OutputStrategy</code> component.
		 * </ul>
		 * <p>
		 */
		@Override
		public void run() {
			
			while (true) { // process messages

				Reply reply = inputOutputHandler.getReply();
					// blocks until reply is available
				
				reply = (Reply) recoder.recode(reply);
				outputStrategy.addReply(reply);
				
			}
			
		}
		
	}
	
}

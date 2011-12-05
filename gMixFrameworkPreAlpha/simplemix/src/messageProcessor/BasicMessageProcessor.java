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

import java.security.KeyPair;
import framework.Implementation;
import framework.Settings;
import message.Reply;
import message.Request;
import architectureInterface.MessageProcessorInterface;


public class BasicMessageProcessor extends Implementation implements MessageProcessorInterface {

	private KeyPair keyPair;

	@Override
	public void initialize() {
		keyPair = mix.getKeyPair();
	}

	
	public void initialize(KeyPair keyPair) {
		this.keyPair = keyPair;
	}
	

	@Override
	public void begin() {
		
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
	
	
	@Override
	public String[] getCompatibleImplementations() {
		return (new String[] {	"outputStrategy.BasicBatch",
								"outputStrategy.BasicPool",
								"inputOutputHandler.BasicInputOutputHandler",
								"keyGenerator.BasicKeyGenerator",
								"messageProcessor.BasicMessageProcessor",
								"externalInformationPort.BasicExternalInformationPort",
								"networkClock.BasicSystemTimeClock",
								"userDatabase.BasicUserDatabase",
								"message.BasicMessage"	
			});
	}


	@Override
	public boolean usesPropertyFile() {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public void constructor() {
		// TODO Auto-generated method stub
	}
	
	
	/**
	 * Returns the number of message that shall be processed in parallel 
	 * (as specified in property file).
	 * 
	 * @return Number of message that shall be processed in parallel.
	 */
	private int getNumberOfThreads() {
		
		int numberOfThreads = Settings.getPropertyAsInt("NUMBER_OF_THREADS");
		
		// -1 means "automatic detection"
		return	(numberOfThreads == -1)
				?  Runtime.getRuntime().availableProcessors()
				: numberOfThreads;
								 
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

				Request request = inputOutputHandler.getRequest(); // blocks until request is available
				request = (Request)recoder.recode(request);

				if (request != null && isMACCorrect(request) && !replayDetection.isReplay(request))
					outputStrategy.addRequest(request);

			}
			
		}


		private boolean isMACCorrect(Request request) {
			// TODO: implement
			return true;
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

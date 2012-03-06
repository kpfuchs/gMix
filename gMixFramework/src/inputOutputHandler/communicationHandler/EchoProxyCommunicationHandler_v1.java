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

package inputOutputHandler.communicationHandler;

import message.Request;
import userDatabase.DatabaseEventListener;
import userDatabase.User;


public class EchoProxyCommunicationHandler_v1 extends GeneralCommunicationHandler implements ProxyCommunicationHandler, DatabaseEventListener {

	private ReplyThread replyThread;
	//private boolean allowFragmentation; // TODO
	
	@Override
	public void constructor() {
		this.replyThread = new ReplyThread();
		//this.allowFragmentation = Settings.getPropertyAsBoolean("ALLOW_FRAGMENTATION_OF_REPLIES");
	}

	
	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	public void begin() {
		replyThread.start();
	}
	

	@Override
	public void userAdded(User user) {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	public void userRemoved(User user) {
		// TODO Auto-generated method stub
		
	}
	
	
	private class ReplyThread extends Thread {
		
		@Override
		public void run() {
			long ctr = 0;
			long timeFrame = 1000000000l; // in ms
			long start = -1;
			//long dur = -1;
			//long start2 = -1;
			Request request = inputOutputHandlerInternal.getProcessedRequest(); // blocking method
			
			while (true) {
				if (start == -1) {
					start = System.nanoTime();
					//start2 = System.currentTimeMillis();
				}else if (System.nanoTime() - start >= timeFrame) {
					System.out.println("received " +(ctr/1000l) +"kbytes in " +((System.nanoTime() - start)/1000000l) +" ms");
					//System.out.println(System.currentTimeMillis() -start2); 
					ctr = 0;
					start = System.nanoTime();
					//start2 = System.currentTimeMillis();
				}
				request = inputOutputHandlerInternal.getProcessedRequest(); // blocking method
				ctr += request.getByteMessage().length;
				
				/*Request request = inputOutputHandlerInternal.getProcessedRequest(); // blocking method
				//System.out.println(mix +" received this message (cleartext): " +Util.md5(request.getByteMessage())); // TODO
				ctr += request.getByteMessage().length;
				if (request.getByteMessage().length == 0)
					System.out.println("proxy received dummy"); 
				System.out.println("proxy received so far " +ctr +" bytes"); 
				if (mix.isDuplex() && request.getByteMessage().length != 0) {// TODO
					//int maxReplySize = recodingScheme.getMaxPayloadForNextReply(request.getOwner());
					Reply reply = MixMessage.getInstanceReply(request.getByteMessage(), request.getOwner(), settings);
					inputOutputHandlerInternal.addUnprocessedReply(reply);
				}*/
			}
			
		}
		
	}

}

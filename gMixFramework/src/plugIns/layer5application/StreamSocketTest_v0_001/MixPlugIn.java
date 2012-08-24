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
package plugIns.layer5application.StreamSocketTest_v0_001;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer5ApplicationMix;
import framework.core.routing.RoutingMode;
import framework.core.socket.socketInterfaces.AnonSocketOptions.CommunicationMode;
import framework.core.socket.socketInterfaces.StreamAnonServerSocket;
import framework.core.socket.socketInterfaces.StreamAnonSocketMix;
import framework.core.util.Util;


public class MixPlugIn extends Implementation implements Layer5ApplicationMix {

	private StreamAnonServerSocket serverSocket;
	
	@Override
	public void constructor() {
		
	}

	
	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}
	

	@Override
	public void begin() {
		System.out.println("StreamSocketTest_v0_001 loaded"); 
		CommunicationMode cm = anonNode.IS_DUPLEX ? CommunicationMode.DUPLEX : CommunicationMode.SIMPLEX_RECEIVER;
		this.serverSocket = anonNode.createStreamAnonServerSocket(settings.getPropertyAsInt("SERVICE_PORT1"), cm, anonNode.ROUTING_MODE != RoutingMode.CASCADE);
		new AcceptorThread().start(); 
	}


	private class AcceptorThread extends Thread {

		@Override
		public void run() {
			while (true)
				new ReplyThread(serverSocket.accept()).start();
		}
	}
	
	
	private class ReplyThread extends Thread {
		
		InputStream inputStream;
		OutputStream outputStream;
		
		
		public ReplyThread(StreamAnonSocketMix socket) {
			System.out.println("accept()"); // TODO: remove 
			this.inputStream = socket.getInputStream();
			if (anonNode.IS_DUPLEX)
				this.outputStream = socket.getOutputStream();
		}
		
		
		@Override
		public void run() {
			long ctr = 0;
			long timeFrame = 1000000000l; // in ms
			long start = -1;
			//long dur = -1;
			//long start2 = -1;
			
			//if (!mix.isDuplex()) { // simplex
			try {
				while (true) {
					byte[] data = new byte[anonNode.MAX_PAYLOAD];
					data = Util.forceRead(inputStream, data);
					if (start == -1) {
						start = System.nanoTime();
						//start2 = System.currentTimeMillis();
					} else if (System.nanoTime() - start >= timeFrame) {
						long dur = (System.nanoTime() - start)/1000000l;
						System.out.println("received " +(ctr/1024l) +"kbytes in " +dur +" ms; " +userDatabase.getNumberOfUsers() +" users; " +((ctr/1024l)/(dur/1000l)));
						//System.out.println(System.currentTimeMillis() -start2); 
						ctr = 0;
						start = System.nanoTime();
						//start2 = System.currentTimeMillis();
					}
					//request = inputOutputHandlerInternal.getProcessedRequest(); // blocking method
					ctr += data.length;
					if (anonNode.IS_DUPLEX && data.length != 0) {
						outputStream.write(data);
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
			}
				
				
			//} else { // duplex

	
	
			//while (true) {
				
				
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
			//}
			
		}
		
	}
}

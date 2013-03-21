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
package plugIns.layer5application.StatisticsPlugInStreamSocket_v0_001;

import java.io.IOException;
import java.io.InputStream;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer5ApplicationMix;
import framework.core.routing.RoutingMode;
import framework.core.socket.socketInterfaces.AnonSocketOptions.CommunicationMode;
import framework.core.socket.socketInterfaces.StreamAnonServerSocket;
import framework.core.socket.socketInterfaces.StreamAnonSocketMix;
import framework.core.socket.stream.BasicOutputStreamMix;


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
		if (anonNode.IS_LAST_MIX) {
			System.out.println("StatisticsPlugInStreamSocket_v0_001 loaded"); 
			CommunicationMode cm = anonNode.IS_DUPLEX ? CommunicationMode.DUPLEX : CommunicationMode.SIMPLEX_RECEIVER;
			this.serverSocket = anonNode.createStreamAnonServerSocket(settings.getPropertyAsInt("SERVICE_PORT1"), cm, anonNode.ROUTING_MODE != RoutingMode.CASCADE);
			new AcceptorThread().start(); 
		}
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
		BasicOutputStreamMix outputStream;
		
		public ReplyThread(StreamAnonSocketMix socket) {
			System.out.println("accept()"); // TODO: remove 
			this.inputStream = socket.getInputStream();
			if (anonNode.IS_DUPLEX)
				this.outputStream = (BasicOutputStreamMix) socket.getOutputStream();
		}
		
		
		@Override
		public void run() {
			try {
				while (true) {
					byte[] data = new byte[512];
					int read = inputStream.read(data);
					//data = Util.forceRead(inputStream, data);
					if (anonNode.IS_DUPLEX && data.length != 0) {
						outputStream.write(data, 0, read);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
	}
	
}

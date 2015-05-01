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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.StatisticsPlugInStreamSocket_v0_001;

import java.io.IOException;
import java.io.InputStream;

import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer5ApplicationMix;
import staticContent.framework.routing.RoutingMode;
import staticContent.framework.socket.socketInterfaces.StreamAnonServerSocket;
import staticContent.framework.socket.socketInterfaces.StreamAnonSocketMix;
import staticContent.framework.socket.socketInterfaces.AnonSocketOptions.CommunicationDirection;
import staticContent.framework.socket.socketInterfaces.NoneBlockingAnonSocketOptions.IO_Mode;
import staticContent.framework.socket.stream.BasicOutputStreamMix;


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
			CommunicationDirection cd = anonNode.IS_DUPLEX ? CommunicationDirection.DUPLEX : CommunicationDirection.SIMPLEX_RECEIVER;
			IO_Mode ioMode = IO_Mode.BLOCKING;
			this.serverSocket = anonNode.createStreamAnonServerSocket(settings.getPropertyAsInt("SERVICE_PORT1"), cd, ioMode, anonNode.ROUTING_MODE != RoutingMode.GLOBAL_ROUTING);
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

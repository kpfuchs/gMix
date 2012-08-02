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
package evaluation.loadGenerator.asFastAsPossible;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import evaluation.loadGenerator.LoadGenerator;
import framework.core.AnonNode;
import framework.core.launcher.ToolName;
import framework.core.socket.socketInterfaces.StreamAnonSocket;
import framework.core.socket.socketInterfaces.AnonSocketOptions.CommunicationMode;
import framework.core.util.Util;


public class AFAP_ThreadBased_LoadGenerator extends AFAP_LoadGenerator {
	
	private AnonNode client;

	
	protected AFAP_ThreadBased_LoadGenerator(LoadGenerator owner) {
		super(owner);
		System.out.println("LOAD_GENERATOR: start at " +System.currentTimeMillis());
		
		// create client
		owner.commandLineParameters.gMixTool = ToolName.CLIENT;
		this.client = new AnonNode(owner.commandLineParameters);
		
		int numberOfConnections = settings.getPropertyAsInt("AL-AFAP-NUMBER_OF_CLIENTS");
		for (int i=0; i<numberOfConnections; i++)
			new RequestThread().start();
	}

	
	public class RequestThread extends Thread {

		private Random random = new Random();
		
		@Override
		public void run() {
			CommunicationMode cm = client.IS_DUPLEX ? CommunicationMode.DUPLEX : CommunicationMode.SIMPLEX_SENDER;
			StreamAnonSocket socket = client.createStreamSocket(cm, false);
			try {
				socket.connect(settings.getPropertyAsInt("SERVICE_PORT1"));
				if (client.IS_DUPLEX)
					new ReplyThread(socket).start();
				OutputStream outputStream = socket.getOutputStream();
				byte[] data = new byte[socket.getMTU()];
				while(true) { 
					random.nextBytes(data);
					outputStream.write(data);
					outputStream.flush();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
		}
	}
	
	
	private class ReplyThread extends Thread {
		
		private StreamAnonSocket socket;
		private InputStream inputStream;
		
		
		public ReplyThread(StreamAnonSocket socket) throws IOException {
			this.socket = socket;
			this.inputStream = socket.getInputStream();
		}
		
		
		@Override
		public void run() {
			int mtu = socket.getMTU();
			while (true) {
				byte[] reply;
				try {
					reply = new byte[mtu];
					Util.forceRead(inputStream, reply);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
}
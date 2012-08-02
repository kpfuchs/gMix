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
package plugIns.layer5application.loadGeneratorPlugIn_v0_001;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer5ApplicationClient;


public class ClientPlugIn extends Implementation implements Layer5ApplicationClient {

	
	@Override
	public void constructor() {
	}


	@Override
	public void initialize() {
		
	}


	@Override
	public void begin() {

	}

	
	/*public class RequestThread extends Thread {

		private Random random = new Random();
		
		@Override
		public void run() {
			
			CommunicationMode cm = anonNode.IS_DUPLEX ? CommunicationMode.DUPLEX : CommunicationMode.SIMPLEX_SENDER;
			StreamAnonSocket socket = anonNode.createStreamSocket(cm, false);
			
			//long ct = 0;
			try {
				socket.connect(settings.getPropertyAsInt("SERVICE_PORT1"));
				if (anonNode.IS_DUPLEX)
					new ReplyThread(socket).start();
				OutputStream outputStream = socket.getOutputStream();
				int mtu = socket.getMTU();
				byte[] data = new byte[mtu];
				//int ctr = 0;
				
				while(true) { 
					random.nextBytes(data);
					outputStream.write(data);
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
		
		//int ct = 0;

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
		
	}*/

}

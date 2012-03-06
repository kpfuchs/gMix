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

import framework.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import userDatabase.User;

import message.MixMessage;
import message.Reply;
import message.Request;


public class Tcp_Sync_IO_ClientCommunicationHandler_v1 extends GeneralCommunicationHandler implements ClientCommunicationHandler {
	//TODO: timeout fŸr inaktive nutzer einfŸhren
	
	private int port; 
	private InetAddress bindAddress;
	private int backlog;
	private ServerSocket serverSocket;
	private int maxRequestLength;
	
	
	@Override
	public void constructor() {
		this.bindAddress = settings.getPropertyAsInetAddress("BIND_ADDRESS");
		this.port = settings.getPropertyAsInt("PORT");
		this.backlog = settings.getPropertyAsInt("BACKLOG");
		this.maxRequestLength = settings.getPropertyAsInt("MAX_REQUEST_LENGTH");
	}
	

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}
	

	@Override
	public void begin() {
		try {
			this.serverSocket = new ServerSocket(port, backlog, bindAddress);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not open ServerSocket"); 
		}
		System.out.println(mix +" listening on " +bindAddress +":" +port);
		new AcceptorThread().start();
	}

	
	private class AcceptorThread extends Thread {
		
		@Override
		public void run() {
			while (true) {
				try {
					Socket client = serverSocket.accept();
					User user = userDatabase.generateUser();
					userDatabase.addUser(user);
					new RequestThread(user, client.getInputStream()).start();
					new ReplyThread(user, client.getOutputStream()).start();
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
			}
		}
		
	}
	
	
	private class RequestThread extends Thread {
		
		private User user;
		private InputStream inputStream;
		
		public RequestThread(User user, InputStream inputStream) {
			this.user = user;
			this.inputStream = inputStream;
		}
		
		@Override
		public void run() {
			try {
				while (true) {
					int messageLength = Util.forceReadInt(inputStream);
					if (messageLength > maxRequestLength)
						throw new IOException("warning: user " +user +" sent a too large message");
					Request request = MixMessage.getInstanceRequest(Util.forceRead(inputStream, messageLength), user, settings);
					inputOutputHandlerInternal.addUnprocessedRequest(request); // might block
				}
			} catch (IOException e) {
				System.err.println("warning: connection to " +user +" lost");
				e.printStackTrace();
			}
		}
	}
	
	
	private class ReplyThread extends Thread {
		
		private User user;
		private OutputStream outputStream;
		
		public ReplyThread(User user, OutputStream outputStream) {
			this.user = user;
			this.outputStream = outputStream;
		}

		@Override
		public void run() {
			try {
				while (true) {
					Reply reply = inputOutputHandlerInternal.getProcessedReply();
					outputStream.write(Util.intToByteArray(reply.getByteMessage().length));
					outputStream.write(reply.getByteMessage());
					outputStream.flush();
				}
			} catch (IOException e) {
				System.err.println("warning: connection to " +user +" lost");
				e.printStackTrace();
			}
		}
	}
	
}

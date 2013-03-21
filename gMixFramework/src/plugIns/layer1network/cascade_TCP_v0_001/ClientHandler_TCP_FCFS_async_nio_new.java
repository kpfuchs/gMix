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
package plugIns.layer1network.cascade_TCP_v0_001;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import framework.core.controller.SubImplementation;
import framework.core.message.Request;
import framework.core.routing.RoutingMode;
import framework.core.userDatabase.User;
import framework.core.userDatabase.UserAttachment;
import framework.core.util.Util;


// MP12-13
// TODO: may block in duplex mode...
public class ClientHandler_TCP_FCFS_async_nio_new extends SubImplementation {

	/* properties */
	private int PORT;
	private InetAddress MIX_BIND_ADDRESS;
	private int BACKLOG;
	private int SO_TIMEOUT;
	private int MAX_REQUEST_LENGTH;
	private int QUEUE_BLOCK_SIZE;
	private int MAX_READS_IN_A_ROW;

	
	@Override
	public void constructor() {
		if (anonNode.ROUTING_MODE != RoutingMode.CASCADE)
			throw new RuntimeException("this is a cascade plug-in; ROUTING_MODE is not set to CASCADE -> will exit now");
		this.MIX_BIND_ADDRESS = settings.getPropertyAsInetAddress("GLOBAL_MIX_BIND_ADDRESS");
		this.PORT = settings.getPropertyAsInt("GLOBAL_MIX_BIND_PORT");
		this.BACKLOG = settings.getPropertyAsInt("BACKLOG");
		this.SO_TIMEOUT = settings.getPropertyAsInt("SO_TIMEOUT");
		this.MAX_REQUEST_LENGTH = settings.getPropertyAsInt("MAX_REQUEST_LENGTH");
		this.QUEUE_BLOCK_SIZE = settings.getPropertyAsInt("GLOBAL_QUEUE_BLOCK_SIZE");
		this.MAX_READS_IN_A_ROW = settings.getPropertyAsInt("MAX_READS_IN_A_ROW");
	}

	
	@Override
	public void initialize() {
		
	}

	
	@Override
	public void begin() {
		new NIOLoop().start();
	}

	
	/**
	 * Main loop which selects the nio events.
	 * 
	 * @author Christopher Bartz
	 */
	private class NIOLoop extends Thread {
		Selector selector;
		ServerSocketChannel serverSocketChannel;
		List<Request> requestList;

		NIOLoop() {
			this.setName("NIOLoop");
			this.requestList = new ArrayList<Request>(QUEUE_BLOCK_SIZE);
		}

		
		@Override
		public void run() {

			try {
				serverSocketChannel = ServerSocketChannel.open();
				serverSocketChannel.configureBlocking(false);
				ServerSocket serverSocket = serverSocketChannel.socket();
				InetSocketAddress endpoint = new InetSocketAddress(MIX_BIND_ADDRESS, PORT);
				serverSocket.bind(endpoint, BACKLOG);
				serverSocket.setSoTimeout(SO_TIMEOUT);

				selector = Selector.open();
				serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not open ServerSocket");
			}

			while (true) {
				try {
					selector.select();
					Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
					while (selectedKeys.hasNext()) {
						SelectionKey sk = selectedKeys.next();
						selectedKeys.remove();

						int readyOps = sk.readyOps();

						if ((readyOps & SelectionKey.OP_ACCEPT) != 0) {
							handleAcceptRequest();
						}

						if ((readyOps & SelectionKey.OP_READ) != 0) {
							handleReadRequest(sk);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

		
		/**
		 * Handles an accept request and generates a
		 * {@link framework.core.userDatabase.User} object and adds it to the
		 * {@link framework.core.userDatabase.UserDataBase}.
		 */
		private void handleAcceptRequest() {

			try {
				SocketChannel sc = serverSocketChannel.accept();
				sc.configureBlocking(false);
				User user = userDatabase.generateUser();
				userDatabase.addUser(user);

				sc.register(selector, SelectionKey.OP_READ,
						new UserChannelData(user, this, sc));
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		
		/**
		 * Tries to read requests from the channel associated with the provided
		 * {@link java.nio.channels.SelectionKey}.
		 * 
		 * @param sk
		 * @throws IOException
		 */
		private void handleReadRequest(SelectionKey sk) throws IOException {

			UserChannelData userData = (UserChannelData) sk.attachment();
			Request request;

			try {
				/*
				 * store up to MAX_READS_IN_A_ROW messages in a list of size
				 * QUEUE_BLOCK_SIZE
				 */
				for (int i = 1; i <= MAX_READS_IN_A_ROW; i++) {
					request = tryToReadRequest(userData);
					if (request != null) {
						requestList.add(request);
						if ((i % QUEUE_BLOCK_SIZE) == 0) {
							anonNode.putInRequestInputQueue(requestList
									.toArray(new Request[0]));
							requestList.clear();
						}
					} else {
						break;
					}
				}

				if (requestList.size() > 0) {
					anonNode.putInRequestInputQueue(requestList
							.toArray(new Request[0]));
					requestList.clear();
				}
			} catch (UnsupportedOperationException e) {
				e.printStackTrace();
				requestList = new ArrayList<Request>(QUEUE_BLOCK_SIZE);
			}

		}

		
		/**
		 * Reads a {@link framework.core.message.Request} from a
		 * {@link framework.core.userDatabase.User}.
		 * 
		 * @param userData
		 *            the data of the user
		 * @return a Request or null if a Request could not be read.
		 * @throws IOException
		 */
		private Request tryToReadRequest(UserChannelData userData)
				throws IOException {
			SocketChannel sc = userData.socketChannel;

			if (userData.receivedData == null)
				userData.receivedData = ByteBuffer.allocate(4);

			if (!userData.requestLengthHeaderRead) {

				if (sc.read(userData.receivedData) == -1) {
					throw new IOException("warning: Connection lost to user " + userData.getOwner());
				}

				if (userData.receivedData.hasRemaining()) {
					/* then we did not read the full header */
					return null;
				} else {
					userData.requestLengthHeaderRead = true;

					userData.receivedData.flip();
					byte[] lengthHeader = new byte[4];
					userData.receivedData.get(lengthHeader);

					int msgLength = Util.byteArrayToInt(lengthHeader);
					if (msgLength > MAX_REQUEST_LENGTH)
						throw new IOException("warning: user " + userData.getOwner() + " sent a too large message");

					userData.receivedData = ByteBuffer.allocate(msgLength);
				}

			}

			if (sc.read(userData.receivedData) == -1) {
				throw new IOException("warning: Connection lost to user "
						+ userData.getOwner());
			}
			if (userData.receivedData.hasRemaining()) {
				/* then we did not read the full message */
				return null;
			} else {
				byte[] msg = new byte[userData.receivedData.capacity()];
				userData.receivedData.flip();
				userData.receivedData.get(msg);
				userData.receivedData = null;
				userData.requestLengthHeaderRead = false;
				return Request.getInstanceRequest(msg, userData.getOwner());
			}

		}
	}

	
	/**
	 * Stores the channel of a user and incomplete data (= a message has not
	 * been received completely).
	 * 
	 * @author Christopher Bartz
	 * 
	 */
	private class UserChannelData extends UserAttachment {

		ByteBuffer receivedData;
		SocketChannel socketChannel;
		boolean requestLengthHeaderRead = false;

		public UserChannelData(User owner, Object callingInstance,
				SocketChannel socketChannel) {
			super(owner, callingInstance);
			this.socketChannel = socketChannel;
		}

	}

}

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
import java.nio.BufferUnderflowException;
import java.util.HashMap;

import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoAcceptor;

import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import framework.core.userDatabase.User;

import framework.core.util.Util;
import framework.core.controller.SubImplementation;


/**
 * Implements an asynchronous Client I/O Handler based on Mina 2.0.4 Extends the
 * abstract class SubImplementation
 * 
 * @author Christopher Bartz, Jan Henrik Röwekamp, Arne Springborn
 * 
 */
public class ClientHandler_TCP_FCFS_async_mina extends SubImplementation {

	private int port;
	private InetAddress bindAddress;
	private int maxRequestLength;
	//private boolean DUPLEX_ON;

	
	@Override
	public void constructor() {
		this.bindAddress = settings.getPropertyAsInetAddress("GLOBAL_MIX_BIND_ADDRESS");
		this.port = settings.getPropertyAsInt("GLOBAL_MIX_BIND_PORT");
		//this.backlog = settings.getPropertyAsInt("BACKLOG");
		this.maxRequestLength = settings.getPropertyAsInt("MAX_REQUEST_LENGTH");
	}

	
	@Override
	public void initialize() {
		//this.DUPLEX_ON = infoService.getIsDuplexModeOn();
	}

	
	@Override
	public void begin() {
		IoAcceptor acceptor = new NioSocketAcceptor();

		// adds one filter(the codec) to the filterchain
		acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new New_ProtocolEncoder(), new New_ProtocolDecoder()));
		// sets the acceptor to use the new implemented Handler
		acceptor.setHandler(new ClientIOHandler());

		try {
			acceptor.bind(new InetSocketAddress(this.bindAddress, this.port));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not open ServerSocket.");
		}

	}
	

	/**
	 * The ClientIOHandler extends the abstract class IoHandlerAdapter and
	 * therefore has to implement the methods exceptionCaught, messageReceived
	 * and sessionCreated
	 * 
	 */
	class ClientIOHandler extends IoHandlerAdapter {
		private int counter;
		HashMap<User, IoSession> lsession;

		ClientIOHandler() {
			super();
			counter = 0;
			lsession = new HashMap<User, IoSession>();
			new AsyncMinaReplyThread().start();
		}

		/**
		 * Implements the exception handling
		 */
		@Override
		public void exceptionCaught(IoSession session, Throwable cause)
				throws Exception {
			cause.printStackTrace();
		}

		/**
		 * is called when a message is received. writes the message to the upper
		 * layer
		 */
		@Override
		public void messageReceived(IoSession session, Object message)
				throws Exception {
			Request request = (Request) message;
			anonNode.putInRequestInputQueue(request);
		}

		/**
		 * is called when a new session is created. generates a new user and
		 * binds the session
		 */
		@Override
		public void sessionCreated(IoSession session) throws Exception {

			if (++this.counter % 100 == 0) {
				System.out.println(counter + " connections");
			}
			User user = userDatabase.generateUser();
			userDatabase.addUser(user);
			lsession.put(user, session);
			session.setAttribute("user", user);
		}

		/**
		 * Implements a basic Thread to reply requests from clients
		 * 
		 */
		class AsyncMinaReplyThread extends Thread {

			@Override
			public void run() {

				while (true) {

					Reply[] replies = anonNode.getFromReplyOutputQueue();
					for (Reply reply : replies) {

						User user = reply.getOwner();
						byte[] bytemsg = reply.getByteMessage();
						byte[] length = Util.intToByteArray(bytemsg.length);

						byte[] msg = new byte[bytemsg.length + length.length];

						for (int i = 0; i < 4; i++) {
							msg[i] = length[i];
						}
						for (int i = 0; i < bytemsg.length; i++) {
							msg[i + length.length] = bytemsg[i];
						}

						lsession.get(user).write(msg);
					}
				}
			}
		}
	}

	
	/**
	 * Implements a Codec to prevent a negative impact of message fragmenting.
	 * is used to convert the input bytes to high-level Mina Objects
	 * 
	 */
	class New_ProtocolDecoder extends CumulativeProtocolDecoder {

		/**
		 * continuously tries to decodes a message until true is returned.
		 */
		@Override
		protected boolean doDecode(IoSession session, IoBuffer in,
				ProtocolDecoderOutput out) throws Exception {

			int startposition = in.position();

			if (in.remaining() < 4) {
				return false;
				// not enough data to read -> doDecode is called again
			}

			byte[] msgLength = new byte[4];
			in.get(msgLength);

			int messageLength = Util.byteArrayToInt(msgLength);
			byte[] msg = new byte[messageLength];

			try {
				in.get(msg, 0, messageLength);
			} catch (BufferUnderflowException e) {
				in.position(startposition);
				return false;
				// not enough data to read -> doDecode is called again
			}

			User user = (User) session.getAttribute("user");
			if (messageLength > maxRequestLength)
				throw new IOException("warning: user " + user
						+ " sent a too large message");
			// a too large message is not expected -> throws an exception
			Request request = MixMessage.getInstanceRequest(msg, user);
			out.write(request);

			return true;
			// reading the message was successful -> continue with the next
			// filter
		}

	}

	
	/**
	 * forwards the sending data to the output. No Protocol processing is needed
	 * 
	 */
	class New_ProtocolEncoder extends ProtocolEncoderAdapter {

		@Override
		public void encode(IoSession session, Object msg,
				ProtocolEncoderOutput out) throws Exception {
			byte[] bytemsg = (byte[]) msg;
			IoBuffer buf = IoBuffer.wrap(bytemsg);
			out.write(buf);
		}

	}

}

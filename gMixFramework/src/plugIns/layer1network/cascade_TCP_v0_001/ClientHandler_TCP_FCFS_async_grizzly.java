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
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.IOStrategy;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;

import framework.core.controller.SubImplementation;
import framework.core.message.MixMessage;
import framework.core.message.Request;
import framework.core.userDatabase.User;
import framework.core.util.Util;


// TODO: may block in duplex mode...
public class ClientHandler_TCP_FCFS_async_grizzly extends SubImplementation {

	private int port;
	private InetAddress bindAddress;
	private int backlog;
	private int maxRequestLength;
	private ConcurrentMap<User, Connection<?>> usermap;
	private int QUEUE_BLOCK_SIZE;

	
	/**
	 * @see framework.ThreePhaseStart#constructor()
	 */
	@Override
	public void constructor() {
		this.bindAddress = settings.getPropertyAsInetAddress("GLOBAL_MIX_BIND_ADDRESS");
		this.port = settings.getPropertyAsInt("GLOBAL_MIX_BIND_PORT");
		this.backlog = settings.getPropertyAsInt("BACKLOG");
		this.maxRequestLength = settings.getPropertyAsInt("MAX_REQUEST_LENGTH");
		this.usermap = new ConcurrentHashMap<User, Connection<?>>();
		this.QUEUE_BLOCK_SIZE = settings.getPropertyAsInt("GLOBAL_QUEUE_BLOCK_SIZE");
	}

	
	/**
	 * @see framework.ThreePhaseStart#initialize()
	 */
	@Override
	public void initialize() {

	}

	
	/**
	 * @see framework.ThreePhaseStart#begin()
	 */
	@Override
	public void begin() {
		final IOStrategy iostrategy = WorkerThreadIOStrategy.getInstance();
		final FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
		final TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();

		filterChainBuilder.add(new TransportFilter());
		filterChainBuilder.add(new Filter());

		transport.setKeepAlive(true);
		transport.setProcessor(filterChainBuilder.build());
		transport.setIOStrategy(iostrategy);

		try {
			transport.bind(this.bindAddress.getHostAddress(), this.port, this.backlog);
			transport.start();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not open ServerSocket.");
		}

	}

	
	/**
	 * Implements the protocol logic.
	 * 
	 * @author Christopher Bartz
	 * 
	 */
	private class Filter extends BaseFilter {

		int counter = 0;
		Map<Connection<?>, User> connMap = new HashMap<Connection<?>, User>();

		
		@Override
		public NextAction handleRead(FilterChainContext ctx) throws IOException {
			Buffer buf = (Buffer) ctx.getMessage(); /*
													 * we assume a Buffer
													 * because we have a
													 * TransportFilter before us
													 * in the chain
													 */
			Connection<?> conn = ctx.getConnection();
			byte[] msg;
			List<Request> requestList = new ArrayList<Request>(QUEUE_BLOCK_SIZE);

			byte[] msgLengthArray = new byte[4];

			/* fetch up to QUEUE_BLOCK_SIZE messages and put them in the queue */
			while (buf.remaining() >= 4) {
				/*
				 * we remember our current position, so we will be able to reset
				 * after an BufferUnderflow
				 */
				int startposition = buf.position();

				buf.get(msgLengthArray);
				int msgLength = Util.byteArrayToInt(msgLengthArray);

				if (buf.remaining() < msgLength) {
					buf.position(startposition);
					buf.shrink();

					if (requestList.size() > 0) {
						anonNode.putInRequestInputQueue(requestList.toArray(new Request[0]));
					}
					return ctx.getStopAction(buf);
				}

				if (msgLength > maxRequestLength) {
					buf.position(startposition + 4 + msgLength);
					System.err.println("warning: user " + connMap.get(conn) + " sent a too large message");
				} else {
					msg = new byte[msgLength];
					try {
						buf.get(msg, 0, msgLength);

						Request request = MixMessage.getInstanceRequest(msg,
								connMap.get(conn));
						requestList.add(request);
						if (requestList.size() >= QUEUE_BLOCK_SIZE) {
							anonNode.putInRequestInputQueue(requestList.toArray(new Request[0]));
							requestList.clear();
						}
					} catch (BufferUnderflowException e) {
						buf.position(startposition);
						buf.shrink();

						if (requestList.size() > 0) {
							anonNode.putInRequestInputQueue(requestList.toArray(new Request[0]));
						}
						return ctx.getStopAction(buf);
					}
				}
			}

			if (requestList.size() > 0) {
				anonNode.putInRequestInputQueue(requestList.toArray(new Request[0]));
			}

			/* if we have up to 3 bytes remaining, we need to store the buffer */
			if (buf.remaining() > 0) {
				buf.shrink();
				return ctx.getStopAction(buf);
			} else {
				return ctx.getStopAction();
			}
		}

		
		@Override
		public NextAction handleAccept(FilterChainContext ctx)
				throws IOException {

			if (++counter % 100 == 0)
				System.out.println(counter + " connections accepted");

			User user = userDatabase.generateUser();
			userDatabase.addUser(user);
			usermap.putIfAbsent(user, ctx.getConnection());
			connMap.put(ctx.getConnection(), user);

			return ctx.getStopAction();
		}

	}

}

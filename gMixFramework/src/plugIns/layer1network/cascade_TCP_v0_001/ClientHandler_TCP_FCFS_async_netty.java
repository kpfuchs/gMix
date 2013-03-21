/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2013  Karl-Peter Fuchs
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

//import java.lang.management.ManagementFactory;
//import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;

import framework.core.controller.SubImplementation;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.userDatabase.DatabaseEventListener;
import framework.core.userDatabase.User;
import framework.core.util.Util;


//TODO: may block in duplex mode...
public class ClientHandler_TCP_FCFS_async_netty extends SubImplementation implements DatabaseEventListener {

	private ChannelFactory _factory;
	private ChannelGroup _channelGroup;
	private HashMap<Channel, User> _userMap;
	private HashMap<User, Channel> _channelMap;
	private HashMap<User, Integer> _userPackets;
	private HashMap<User, Long> _userResetTimestamps;
	private long _timestamp;
	private Channel _serverChannel;
	private int _maxRequestLength;
	private int _maxReadsInARow;
	private int _blackListDuration;
	private int _port;
	private InetAddress _bindAddress;

	
	@Override
	public void constructor() {
		_channelGroup = new DefaultChannelGroup("Connections");
		_factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
		_port = settings.getPropertyAsInt("GLOBAL_MIX_BIND_PORT");
		_bindAddress = settings.getPropertyAsInetAddress("GLOBAL_MIX_BIND_ADDRESS");
		_maxReadsInARow = settings.getPropertyAsInt("MAX_READS_IN_A_ROW");

		_userMap = new HashMap<Channel, User>();
		_channelMap = new HashMap<User, Channel>();
		_userPackets = new HashMap<User, Integer>();
		_userResetTimestamps = new HashMap<User, Long>();
		_timestamp = 0L;
		_blackListDuration = 1;

		_maxRequestLength = settings.getPropertyAsInt("MAX_REQUEST_LENGTH");

	}

	
	@Override
	public void initialize() {

	}

	
	@Override
	public void begin() {
		new NettyThread().start();
		new ReplyThread().start();

	}

	
	@Override
	public void userAdded(User user) {

	}

	
	@Override
	public void userRemoved(User user) {

	}

	
	private void openSocket() {
		ServerBootstrap bootstrap = new ServerBootstrap(_factory);
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(new ReplayingDecoderImplemenatation(),
						new FrameForwarder());
			}
		});
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.keepAlive", true);

		_serverChannel = bootstrap.bind(new InetSocketAddress(_bindAddress,
				_port));

		_channelGroup.add(_serverChannel);
	}

	
	private class NettyThread extends Thread {
		@Override
		public void run() {
			openSocket();
			// System.out.println("Adding Thread #" +
			// Thread.currentThread().getId() + " to Netty pool.");
			// GlobalLauncher.NettyThreads.add((Long)Thread.currentThread().getId());

		}
	}

	
	private class ReplayingDecoderImplemenatation extends
			ReplayingDecoder<States> {

		@Override
		protected Object decode(ChannelHandlerContext ctx, Channel channel,
				ChannelBuffer buffer, States state) {
			// long threadID = Thread.currentThread().getId();
			// if(!GlobalLauncher.NettyThreads.contains(threadID))
			// GlobalLauncher.NettyThreads.add(threadID);
			return buffer.readBytes(buffer.readInt());
		}

	}

	
	private class FrameForwarder extends SimpleChannelHandler {

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

			User user;
			if (e.getChannel().getAttachment() == null) {
				user = userDatabase.generateUser();
				userDatabase.addUser(user);
				e.getChannel().setAttachment(true);
				_userMap.put(e.getChannel(), user);
				_channelMap.put(user, e.getChannel());
				_userPackets.put(user, 0);
			} else {
				user = _userMap.get(e.getChannel());
			}

			_userMap.put(e.getChannel(), user);

			ChannelBuffer buf = (ChannelBuffer) e.getMessage();
			byte[] upBuf = new byte[buf.readableBytes()];
			buf.readBytes(upBuf);
			if (upBuf.length > _maxRequestLength) {
				System.err.println("warning: user " + user
						+ " sent a too large message");
			}
			Request r = MixMessage.getInstanceRequest(upBuf, user);

			int currentPackets = getUserpackets(user);
			if (currentPackets <= _maxReadsInARow) {
				increaseUserpackets(user, currentPackets);
				// _userPackets.put(user, currentPackets + 1);
			} else {
				long targetTime = _timestamp + _blackListDuration;
				while (_userResetTimestamps.containsValue(targetTime))
					--targetTime;

				_userResetTimestamps.put(user, targetTime);

				do {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e1) {

					}
				} while (_userResetTimestamps.get(user) > _timestamp);

				_userPackets.put(user, 0);
				_blackListDuration = _userMap.size();
			}

			// int currentPackets = increaseUserpackets(user);
			//
			// if(currentPackets % 1000 == 0)
			// System.out.println("Accepted " + currentPackets +
			// " frames from user " + user);

			anonNode.putInRequestInputQueue(r);
			increaseTimestamp();

			// System.out.println("I'm a Netty Thread and have got ID #" +
			// Thread.currentThread().getId());
		}

	}

	
	private synchronized void increaseUserpackets(User user, int currentPackets) {
		// int currentPackets = _userPackets.get(user);
		_userPackets.put(user, currentPackets + 1);
		// return currentPackets;
	}

	
	private synchronized int getUserpackets(User user) {
		return _userPackets.get(user);
	}

	
	private enum States {
		LENGTH, PAYLOAD;
	}

	
	private class ReplyThread extends Thread {
		
		@Override
		public void run() {
			while (true) {
				Reply[] replies = anonNode.getFromReplyOutputQueue();
				for (Reply reply : replies) {
					assert reply != null;
					assert reply.getOwner() != null;

					Channel chn = _channelMap.get(reply.getOwner());

					ChannelBuffer outBuf = ChannelBuffers.copiedBuffer(
							Util.intToByteArray(reply.getByteMessage().length),
							reply.getByteMessage());

					chn.write(outBuf);
				}
			}

		}
	}
	

	private synchronized void increaseTimestamp() {
		++_timestamp;
	}

}

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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
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
import framework.core.launcher.GlobalLauncher;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.userDatabase.DatabaseEventListener;
import framework.core.userDatabase.User;
import framework.core.util.Util;


public class ClientHandler_TCP_FCFS_async_netty_2 extends SubImplementation
		implements DatabaseEventListener {

	private ChannelFactory _factory;
	private ChannelGroup _channelGroup;
	private HashMap<Channel, User> _userMap;
	private HashMap<User, Channel> _channelMap;

	private Channel _serverChannel;
	private int _maxRequestLength;

	private int _port;
	private InetAddress _bindAddress;

	@Override
	public void userAdded(User user) {
		// TODO Auto-generated method stub

	}

	@Override
	public void userRemoved(User user) {
		// TODO Auto-generated method stub

	}

	@Override
	public void constructor() {
		_channelGroup = new DefaultChannelGroup("Connections");
		_factory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool());
		_port = settings.getPropertyAsInt("GLOBAL_MIX_BIND_PORT");
		_bindAddress = settings
				.getPropertyAsInetAddress("GLOBAL_MIX_BIND_ADDRESS");

		_userMap = new HashMap<Channel, User>();
		_channelMap = new HashMap<User, Channel>();
		_maxRequestLength = settings.getPropertyAsInt("MAX_REQUEST_LENGTH");

	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub

	}

	@Override
	public void begin() {
		new NettyThread().start();
		new ReplyThread().start();

	}

	private void openSocket() {
		ServerBootstrap bootstrap = new ServerBootstrap(_factory);
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(new ReplayingDecoderImplemenatation(), new FrameForwarder());
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
			//System.out.println("Adding Thread #" + Thread.currentThread().getId() + " to Netty pool.");
			//GlobalLauncher.NettyThreads.add((Long)Thread.currentThread().getId());
			
		}
	}

	private class ReplayingDecoderImplemenatation extends
			ReplayingDecoder<States> {

		@Override
		protected Object decode(ChannelHandlerContext ctx, Channel channel,
				ChannelBuffer buffer, States state) {
			long threadID = Thread.currentThread().getId();
			//if(!GlobalLauncher.NettyThreads.contains(threadID))
			//	GlobalLauncher.NettyThreads.add(threadID);
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
			}
			else
			{
				user =_userMap.get(e.getChannel());
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
			anonNode.putInRequestInputQueue(r);
			
			
			
			//System.out.println("I'm a Netty Thread and have got ID #" + Thread.currentThread().getId());
		}

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

}

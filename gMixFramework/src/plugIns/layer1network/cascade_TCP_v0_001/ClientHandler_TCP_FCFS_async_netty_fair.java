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
import org.jboss.netty.handler.execution.ChannelUpstreamEventRunnable;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.util.ObjectSizeEstimator;

import framework.core.controller.SubImplementation;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.userDatabase.DatabaseEventListener;
import framework.core.userDatabase.User;
import framework.core.util.Util;


/**
 * Implements an asynchronous client handler based on the Netty framework
 * (https://netty.io) Netty version 3.5.8 was used during implementation and all
 * versions above should work fine. Netty version 3.3.1 is NOT compatible (since
 * it lacks channel attachments). Make sure to update ivy.xml accordingly.
 * 
 * Netty is an event based IO framework, and makes use of event handlers
 * (similar to GUI programming), pipelines and channels. A channel represents
 * the communication channel to a certain peer. Therefore every communication
 * partner can be identified by the according channel. Everytime a message is
 * received an event is raised and the message is passed to the pipeline defined
 * for the related channel. The pipeline consists of an arbitrary amount of
 * event handlers, each processing the result of the channel located before it
 * in the pipeline. Different handlers may serve very different tasks. In this
 * implementation there are three handlers in the pipeline: One for decoding the
 * message format, one for relaying the decoded message to the upper layers and
 * finally one to enforce fairness among client channels and also mix-to-mix
 * channels. While Netty is based on Java NIO, it can also extremely easily be
 * changed to Java's classic stream based network handling. To achieve this, see
 * the comment in the class constructor.
 * 
 * Useful hints for implementations of client IO handlers: - Client IO handlers
 * need to extend the abstract class SubImplementation. - The interface to pass
 * the data to the above layers is the "Request Input Queue". - Data can be
 * forwarded by calling the putInRequestInputQueue(Request) method of
 * "anonNode". anonNode is a member of SubImplementation and by that available
 * as soon as the class extends SubImplementation. - To forward data it needs to
 * be assigned to a certain user. - We are in charge of creating new users for
 * packets with so far unknown origin. If a new user gets created make sure to
 * put it into the userDatabase (member of SubImplementation). - The easiest way
 * to keep track of the users (and in Netty terms by that channels) is to store
 * them in a table and look them up whenever a message arrives. For this Netty
 * implementation this problem is solved with channel attachments. - !! Wrong
 * user assignment may lead to RSA-errors on higher levels of the framework !! -
 * For the actual forward process a Request is required, which can be created
 * with an instance of User and some data by calling
 * MixMessage.getInstanceRequest(data, user), which is a static method.
 * 
 * @author Jan Henrik Röwekamp
 * 
 */
public class ClientHandler_TCP_FCFS_async_netty_fair extends SubImplementation implements DatabaseEventListener {

	private ChannelFactory _factory;
	private ChannelGroup _channelGroup;
	private HashMap<Channel, User> _userMap;
	private HashMap<User, Channel> _channelMap;
	private Channel _serverChannel;
	private int _maxRequestLength;
	private int _maxReadsInARow;
	private int _maxReadsPreferMixFactor;
	private int _port;
	private InetAddress _bindAddress;
	private Channel _exampleMixChannel;
	private OrderedMemoryAwareThreadPoolExecutor _ordMemAwareInst;

	
	@Override
	public void constructor() {
		_channelGroup = new DefaultChannelGroup("Connections");

		// Netty Feature:
		// The whole handler can instantly be converted to an equivalent
		// stream-based version, by changing
		// NioServerSocketChannelFactory to OioServerSocketChannelFactory. (The
		// class still needs to be imported)
		_factory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool());
		_port = settings.getPropertyAsInt("GLOBAL_MIX_BIND_PORT");
		_bindAddress = settings.getPropertyAsInetAddress("GLOBAL_MIX_BIND_ADDRESS");

		_userMap = new HashMap<Channel, User>();
		_channelMap = new HashMap<User, Channel>();

		_maxRequestLength = settings.getPropertyAsInt("MAX_REQUEST_LENGTH");
		_maxReadsInARow = settings.getPropertyAsInt("MAX_READS_IN_A_ROW");
		_maxReadsPreferMixFactor = settings.getPropertyAsInt("MAX_READS_PREFER_MIX_FACTOR");
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
	
	
	/**
	 * The main Netty thread. Opens the server socket and waits for incoming
	 * data.
	 */
	private class NettyThread extends Thread {
		@Override
		public void run() {
			openSocket();
		}
	}

	
	/**
	 * Opens the server socket. Generates a new Pipeline factory and
	 * instantiates the related decoding classes. The fairness is enforced by
	 * the third element in the pipeline: the
	 * OrderedMemoryAwareThreadPoolExecutor For more information on how it's
	 * used for that purpose also check the comment on the
	 * gMixMessageSizeEstimator class.
	 */
	private void openSocket() {
		ServerBootstrap bootstrap = new ServerBootstrap(_factory);

		// Nettys OrderedMemoryAwareThreadPoolExecutor is used to limit the
		// total memory consumed by the channels altogether.
		// It also gives the possibility to limit every channel to a certain
		// amount of memory (instead or in addition to a
		// global maximum).
		// The way the memory value is computed can be overwritten by setting an
		// according ObjectSizeEstimator.
		// We now use exactly this possibility to assign mix messages a smaller
		// relative size than normal messages in
		// the gMixMessageSizeEstimator class. By that more mix-to-mix
		// communication messages will fit into a certain
		// channel, before it's put on hold, than client ones do. By that
		// mix-to-mix communication gets preferred over client-
		// to-mix communication.
		// The OrderedMemoryAwareThreadPoolExecutor is a single thread running
		// for all input pipelines (since it influences
		// them all). Since the RequestInputQueue is blocking (it's implemented
		// as ArrayBlockingQueue) anyways,
		// essentially no speed will be lost by going single-threaded in this
		// case.
		_ordMemAwareInst = new OrderedMemoryAwareThreadPoolExecutor(1, _maxReadsInARow * _maxReadsPreferMixFactor, 0);
		_ordMemAwareInst.setObjectSizeEstimator(new gMixMessageSizeEstimator());
		bootstrap.setPipelineFactory(new PipelineFactory());
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.keepAlive", true);

		_serverChannel = bootstrap.bind(new InetSocketAddress(_bindAddress, _port));

		_channelGroup.add(_serverChannel);
	}

	
	/**
	 * The PipelineFactory is used to generate pipelines for the channels. In
	 * this class the order, count and choice of the Elements of the pipeline
	 * for each channel is defined.
	 * 
	 */
	private class PipelineFactory implements ChannelPipelineFactory {
		@Override
		public ChannelPipeline getPipeline() throws Exception {
			ChannelPipeline pipeline = Channels.pipeline(
					new ReplayingDecoderImplemenatation(),
					new FrameForwarder(),
					new ExecutionHandler(_ordMemAwareInst));
			return pipeline;
		}
	}

	
	/**
	 * Essential part for the client/mix fairness system. Used by the
	 * OrderedMemoryAwareThreadPoolExecutor. All messages of the (an) other mix
	 * are weighted by 1. All user messages are weighted by
	 * _maxReadsPreferMixFactor set in the options. The mix is by that allowed
	 * to forward _maxReadsPreferMixFactor times more messages at once from a
	 * fellow mix, than it is from a client. Since the maximum memory size per
	 * channel is _maxReadsInARow * _maxReadsPreferMixFactor, exactly
	 * _maxReadsInARow client messages will fit, before the channel is put on
	 * hold.
	 */
	private class gMixMessageSizeEstimator implements ObjectSizeEstimator {
		@Override
		public int estimateSize(Object o) {
			ChannelUpstreamEventRunnable run = (ChannelUpstreamEventRunnable) o;
			if (run.unwrap() instanceof MessageEvent) {
				if (run.getContext().getChannel().equals(_exampleMixChannel))
					return 1;
				return _maxReadsPreferMixFactor;
			} else
				return 0;
		}
	}

	
	/**
	 * Netty magic. Reads the current fragmented packets until there is enough
	 * data to reconstruct a whole message. The Event will be held at the
	 * ReplayingDecoder state of the Pipeline until there is a full message
	 * available. As soon as there is, it will be relayed to the FrameForwarder.
	 */
	private class ReplayingDecoderImplemenatation extends
			ReplayingDecoder<States> {

		@Override
		protected Object decode(ChannelHandlerContext ctx, Channel channel,
				ChannelBuffer buffer, States state) {
			return buffer.readBytes(buffer.readInt());
		}
	}
	

	/**
	 * Forwards the messages, that have been decoded by the ReplayingDecoder to
	 * the RequestInputQueue and handles user assignments.
	 */
	private class FrameForwarder extends SimpleChannelHandler {

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

			User user;
			if (e.getChannel().getAttachment() == null) { // If channel has not
															// been marked yet,
															// generate a new
															// user for it and
															// mark it.
				user = userDatabase.generateUser();
				userDatabase.addUser(user);
				e.getChannel().setAttachment(true);
				_userMap.put(e.getChannel(), user);
				_channelMap.put(user, e.getChannel());
				if (_exampleMixChannel == null)
					_exampleMixChannel = e.getChannel();
			} else { // If it has been marked already, get the related user.
				user = _userMap.get(e.getChannel());
			}

			// Use Nettys ChannelBuffer to read the data from the decoded
			// packet.
			// If the message is longer than the allowed length, an error
			// message
			// will be printed to the console.
			ChannelBuffer buf = (ChannelBuffer) e.getMessage();
			byte[] upBuf = new byte[buf.readableBytes()];
			buf.readBytes(upBuf);
			if (upBuf.length > _maxRequestLength) {
				System.err.println("warning: user " + user + " sent a too large message");
			}

			// Forward the message with the related user
			Request r = MixMessage.getInstanceRequest(upBuf, user);
			anonNode.putInRequestInputQueue(r);
		}
	}

	
	/**
	 * NOT USED Can be used for further optimization of message decoding by the
	 * ReplayingDecoder. Saves the computation of 4 bytes per message
	 * fragmentation with checkpoints, and thus should not have a big impact on
	 * performance in our case.
	 */
	private enum States {
		LENGTH, PAYLOAD;
	}

	
	/**
	 * Sends out Reply messages as fast as possible to the related Netty
	 * channel. Nettys built-in read/write fairness handles the read/write
	 * ratio.
	 * 
	 */
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

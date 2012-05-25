package framework.core.socket.socketInterfaces;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;

import framework.core.AnonNode;
import framework.core.config.Settings;
import framework.core.controller.Layer1NetworkClientController;
import framework.core.controller.Layer2RecodingSchemeClientController;
import framework.core.controller.Layer3OutputStrategyClientController;
import framework.core.interfaces.Layer1NetworkClient;
import framework.core.interfaces.Layer2RecodingSchemeClient;
import framework.core.interfaces.Layer3OutputStrategyClient;
import framework.core.message.Request;
import framework.core.interfaces.ThreePhaseStart;



//TODO: ioexception instead of throw new RuntimeException(where possible); 
//TODO: extends implementation ? (layer5clientimpl)
public abstract class AdaptiveAnonSocket implements AnonSocket, AnonSocketOptions, AnonSocketAddressData {

	protected final static int NOT_SET = -1;
	
	protected int endToEndPseudonym = NOT_SET;
	protected int sourcePseudonym = NOT_SET;
	protected int sourcePort = NOT_SET;
	protected int destinationPseudonym = NOT_SET;
	protected int destinationPort = NOT_SET;
	
	protected boolean isDuplex = false;
	protected CommunicationMode communicationMode = null;
	protected boolean isConnectionBased = false;
	protected boolean isReliable = false;
	protected boolean isOrderPreserving = false;
	protected boolean isFreeRoute = false;
	protected boolean isClientSideSocket;
	
	protected Layer1NetworkClient layer1;
	protected Layer2RecodingSchemeClient layer2;
	protected Layer3OutputStrategyClient layer3;
	protected AnonNode owner;
	protected Settings settings;
	
	protected ArrayBlockingQueue<AnonMessage> receivedDatagrams = null;
	protected ArrayBlockingQueue<Request> receivedRequests = null;
	
	
	// client-side
	public AdaptiveAnonSocket(
			AnonNode owner,
			CommunicationMode communicationMode,
			boolean isConnectionBased, 
			boolean isReliable, 
			boolean isOrderPreserving, 
			boolean isFreeRoute
			) {
		
		this.isClientSideSocket = true;
		init(owner, communicationMode, isConnectionBased, isReliable, isOrderPreserving, isFreeRoute);
		
		if (!owner.LAYER_1_LINKS_MESSAGES) {
			this.endToEndPseudonym = new Random().nextInt(); // TODO
			this.sourcePseudonym = endToEndPseudonym;
			this.sourcePort = endToEndPseudonym;
		}
		
		Layer1NetworkClientController layer1controller = owner.getNetworkLayerControllerClient();
		this.layer1 = layer1controller.loadClientPluginInstance();
		Layer2RecodingSchemeClientController layer2controller = owner.getRecodingLayerControllerClient();
		this.layer2 = layer2controller.loadClientPluginInstance();
		Layer3OutputStrategyClientController layer3controller = owner.getOutputStrategyLayerControllerClient();
		this.layer3 = layer3controller.loadClientPluginInstance();
		
		// three phase start:
		this.layer1.setReferences(layer1, layer2, layer3);
		this.layer2.setReferences(layer1, layer2, layer3);
		this.layer3.setReferences(layer1, layer2, layer3);
		((ThreePhaseStart)this.layer1).constructor();
		((ThreePhaseStart)this.layer2).constructor();
		((ThreePhaseStart)this.layer3).constructor();
		((ThreePhaseStart)this.layer1).initialize();
		((ThreePhaseStart)this.layer2).initialize();
		((ThreePhaseStart)this.layer3).initialize();
		((ThreePhaseStart)this.layer1).begin();
		((ThreePhaseStart)this.layer2).begin();
		((ThreePhaseStart)this.layer3).begin();
	}
	
	
	// server/mix-side
	public AdaptiveAnonSocket(
			AnonNode owner,
			int endToEndPseudonym,
			CommunicationMode communicationMode,
			boolean isConnectionBased, 
			boolean isReliable, 
			boolean isOrderPreserving, 
			boolean isFreeRoute
			) {
		
		if (!owner.IS_MIX)
			throw new RuntimeException("this constructor is only for mixes"); 
		this.isClientSideSocket = false;
		
		init(owner, communicationMode, isConnectionBased, isReliable, isOrderPreserving, isFreeRoute);
		
		this.endToEndPseudonym = endToEndPseudonym;
		this.destinationPseudonym = endToEndPseudonym;
		this.sourcePseudonym = endToEndPseudonym;
		
		this.receivedDatagrams = new ArrayBlockingQueue<AnonMessage>(owner.SOCKET_MIX_BACKEND_QUEUE_SIZE);
		this.receivedRequests = new ArrayBlockingQueue<Request>(owner.SOCKET_MIX_BACKEND_QUEUE_SIZE);
	}
	
	
	private void init(
			AnonNode owner,
			CommunicationMode communicationMode,
			boolean isConnectionBased, 
			boolean isReliable, 
			boolean isOrderPreserving, 
			boolean isFreeRoute
			) {
		
		if (communicationMode == CommunicationMode.DUPLEX && !owner.IS_DUPLEX)
			throw new RuntimeException("the current plug-in config does not suport duplex sockets");
		if (isConnectionBased && !owner.IS_CONNECTION_BASED)
			throw new RuntimeException("the current plug-in config does not suport connection-based sockets"); 
		if (isReliable && !owner.IS_RELIABLE)
			throw new RuntimeException("the current plug-in config does not suport reliable transfer"); 
		if (isOrderPreserving && !owner.IS_ORDER_PRESERVING)
			throw new RuntimeException("the current plug-in config does not suport order-preserving transfer"); 
		
		this.owner = owner;
		this.settings = owner.getSettings();
		this.communicationMode = communicationMode;
		this.isConnectionBased = isConnectionBased;
		this.isReliable = isReliable;
		this.isOrderPreserving = isOrderPreserving;
		this.isFreeRoute = isFreeRoute;
		if (communicationMode == CommunicationMode.DUPLEX)
			this.isDuplex = true;
	}


	public String toString() {
		String s = "";
		s += "endToEndPseudonym: " +endToEndPseudonym +"\n";
		s += "sourcePseudonym: " +sourcePseudonym +"\n";
		s += "sourcePort: " +sourcePort +"\n";
		s += "destinationPseudonym: " +destinationPseudonym +"\n";
		s += "destinationPort: " +destinationPort +"\n";
		s += "communicationMode: " +communicationMode +"\n";
		s += "isReliable: " +isReliable +"\n";
		s += "isOrderPreserving: " +isOrderPreserving +"\n";
		s += "isFreeRoute: " +isOrderPreserving +"\n";
		s += "isFreeRoute: " +isFreeRoute +"\n";
		return s;
	}


	public void newIncomingMessage(Request message) {
		if (isClientSideSocket)
			throw new RuntimeException("this method is only available on server-side plugins"); 
		try {
			receivedRequests.put(message);
		} catch (InterruptedException e) {
			e.printStackTrace();
			newIncomingMessage(message);
		}
	}
	
	
	public Request getNextRequest() {
		if (isClientSideSocket)
			throw new RuntimeException("this method is only available on server-side plugins"); 
		try {
			return receivedRequests.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return getNextRequest();
		}
	}
	
	
	// returns a request only if its size is smaller than maxSize
	public Request getNextRequest(int maxSize) {
		if (isClientSideSocket)
			throw new RuntimeException("this method is only available on server-side plugins"); 
		try {
			Request nextRequest = receivedRequests.peek();
			if (nextRequest == null || nextRequest.getByteMessage().length > maxSize)
				return null;
			else
				return receivedRequests.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return getNextRequest();
		}
	}
	
	
	public int availableRequests() {
		if (isClientSideSocket)
			throw new RuntimeException("this method is only available on server-side plugins"); 
		return receivedRequests.size();
	}
	
	
	public int sizeOfNextRequest() {
		if (isClientSideSocket)
			throw new RuntimeException("this method is only available on server-side plugins"); 
		Request next = receivedRequests.peek();
		if (next == null)
			return 0;
		else 
			return next.getByteMessage().length;
	}
	
	
	public int availableData() {
		if (isClientSideSocket)
			throw new RuntimeException("this method is only available on server-side plugins"); 
		int result = 0;
		Iterator<Request> i = receivedRequests.iterator();
		while (i.hasNext())
			result += i.next().getByteMessage().length;
		return result;
	}
	
	
	// AnonSocketOptions
	@Override
	public boolean getIsConnectionBased() {
		return this.isConnectionBased;
	}


	// AnonSocketOptions
	@Override
	public boolean getIsReliable() {
		return this.isReliable;
	}


	// AnonSocketOptions
	@Override
	public boolean getIsOrderPreserving() {
		return this.isOrderPreserving;
	}


	// AnonSocketOptions
	@Override
	public boolean getIsFreeRouteSocket() {
		return this.isFreeRoute;
	}


	// AnonSocketOptions
	@Override
	public boolean getIsDuplex() {
		return this.isDuplex;
	}


	// AnonSocketOptions
	@Override
	public CommunicationMode getCommunicationMode() {
		return this.communicationMode;
	}


	// AnonSocketAddressData
	@Override
	public int getDestinationPort() {
		return this.destinationPort;
	}


	// AnonSocketAddressData
	@Override
	public AnonNode getOwner() {
		return this.owner;
	}


	// AnonSocketAddressData
	@Override
	public int getEndToEndPseudonym() {
		assert endToEndPseudonym != NOT_SET;
		return this.endToEndPseudonym;
	}


	// AnonSocketAddressData
	@Override
	public int getSourcePseudonym() {
		return this.sourcePseudonym;
	}


	// AnonSocketAddressData
	@Override
	public int getSourcePort() {
		return this.sourcePort;
	}


	// AnonSocketAddressData
	@Override
	public int getDestinationPseudonym() {
		return this.destinationPseudonym;
	}
	
}
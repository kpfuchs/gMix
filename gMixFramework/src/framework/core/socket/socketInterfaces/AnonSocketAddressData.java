package framework.core.socket.socketInterfaces;

import framework.core.AnonNode;


public interface AnonSocketAddressData {

	public int getEndToEndPseudonym();
	public int getSourcePseudonym();
	public int getSourcePort();
	public int getDestinationPseudonym();
	public int getDestinationPort();
	
	public AnonNode getOwner();
	
}

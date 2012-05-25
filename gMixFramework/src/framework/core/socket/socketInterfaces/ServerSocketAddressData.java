package framework.core.socket.socketInterfaces;

import framework.core.AnonNode;


public interface ServerSocketAddressData {

	public int getBindPseudonym();
	public int getBindPort();
	public AnonNode getOwner();
	
}

package framework.core.socket.socketInterfaces;


public interface ConnectedDatagramAnonServerSocket extends AnonSocketOptions, ServerSocketAddressData, AnonServerSocket {

	public ConnectedDatagramAnonSocketMix accept();
	
}

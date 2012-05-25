package framework.core.socket.socketInterfaces;


public interface StreamAnonServerSocket extends AnonServerSocket, AnonSocketOptions, ServerSocketAddressData {

	public StreamAnonSocketMix accept();
	
}

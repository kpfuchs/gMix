package framework.core.socket.socketInterfaces;



public interface DatagramAnonServerSocket extends AnonServerSocket, AnonSocketOptions, ServerSocketAddressData {

	public AnonMessage receiveMessage();
	public void sendMessage(AnonMessage message); // not available if socket is simplex
	public int getMaxSizeForNextMessageSend(); // not available if socket is simplex
	public int getMaxSizeForNextMessageReceive();
}

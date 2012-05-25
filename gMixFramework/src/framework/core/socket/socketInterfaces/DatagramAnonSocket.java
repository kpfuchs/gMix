package framework.core.socket.socketInterfaces;



public interface DatagramAnonSocket extends AnonSocket {

	public void sendMessage(int destPort, byte[] payload); // only available if fixed route socket
	public void sendMessage(int destinationPseudonym, int destPort, byte[] payload); // only available if free route socket
	public int getMaxSizeForNextMessageSend();
	public AnonMessage receiveMessage(); // may be not available if the implementing socket is simplex
	public int getMaxSizeForNextMessageReceive();
	
}

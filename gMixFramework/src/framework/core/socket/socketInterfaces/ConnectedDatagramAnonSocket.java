package framework.core.socket.socketInterfaces;



public interface ConnectedDatagramAnonSocket extends AnonSocket {

	public void connect(int destinationPort); // only available if fixed route socket
	public void connect(int destinationPseudonym, int destinationPort); // only available if free route socket
	public void disconnect();
	public boolean isConnected();
	
	public void sendMessage(byte[] payload);
	public byte[] receiveMessage(); // may be not available if the implementing socket is simplex
	public int getMaxSizeForNextMessageSend();
	public int getMaxSizeForNextMessageReceive(); // may be not available if the implementing socket is simplex

}

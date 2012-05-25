package framework.core.socket.socketInterfaces;

import java.io.IOException;


public interface ConnectedDatagramAnonSocketMix extends AnonSocket {
	
	public void disconnect() throws IOException;
	public boolean isConnected();
	
	public byte[] receiveMessage() throws IOException;
	public void sendMessage(byte[] payload) throws IOException;  // may be not available if the implementing socket is simplex
	public int getMaxSizeForNextMessageSend() throws IOException;
	public int getMaxSizeForNextMessageReceive() throws IOException; // may be not available if the implementing socket is simplex

}

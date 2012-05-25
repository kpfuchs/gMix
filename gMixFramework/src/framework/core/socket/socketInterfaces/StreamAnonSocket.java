package framework.core.socket.socketInterfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;



public interface StreamAnonSocket extends AnonSocket {

	public void connect(int destinationPort) throws IOException ; // only available if fixed route socket
	public void connect(int destinationPseudonym, int destinationPort) throws IOException ; // only available if free route socket
	public void disconnect() throws IOException ;
	public boolean isConnected();
	
	public OutputStream getOutputStream() throws IOException ;
	public InputStream getInputStream() throws IOException ; // may be not available if the implementing socket is simplex
	
}

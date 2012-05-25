package framework.core.socket.socketInterfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public interface StreamAnonSocketMix extends AnonSocket {

	public void disconnect() throws IOException ;
	public boolean isConnected();
	
	public OutputStream getOutputStream(); // may be not available if the implementing socket is simplex
	public InputStream getInputStream(); 
	
}

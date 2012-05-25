package framework.core.socket.socketInterfaces;

public interface AnonSocketOptions {

	public enum CommunicationMode {DUPLEX, SIMPLEX_SENDER, SIMPLEX_RECEIVER}
	
	public boolean getIsConnectionBased();
	public boolean getIsReliable();
	public boolean getIsOrderPreserving();
	public boolean getIsFreeRouteSocket(); // only free route sockets support destination addresses (for connect() or send() methods)
	public boolean getIsDuplex();
	public CommunicationMode getCommunicationMode();
	
}

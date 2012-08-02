package evaluation.loadGenerator.applicationLevelTraffic.requestReply;

public class EndOfFileReachedException extends Exception {
	
	
	/**The serialVersionUID as identifier for this serializable class*/
	private static final long serialVersionUID = 348520384570234958L;
	
	
	/**
	 * Constructs a EndOfFileReachedException (empty constructor).
	 */
	public EndOfFileReachedException() {
		
	}
	
	
	/**
	 * Returns the String "End of file reched".
	 * @return "End of file reched"
	 */
	public String getMessage() {
		return "End of file reched";
	}
	
	
	/**
	 * Returns the String "EndOfFileReachedException".
	 * @return "EndOfFileReachedException"
	 */
	public String toString() {
		return "EndOfFileReachedException";

	}
}

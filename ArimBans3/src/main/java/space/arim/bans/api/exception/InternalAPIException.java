package space.arim.bans.api.exception;

abstract public class InternalAPIException extends Exception {

	private static final long serialVersionUID = 5105015608081031953L;

	public InternalAPIException(String message) {
		super(message);
	}
	
	public InternalAPIException(String message, Exception cause) {
		super(message, cause);
	}
	
}

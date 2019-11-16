package space.arim.bans.api.exception;

public class FetcherException extends InternalAPIException {

	private static final long serialVersionUID = -1621619586735818392L;
	
	public FetcherException(String message, Exception cause) {
		super("Fetcher error: " + message, cause);
	}
	
	public FetcherException(String message) {
		super("Fetcher error: " + message);
	}

}

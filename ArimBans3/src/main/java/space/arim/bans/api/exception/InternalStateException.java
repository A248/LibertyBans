package space.arim.bans.api.exception;

public class InternalStateException extends IllegalStateException {

	private static final long serialVersionUID = -1942160906591056894L;
	
	public InternalStateException(String message) {
		super(message);
	}
	
	public InternalStateException(String message, Class<?> clazz) {
		super(message + " for class " + clazz.getSimpleName());
	}

	public InternalStateException(String message, Exception cause) {
		super(message, cause);
	}
	
	public InternalStateException(String message, Exception cause, Class<?> clazz) {
		super(message + " for class " + clazz.getSimpleName(), cause);
	}

}

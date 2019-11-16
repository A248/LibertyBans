package space.arim.bans.api.exception;

public class InvalidSubjectException extends InternalStateException {

	private static final long serialVersionUID = 5864870266394935646L;

	/**
     * Constructs an <code>InvalidSubjectException</code> with the
     * specified detail message.
     *
     * @param   s   the detail message.
     */
	public InvalidSubjectException(String s) {
		super(s);
	}
	
	/**
     * Constructs an <code>InvalidSubjectException</code> with the
     * specified detail message and cause
     *
     * @param   s      the detail message.
     * @param   cause  the cause
     */
	public InvalidSubjectException(String s, Exception cause) {
		super(s, cause);
	}
}

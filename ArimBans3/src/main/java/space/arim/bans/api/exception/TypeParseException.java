package space.arim.bans.api.exception;

public class TypeParseException extends InternalStateException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3151919408451576826L;

	/**
     * Constructs a <code>TypeParseException</code> with the
     * specified input. Used when subject cannot be parsed or serialised.
     *
     * @param   s   the input to be parsed.
     */
	public TypeParseException(String s, Class<?> clazz) {
		super("Could not parse " + s + " as " + clazz);
	}
}

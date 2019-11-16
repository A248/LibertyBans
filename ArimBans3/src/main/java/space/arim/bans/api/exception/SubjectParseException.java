package space.arim.bans.api.exception;

import space.arim.bans.api.Subject;

public class SubjectParseException extends TypeParseException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2473249423167381391L;

	/**
     * Constructs a <code>SubjectParseException</code> with the
     * specified input. Used when subject cannot be parsed or serialised.
     *
     * @param   s   the input to be parsed.
     */
	public SubjectParseException(String s) {
		super(s, Subject.class);
	}
}

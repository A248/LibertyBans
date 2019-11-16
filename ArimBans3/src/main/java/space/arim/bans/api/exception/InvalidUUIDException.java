package space.arim.bans.api.exception;

import java.util.UUID;

public class InvalidUUIDException extends InternalStateException {
	
	private static final long serialVersionUID = 3047495662110600474L;
	
	/**
     * Constructs an <code>InvalidUUIDException</code> with the
     * specified UUID. Used when UUID does not match to any player.
     *
     * @param   uuid   the uuid.
     */
	public InvalidUUIDException(UUID uuid) {
		this("UUID " + uuid.toString() + " does not match any player!");
	}
	
	/**
     * Constructs an <code>InvalidUUIDException</code> with the
     * specified UUID and cause. Used when UUID does not match to any player.
     *
     * @param   uuid   the UUID
     * @param   cause  the cause
     */
	public InvalidUUIDException(UUID uuid, Exception cause) {
		this("UUID " + uuid.toString() + " does not match any player!", cause);
	}
	
	/**
     * Constructs an <code>InvalidUUIDException</code> with the
     * specified detail message.
     *
     * @param   message   the detail message.
     */
	public InvalidUUIDException(String message) {
		super(message);
	}
	
	/**
     * Constructs an <code>InvalidUUIDException</code> with the
     * specified detail message and cause.
     *
     * @param   message  the detail message.
     * @param   cause    the cause
     */
	public InvalidUUIDException(String message, Exception cause) {
		super(message, cause);
	}

}

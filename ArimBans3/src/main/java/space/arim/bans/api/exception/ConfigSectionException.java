package space.arim.bans.api.exception;

public class ConfigSectionException extends InternalStateException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3818324615465756966L;
	
	/**
     * Constructs a <code>ConfigSectionException</code> with the
     * specified YAML key.
     *
     * @param   key   the config section key.
     */
	public ConfigSectionException(String key) {
		super("Error in config section " + key + ".");
	}
	
	/**
     * Constructs a <code>ConfigSectionException</code> with the
     * specified YAML key and cause
     *
     * @param   key    the config section key.
     * @param   cause  the exception for this exception
     */
	public ConfigSectionException(String key, Exception cause) {
		super("Error in config section " + key + ".", cause);
	}
}

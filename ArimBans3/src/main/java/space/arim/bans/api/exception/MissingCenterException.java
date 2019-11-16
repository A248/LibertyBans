package space.arim.bans.api.exception;

public class MissingCenterException extends InternalStateException {

	private static final long serialVersionUID = 2710517627701613654L;

	public MissingCenterException(String message) {
		super("Because space.arim.bans.ArimBans was null, the following failed: " + message);
	}

}

package space.arim.bans.api.exception;

import java.util.UUID;

public class MissingCacheException extends InternalAPIException {

	private static final long serialVersionUID = 266053968014366060L;

	public MissingCacheException(UUID playeruuid) {
		super("Player " + playeruuid.toString() + " is not cached.");
	}
	
	public MissingCacheException(String name) {
		super("Player " + name + " is not cached.");
	}

}

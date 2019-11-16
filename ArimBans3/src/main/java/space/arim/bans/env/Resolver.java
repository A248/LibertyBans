package space.arim.bans.env;

import java.util.UUID;

import space.arim.bans.api.exception.PlayerNotFoundException;
import space.arim.bans.internal.Configurable;

public interface Resolver extends AutoCloseable, Configurable {
	
	public UUID uuidFromName(final String name) throws PlayerNotFoundException;
	
	public String nameFromUUID(final UUID playeruuid) throws PlayerNotFoundException;
}

package space.arim.bans.api.exception;

import java.util.UUID;

public class PlayerNotFoundException extends InternalAPIException {

	
	private static final long serialVersionUID = 1659138619614361712L;
	
	public PlayerNotFoundException(UUID playeruuid) {
		super("Player by uuid " + playeruuid.toString() + " could not be found through the cache, internal Bukkit API, nor external Mojang API.");
	}
	public PlayerNotFoundException(String name) {
		super("Player by name " + name + " could not be found through the cache, internal Bukkit API, nor external Mojang API.");
	}

}

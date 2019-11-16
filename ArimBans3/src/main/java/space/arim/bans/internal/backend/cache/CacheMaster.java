package space.arim.bans.internal.backend.cache;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.UUID;

import space.arim.bans.api.exception.MissingCacheException;
import space.arim.bans.internal.Replaceable;

public interface CacheMaster extends Replaceable {
	
	ArrayList<String> getIps(UUID playeruuid);
	
	String getName(UUID playeruuid) throws MissingCacheException;
	
	UUID getUUID(String name) throws MissingCacheException;
	
	void update(UUID playeruuid, String name, String ip);
	
	boolean uuidExists(UUID uuid);
	
	boolean hasIp(UUID playeruuid, String ip);
	
	void loadAll(ResultSet data);
}

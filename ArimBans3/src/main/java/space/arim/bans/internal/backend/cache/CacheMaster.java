package space.arim.bans.internal.backend.cache;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.UUID;

import space.arim.bans.api.exception.MissingCacheException;
import space.arim.bans.internal.Replaceable;

public interface CacheMaster extends Replaceable {
	
	public ArrayList<String> getIps(UUID playeruuid);
	
	public String getName(UUID playeruuid) throws MissingCacheException;
	
	public UUID getUUID(String name) throws MissingCacheException;
	
	public void update(UUID playeruuid, String name, String ip);
	
	public boolean hasIp(UUID playeruuid, String ip);
	
	public void loadAll(ResultSet data);
	
	public void saveAll();
}

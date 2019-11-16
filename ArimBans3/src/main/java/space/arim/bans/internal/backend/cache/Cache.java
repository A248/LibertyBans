package space.arim.bans.internal.backend.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import space.arim.bans.ArimBans;
import space.arim.bans.api.exception.InvalidUUIDException;
import space.arim.bans.api.exception.MissingCacheException;
import space.arim.bans.internal.sql.SqlQuery;

public class Cache implements CacheMaster {
	private ArimBans center;
	private ConcurrentHashMap<UUID, ArrayList<String>> ips = new ConcurrentHashMap<UUID, ArrayList<String>>();
	private ConcurrentHashMap<UUID, String> uuids = new ConcurrentHashMap<UUID, String>();

	public Cache(ArimBans center) {
		this.center = center;
		refreshConfig();
	}

	@Override
	public void loadAll(ResultSet data) {
		try {
			while (data.next()) {
				UUID uuid;
				try {
					uuid = UUID.fromString(data.getString("uuid"));
					uuids.put(uuid, data.getString("name"));
					ips.put(uuid, internaliseIps(data.getString("iplist")));
				} catch (IllegalArgumentException ex) {
					center.logError(ex);
				}
			}
		} catch (SQLException ex) {
			center.logError(ex);
		}
	}
	
	private String externaliseIps(ArrayList<String> iplist) {
		if (iplist.isEmpty()) {
			return "<empty>";
		}
		String list = null;
		for (String s : iplist) {
			if (list != null) {
				list = list.concat(",").concat(s);
			} else {
				list = s;
			}
		}
		return list;
	}

	private ArrayList<String> internaliseIps(String input) {
		return (input.equalsIgnoreCase("<empty>")) ? new ArrayList<String>() : new ArrayList<String>(Arrays.asList(input.split(",")));
	}

	@Override
	public ArrayList<String> getIps(UUID playeruuid) {
		if (ips.containsKey(playeruuid)) {
			return ips.get(playeruuid);
		}
		throw new InvalidUUIDException(playeruuid);
	}
	
	@Override
	public String getName(UUID playeruuid) throws MissingCacheException {
		if (uuids.containsKey(playeruuid)) {
			return uuids.get(playeruuid);
		}
		throw new MissingCacheException(playeruuid);
	}
	
	@Override
	public UUID getUUID(String name) throws MissingCacheException {
		for (UUID id : uuids.keySet()) {
			if (uuids.get(id).equalsIgnoreCase(name)) {
				return id;
			}
		}
		throw new MissingCacheException("Player " + name + " does not exist!");
	}
	
	@Override
	public void update(UUID playeruuid, String name, String ip) {
		if (uuids.containsKey(playeruuid)) {
			if (!uuids.get(playeruuid).equalsIgnoreCase(name) || !ips.get(playeruuid).contains(ip)) {
				center.async().execute(() -> {
					if (!uuids.get(playeruuid).equalsIgnoreCase(name)) {
						uuids.replace(playeruuid, name);
						center.sql().executeQuery(SqlQuery.Query.UPDATE_NAME_FOR_UUID.eval(center.sql().mode()), name, System.currentTimeMillis(), playeruuid.toString());
					}
					if (!ips.get(playeruuid).contains(ip)) {
						ArrayList<String> list = ips.get(playeruuid);
						if (ip != null) {
							list.add(ip);
						}
						ips.replace(playeruuid, list);
						center.sql().executeQuery(SqlQuery.Query.UPDATE_IPS_FOR_UUID.eval(center.sql().mode()), externaliseIps(list), System.currentTimeMillis(), playeruuid.toString());
					}
				});
			}
		} else {
			center.async().execute(() -> {
				ArrayList<String> list = new ArrayList<String>();
				if (ip != null) {
					list.add(ip);
				}
				uuids.put(playeruuid, name);
				ips.put(playeruuid, list);
				center.sql().executeQuery(SqlQuery.Query.INSERT_CACHE.eval(center.sql().mode()), playeruuid.toString(), name, externaliseIps(list), System.currentTimeMillis(), System.currentTimeMillis());
			});
		}
	}

	@Override
	public boolean hasIp(UUID playeruuid, String ip) {
		if (ips.containsKey(playeruuid)) {
			if (ips.get(playeruuid).contains(ip)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void saveAll() {
		
	}
	
	@Override
	public void close() {
		ips.clear();
		uuids.clear();
		ips = null;
		uuids = null;
	}

	@Override
	public void refreshConfig() {
		
	}

}

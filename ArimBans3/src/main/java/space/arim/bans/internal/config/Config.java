package space.arim.bans.internal.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import space.arim.bans.ArimBans;
import space.arim.bans.api.exception.ConfigSectionException;

public class Config implements ConfigMaster {
	private ArimBans center;
	private static final Map<String, Object> defaults = Collections.unmodifiableMap(defaults());
	private ConcurrentHashMap<String, Object> config = new ConcurrentHashMap<String, Object>();

	public Config(ArimBans center) {
		this.center = center;
		refreshConfig();
	}

	private static ConcurrentHashMap<String, Object> defaults() {
		ConcurrentHashMap<String, Object> defaults = new ConcurrentHashMap<String, Object>();
		defaults.put("storage.mode", "mysql");
		defaults.put("storage.min-connections", "2");
		defaults.put("storage.max-connections", "5");
		defaults.put("storage.mysql.host", "localhost");
		defaults.put("storage.mysql.port", "3306");
		defaults.put("storage.mysql.user", "ArimBans");
		defaults.put("storage.mysql.database", "arimbans");
		defaults.put("storage.mysql.password", "defaultpassword");
		defaults.put("storage.mysql.url",
				"jdbc:mysql://<ip>:<port>/<database>?autoReconnect=true&useUnicode=true&characterEncoding=utf8");
		defaults.put("storage.hsqldb.url", "jdbc:hsqldb:file:<file>");
		defaults.put("formatting.use-json", "true");
		defaults.put("formatting.permanent-display", "Permanently");
		defaults.put("formatting.console-display", "Console");
		defaults.put("formatting.date", "dd/MM/yyyy HH:mm:ss");
		defaults.put("bans.event-priority", "HIGHEST");
		defaults.put("mutes.event-priority", "HIGHEST");
		defaults.put("fetchers.internal", "true");
		defaults.put("fetchers.ashcon", "true");
		defaults.put("fetchers.mojang", "true");
		return defaults;
	}
	
	@Override
	public void refreshConfig() {
		for (HashMap.Entry<String, Object> entry : defaults.entrySet()) {
			config.put(entry.getKey(), entry.getValue());
		}
	}

	private void warning(String message) {
		center.log(message);
		center.environment().logger().warning(message);
	}
	
	private void configWarning(String key, Class<?> type) {
		warning("Configuration " + key + " does not map to a " + type.getSimpleName() + "!");
	}
	
	@Override
	public String getString(String key) {
		if (config.containsKey(key)) {
			Object obj = config.get(key);
			if (obj instanceof String) {
				return (String) obj;
			}
			configWarning(key, String.class);
		}
		return (String) defaults.get(key);
	}

	@Override
	public String[] getStrings(String key) {
		if (config.containsKey(key)) {
			Object obj = config.get(key);
			if (obj instanceof String[]) {
				return (String[]) obj;
			}
			configWarning(key, String[].class);
		}
		return (String[]) defaults.get(key);
	}
	
	@Override
	public void close() {
		config.clear();
	}

	@Override
	public boolean parseBoolean(String key) {
		switch (getString(key).toLowerCase()) {
		case "true":
			return true;
		case "yes":
			return true;
		case "false":
			return false;
		case "no":
			return false;
		default:
			throw new ConfigSectionException(key);
		}
	}

	@Override
	public int parseInt(String key) {
		try {
			return Integer.parseInt(getString(key));
		} catch (NumberFormatException ex) {
			throw new ConfigSectionException(key, ex);
		}
	}
}

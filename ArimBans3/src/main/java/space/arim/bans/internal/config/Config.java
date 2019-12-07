/*
 * ArimBans, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.internal.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.yaml.snakeyaml.Yaml;

import space.arim.bans.ArimBans;
import space.arim.bans.api.CommandType;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.exception.ConfigLoadException;
import space.arim.bans.api.exception.InternalStateException;
import space.arim.bans.api.util.ToolsUtil;

public class Config implements ConfigMaster {
	
	private final ArimBans center;
	
	private final File configYml;
	private final File messagesYml;
	private final Map<String, Object> configDefaults;
	private final Map<String, Object> messageDefaults;
	private final ConcurrentHashMap<String, Object> configValues = new ConcurrentHashMap<String, Object>();
	private final ConcurrentHashMap<String, Object> messageValues = new ConcurrentHashMap<String, Object>();
	
	private static final String CONFIG_PATH = "config.yml";
	private static final String MESSAGES_PATH = "messages.yml";
	
	private static final int CONFIG_VERSION = 1;
	private static final int MESSAGES_VERSION = 1;

	public Config(ArimBans center) {
		this.center = center;
		this.configYml = new File(center.dataFolder(), "config.yml");
		this.messagesYml = new File(center.dataFolder(), "messages.yml");
		
		Yaml yaml = new Yaml();
		
		// Save files if nonexistent
		saveIfNotExist(configYml, CONFIG_PATH);
		saveIfNotExist(messagesYml, MESSAGES_PATH);
		
		// Load config defaults
		configDefaults = loadDefaults(CONFIG_PATH, yaml);
		messageDefaults = loadDefaults(MESSAGES_PATH, yaml);
		configValues.putAll(configDefaults);
		messageValues.putAll(messageDefaults);
		
		// Load config values
		configValues.putAll(loadFile(configYml, yaml));
		messageValues.putAll(loadFile(messagesYml, yaml));
		
		// Check config versions
		configVersion();
		messagesVersion();
	}
	
	private void saveIfNotExist(File path, String source) {
		if (!path.exists()) {
			try (InputStream output = getClass().getResourceAsStream(source)) {
				if (!ToolsUtil.saveFromStream(path, output)) {
					center.logError(new ConfigLoadException(path));
				}
			} catch (IOException ex) {
				center.logError(ex);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> loadDefaults(String source, Yaml yaml) {
		try (InputStream input = getClass().getResourceAsStream(source)) {
			return (Map<String, Object>) yaml.load(input);
		} catch (IOException ex) {
			center.logError(new ConfigLoadException("Could not load internal resource " + source, ex));
			return new HashMap<String, Object>();
		}
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> loadFile(File source, Yaml yaml) {
		try (FileReader reader = new FileReader(source)) {
			return (Map<String, Object>) yaml.load(reader);
		} catch (IOException ex) {
			center.logError(new ConfigLoadException(source, ex));
			return new HashMap<String, Object>();
		}
	}
	
	private void configVersion() {
		Object ver = configValues.get("config-version");
		if (ver instanceof Integer) {
			if ((Integer) ver == CONFIG_VERSION) {
				return;
			}
		}
		File dest = new File(center.dataFolder(), "config-backups" + File.separator + ToolsUtil.fileDateFormat() + "-config.yml");
		center.log(Level.WARNING, "Detected outdated config version. Saving old configuration to " + dest.getPath());
		configYml.renameTo(dest);
		saveIfNotExist(configYml, CONFIG_PATH);
	}
	
	private void messagesVersion() {
		Object ver = messageValues.get("messages-version");
		if (ver instanceof Integer) {
			if ((Integer) ver == MESSAGES_VERSION) {
				return;
			}
		}
		File dest = new File(center.dataFolder(), "messages-backups" + File.separator + ToolsUtil.fileDateFormat() + "-messages.yml");
		center.log(Level.WARNING, "Detected outdated config version. Saving old configuration to " + dest.getPath());
		messagesYml.renameTo(dest);
		saveIfNotExist(messagesYml, MESSAGES_PATH);
	}
	
	@Override
	public void refresh(boolean first) {
		if (!first) {
			Yaml yaml = new Yaml();
			configValues.putAll(loadFile(configYml, yaml));
			messageValues.putAll(loadFile(messagesYml, yaml));
			configVersion();
			messagesVersion();
		}
	}
	
	@Override
	public void refreshConfig(boolean first) {
		if (!first) {
			configValues.putAll(loadFile(configYml, new Yaml()));
			configVersion();
		}
	}
	
	@Override
	public void refreshMessages(boolean first) {
		if (!first) {
			messageValues.putAll(loadFile(messagesYml, new Yaml()));
			messagesVersion();
		}
	}

	private void warning(String message) {
		center.log(Level.WARNING, message);
	}
	
	private void configWarning(String key, Class<?> type, File file) {
		warning("Configuration " + key + " does not map to a " + type.getSimpleName() + " in " + file.getPath());
	}
	
	private void configWarning(String key, Class<?> type) {
		configWarning(key, type, configYml);
	}
	
	private List<String> encodeList(List<String> list) {
		for (int n = 0; n < list.size(); n++) {
			list.set(n, ToolsUtil.encode(list.get(n)));
		}
		return list;
	}
	
	@Override
	public String getConfigString(String key) {
		return ToolsUtil.encode(cfgGet(key, String.class));
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getConfigStrings(String key) {
		if (configValues.containsKey(key)) {
			Object obj = configValues.get(key);
			if (obj instanceof List<?>) {
				return encodeList((List<String>) obj);
			}
			configWarning(key, List.class);
		}
		return encodeList((List<String>) configDefaults.get(key));
	}
	
	@Override
	public boolean getConfigBoolean(String key) {
		return cfgGet(key, Boolean.class);
	}

	@Override
	public int getConfigInt(String key) {
		return cfgGet(key, Integer.class);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T cfgGet(String key, Class<T> type) {
		if (configValues.containsKey(key)) {
			Object obj = configValues.get(key);
			if (type.isInstance(obj)) {
				return (T) obj;
			}
			configWarning(key, type);
		}
		return (T) configDefaults.get(key);
	}
	
	@Override
	public String getMessagesString(String key) {
		return ToolsUtil.encode(msgGet(key, String.class));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<String> getMessagesStrings(String key) {
		if (messageValues.containsKey(key)) {
			Object obj = messageValues.get(key);
			if (obj instanceof List<?>) {
				return encodeList((List<String>) obj);
			}
			configWarning(key, List.class, messagesYml);
		}
		return (List<String>) messageDefaults.get(key);
	}
	
	@Override
	public boolean getMessagesBoolean(String key) {
		return msgGet(key, Boolean.class);
	}
	
	@Override
	public int getMessagesInt(String key) {
		return msgGet(key, Integer.class);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T msgGet(String key, Class<T> type) {
		if (messageValues.containsKey(key)) {
			Object obj = messageValues.get(key);
			if (type.isInstance(obj)) {
				return (T) obj;
			}
			configWarning(key, type, messagesYml);
		}
		return (T) messageDefaults.get(key);
	}
	
	private String leadKey(CommandType.Category category) {
		switch (category) {
		case ADD:
			return "additions.";
		case REMOVE:
			return "removals.";
		case LIST:
			return "list.";
		case OTHER:
			return "other.";
		default:
			throw new InternalStateException("What other command category is there?!?");
		}
	}
	
	@Override
	public String keyString(PunishmentType type, CommandType.Category category) {
		switch (type) {
		case BAN:
			return leadKey(category) + "bans.";
		case MUTE:
			return leadKey(category) + "mutes.";
		case WARN:
			return leadKey(category) + "warns.";
		case KICK:
			return leadKey(category) + "kicks.";
		default:
			throw new InternalStateException("What other punishment type is there?!?");
		}
	}
	
	@Override
	public File getDataFolder() {
		return center.dataFolder();
	}
	
}

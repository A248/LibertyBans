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
import java.util.Collections;
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

import space.arim.universal.util.UniversalUtil;

import space.arim.api.util.FilesUtil;
import space.arim.api.util.minecraft.MinecraftUtil;

public class Config implements ConfigMaster {
	
	private final ArimBans center;
	
	private final Map<String, Object> configDefaults;
	private final Map<String, Object> messagesDefaults;
	private final ConcurrentHashMap<String, Object> configValues = new ConcurrentHashMap<String, Object>();
	private final ConcurrentHashMap<String, Object> messagesValues = new ConcurrentHashMap<String, Object>();
	
	private static final String CONFIG_PATH = "config.yml";
	private static final String MESSAGES_PATH = "messages.yml";
	
	private static final int CONFIG_VERSION = 1;
	private static final int MESSAGES_VERSION = 1;

	public Config(ArimBans center) {
		this.center = center;
		
		// Load config defaults
		Yaml yaml = new Yaml();
		configDefaults = loadDefaults(CONFIG_PATH, yaml);
		messagesDefaults = loadDefaults(MESSAGES_PATH, yaml);
		configValues.putAll(configDefaults);
		messagesValues.putAll(messagesDefaults);
		
	}
	
	@Override
	public File getDataFolder() {
		return center.dataFolder();
	}
	
	private File saveIfNotExist(String resource) {
		File target = new File(center.dataFolder(), resource);
		if (!target.exists()) {
			try (InputStream input = Config.class.getResourceAsStream(File.separator + resource)) {
				if (FilesUtil.saveFromStream(target, input)) {
					center.logs().logBoth(Level.FINE, "Resource " + resource + " successfully extracted to " + target.getPath());
				} else {
					center.logs().logBoth(Level.WARNING, "Resource extraction to " + target.getPath() + " failed.");
				}
			} catch (IOException ex) {
				center.logs().logError(ex);
			}
		} else {
			center.logs().log(Level.FINER, "File " + target.getPath() + " exists; good!");
		}
		return target;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> loadDefaults(String source, Yaml yaml) {
		try (InputStream stream = Config.class.getResourceAsStream(File.separator + source)) {
			return (Map<String, Object>) yaml.load(stream);
		} catch (IOException ex) {
			throw new ConfigLoadException(source, ex);
		}
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> loadFile(File source, Yaml yaml) {
		try (FileReader reader = new FileReader(source)) {
			return (Map<String, Object>) yaml.load(reader);
		} catch (IOException ex) {
			center.logs().logError(new ConfigLoadException(source, ex));
		}
		return Collections.emptyMap();
	}
	
	private File checkVersion(File source, Map<String, Object> values, int version, String resource) {
		Object ver = values.get("do-not-touch-version");
		if (!(ver instanceof Integer && (Integer) ver == version)) {
			File dest = FilesUtil.dateSuffixedFile(center.dataFolder(), resource + "-", "configuration-backups");
			center.logs().logBoth(Level.WARNING, "Detected outdated " + resource + " version. Saving old configuration to " + dest.getPath());
			//center.logs().logBoth(Level.SEVERE, "*** PLEASE READ ***\n" + "Your " + resource + " version is outdated. ArimBans attempted to copy the latest configuration and save your outdated " + resource + " to a backup location. " + "However, we were unable to complete this operation, and as such, your " + resource + " remains outdated. " + Logs.PLEASE_CREATE_GITHUB_ISSUE_URL + " to address this.");
			source.renameTo(dest);
			source = saveIfNotExist(resource);
		}
		return source;
	}
	
	private void loadConfig(Yaml yaml) {
		File configYml = saveIfNotExist(CONFIG_PATH);
		configValues.putAll(loadFile(configYml, yaml));
		checkVersion(configYml, configValues, CONFIG_VERSION, CONFIG_PATH);
	}
	
	private void loadMessages(Yaml yaml) {
		File messagesYml = saveIfNotExist(MESSAGES_PATH);
		messagesValues.putAll(loadFile(messagesYml, yaml));
		checkVersion(messagesYml, messagesValues, MESSAGES_VERSION, MESSAGES_PATH);
	}
	
	@Override
	public void refresh(boolean first) {
		Yaml yaml = new Yaml();
		loadConfig(yaml);
		loadMessages(yaml);
	}
	
	@Override
	public void refreshConfig(boolean first) {
		loadConfig(new Yaml());
	}
	
	@Override
	public void refreshMessages(boolean first) {
		loadMessages(new Yaml());
	}
	
	private List<String> encodeList(List<String> list) {
		for (int n = 0; n < list.size(); n++) {
			list.set(n, MinecraftUtil.encode(list.get(n)));
		}
		return list;
	}
	
	@Override
	public String getConfigString(String key) {
		return MinecraftUtil.encode(cfgGet(key, String.class));
	}
	
	@Override
	public String getMessagesString(String key) {
		return MinecraftUtil.encode(msgsGet(key, String.class));
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getConfigStrings(String key) {
		return encodeList(cfgGet(key, List.class));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<String> getMessagesStrings(String key) {
		return encodeList(msgsGet(key, List.class));
	}
	
	@Override
	public boolean getConfigBoolean(String key) {
		return cfgGet(key, Boolean.class);
	}
	
	@Override
	public boolean getMessagesBoolean(String key) {
		return msgsGet(key, Boolean.class);
	}

	@Override
	public int getConfigInt(String key) {
		return cfgGet(key, Integer.class);
	}
	
	@Override
	public int getMessagesInt(String key) {
		return msgsGet(key, Integer.class);
	}
	
	private <T> T cfgGet(String key, Class<T> type) {
		T obj = getFromMap(configValues, key, type);
		return (obj != null) ? obj : getFromMap(configDefaults, key, type);
	}
	
	private <T> T msgsGet(String key, Class<T> type) {
		T obj = getFromMap(messagesValues, key, type);
		return (obj != null) ? obj : getFromMap(messagesDefaults, key, type);
	}
	
	private <T> T getFromMap(Map<String, Object> map, String key, Class<T> type) {
		center.logs().log(Level.FINEST, "Getting configuration key " + key);
		return UniversalUtil.getFromMapRecursive(map, key, type);
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

	
}

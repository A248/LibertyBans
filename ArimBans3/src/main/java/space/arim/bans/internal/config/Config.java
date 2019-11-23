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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.yaml.snakeyaml.Yaml;

import com.google.common.io.ByteStreams;

import space.arim.bans.ArimBans;
import space.arim.bans.api.exception.ConfigLoadException;
import space.arim.bans.api.util.Tools;

public class Config implements ConfigMaster {
	
	private final ArimBans center;
	
	private final File configYml;
	private final File messagesYml;
	private final Map<String, Object> configDefaults;
	private final Map<String, Object> messageDefaults;
	private ConcurrentHashMap<String, Object> configValues = new ConcurrentHashMap<String, Object>();
	private ConcurrentHashMap<String, Object> messageValues = new ConcurrentHashMap<String, Object>();

	public Config(ArimBans center) {
		this.center = center;
		this.configYml = new File(center.dataFolder(), "config.yml");
		this.messagesYml = new File(center.dataFolder(), "messages.yml");
		
		Yaml yaml = new Yaml();
		
		// Save files if nonexistent
		saveIfNotExist(configYml, "/src/main/resources/config.yml");
		saveIfNotExist(messagesYml, "/src/main/resources/messages.yml");
		
		// Load config defaults
		configDefaults = loadDefaults("/src/main/resources/config.yml", yaml);
		messageDefaults = loadDefaults("/src/main/resources/messages.yml", yaml);
		configValues.putAll(configDefaults);
		messageValues.putAll(messageDefaults);
		
		// Load config values
		configValues.putAll(loadFile(configYml, yaml));
		messageValues.putAll(loadFile(messagesYml, yaml));
		
		// Check config versions
		checkVersion();
	}
	
	private void saveIfNotExist(File target, String source) {
		if (!target.exists()) {
			if (!Tools.generateFile(target)) {
				ConfigLoadException exception = new ConfigLoadException(target);
				center.logError(exception);
				throw exception;
			}
			try (InputStream input = getClass().getResourceAsStream(source); FileOutputStream output = new FileOutputStream(target)) {
				ByteStreams.copy(input, output);
			} catch (IOException ex) {
				ConfigLoadException exception = new ConfigLoadException("Could not save " + target.getPath() + " from " + source, ex);
				center.logError(exception);
				throw exception;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> loadDefaults(String source, Yaml yaml) {
		try (InputStream input = getClass().getResourceAsStream(source)) {
			return (Map<String, Object>) yaml.load(input);
		} catch (IOException ex) {
			ConfigLoadException exception = new ConfigLoadException("Could not load internal resource " + source, ex);
			center.logError(exception);
			throw exception;
		}
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> loadFile(File source, Yaml yaml) {
		try (FileReader reader = new FileReader(source)) {
			return (Map<String, Object>) yaml.load(reader);
		} catch (IOException ex) {
			ConfigLoadException exception = new ConfigLoadException(source, ex);
			center.logError(exception);
			throw exception;
		}
	}
	
	private void checkVersion() {
		if (configValues.get("other.version") != configDefaults.get("other.version")) {
			configYml.delete();
			saveIfNotExist(configYml, "/src/main/resources/config.yml");
		}
		if (messageValues.get("other.version") != messageDefaults.get("other.version")) {
			messagesYml.delete();
			saveIfNotExist(messagesYml, "/src/main/resources/messages.yml");
		}
	}
	
	@Override
	public void refreshConfig() {
		Yaml yaml = new Yaml();
		configValues.putAll(loadFile(configYml, yaml));
		messageValues.putAll(loadFile(messagesYml, yaml));
		checkVersion();
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
		return get(key, String.class);
	}

	@Override
	public String[] getStrings(String key) {
		if (configValues.containsKey(key)) {
			Object obj = configValues.get(key);
			if (obj instanceof String[]) {
				return (String[]) obj;
			}
			configWarning(key, String[].class);
		}
		return (String[]) configDefaults.get(key);
	}
	
	@Override
	public boolean getBoolean(String key) {
		return get(key, Boolean.class);
	}

	@Override
	public int getInt(String key) {
		return get(key, Integer.class);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T get(String key, Class<T> type) {
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
	public String getMessage(String key) {
		if (messageValues.containsKey(key)) {
			Object obj = messageValues.get(key);
			if (obj instanceof String) {
				return (String) obj;
			}
		}
		return (String) messageDefaults.get(key);
	}
	
	@Override
	public String[] getMessages(String key) {
		return null;
	}
	
	@Override
	public void close() {

	}
}

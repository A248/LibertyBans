package space.arim.bans.internal.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.yaml.snakeyaml.Yaml;

import com.google.common.io.ByteStreams;

import space.arim.bans.ArimBans;
import space.arim.bans.api.Tools;
import space.arim.bans.api.exception.ConfigLoadException;
import space.arim.bans.api.exception.ConfigSectionException;
import space.arim.bans.api.exception.InternalStateException;

public class Config implements ConfigMaster {
	private ArimBans center;
	
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
				InternalStateException exception = new ConfigLoadException(target);
				center.logError(exception);
				throw exception;
			}
			try (InputStream input = getClass().getResourceAsStream(source); OutputStream output = new FileOutputStream(target)) {
				ByteStreams.copy(input, output);
			} catch (IOException ex) {
				InternalStateException exception = new ConfigLoadException("Could not save " + target.getPath() + " from " + source, ex);
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
			InternalStateException exception = new ConfigLoadException("Could not load internal resource " + source, ex);
			center.logError(exception);
			throw exception;
		}
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> loadFile(File source, Yaml yaml) {
		try (FileReader reader = new FileReader(source)) {
			return (Map<String, Object>) yaml.load(reader);
		} catch (IOException ex) {
			InternalStateException exception = new ConfigLoadException(source, ex);
			center.logError(exception);
			throw exception;
		}
	}
	
	private void checkVersion() {
		if (configValues.get("version") != configDefaults.get("version")) {
			configYml.delete();
			saveIfNotExist(configYml, "/src/main/resources/config.yml");
		}
		if (messageValues.get("version") != messageDefaults.get("version")) {
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
	
	private ConcurrentHashMap<String, Object> config() {
		return configValues;
	}
	
	@Override
	public String getString(String key) {
		if (config().containsKey(key)) {
			Object obj = config().get(key);
			if (obj instanceof String) {
				return (String) obj;
			}
			configWarning(key, String.class);
		}
		return (String) configDefaults.get(key);
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
	public int getInt(String key) {
		try {
			return Integer.parseInt(getString(key));
		} catch (NumberFormatException ex) {
			throw new ConfigSectionException(key, ex);
		}
	}
	
	@Override
	public String getMessage(String key) {
		return null;
	}
	
	@Override
	public String[] getMessages(String key) {
		return null;
	}
	
	@Override
	public void close() {

	}
}

/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.dazzleconf.AuxiliaryKeys;
import space.arim.dazzleconf.ConfigurationFactory;
import space.arim.dazzleconf.ConfigurationOptions;
import space.arim.dazzleconf.error.ConfigFormatSyntaxException;
import space.arim.dazzleconf.error.InvalidConfigException;
import space.arim.dazzleconf.ext.snakeyaml.CommentMode;
import space.arim.dazzleconf.ext.snakeyaml.SnakeYamlConfigurationFactory;
import space.arim.dazzleconf.ext.snakeyaml.SnakeYamlOptions;
import space.arim.omnibus.util.ThisClass;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class ConfigHolder<C> {

	private final Class<C> configClass;
	
	private volatile C instance;
	
	private static final ConfigurationOptions CONFIG_OPTIONS;
	private static final SnakeYamlOptions YAML_OPTIONS;
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	static {
		ConfigurationOptions.Builder optionsBuilder = new ConfigurationOptions.Builder()
				.setCreateSingleElementCollections(true);
		ConfigSerialisers.addTo(optionsBuilder);
		CONFIG_OPTIONS = optionsBuilder.build();

		YAML_OPTIONS = new SnakeYamlOptions.Builder().commentMode(CommentMode.alternativeWriter()).build();
	}
	
	public ConfigHolder(Class<C> configClass) {
		this.configClass = Objects.requireNonNull(configClass);
	}

	public Class<C> getConfigClass() {
		return configClass;
	}

	public C getConfigData() {
		return instance;
	}

	public CompletableFuture<ConfigResult> reload(Path path) {
		return CompletableFuture.supplyAsync(() -> {
			ConfigurationFactory<C> factory = SnakeYamlConfigurationFactory.create(configClass, CONFIG_OPTIONS, YAML_OPTIONS);
			C defaults = factory.loadDefaults();
			try {
				if (!Files.exists(path)) {
					try (FileChannel fileChannel = FileChannel.open(path,
							StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {

						factory.write(defaults, fileChannel);
					}
					this.instance = defaults;
					return ConfigResult.SUCCESS_WITH_DEFAULTS;
				}
				C instance = loadFromPath(factory, defaults, path);
				this.instance = instance;
				if (instance == defaults) {
					// loadFromPath indicates a user failure
					return ConfigResult.USER_ERROR;
				}
				return ConfigResult.SUCCESS_LOADED;

			} catch (IOException ex) {
				logger.warn("Encountered I/O error while reloading the configuration", ex);
				return ConfigResult.IO_ERROR;
			}
		});
	}

	/**
	 * Attempts to load the configuration from the specified path
	 *
	 * @param defaults the default configuration
	 * @param path the path
	 * @return the loaded configuration if everything went properly, or the default configuration if invalid
	 */
	private C loadFromPath(ConfigurationFactory<C> factory,
						   C defaults,
						   Path path) throws IOException {
		C config;
		// Load existing configuration
		try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)) {

			config = factory.load(fileChannel, defaults);

		} catch (ConfigFormatSyntaxException ex) {
			logger.warn(
					"The YAML syntax in your configuration is invalid. "
					+ "Please use a YAML validator such as https://yaml-online-parser.appspot.com/. "
					+ "Paste your configuration there and use it to work through errors. "
					+ "Run /libertybans reload when done. For now, the default configuration will be used.", ex);
			return defaults;

		} catch (InvalidConfigException ex) {
			logger.warn(
					"The values in your configuration are invalid. "
					+ "Please correct the issue and run /libertybans reload. "
					+ "For now, the default configuration will be used.", ex);
			return defaults;
		}
		if (config instanceof AuxiliaryKeys) {
			// Update existing configuration with missing keys
			try (FileChannel fileChannel = FileChannel.open(path,
					StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {

				factory.write(config, fileChannel);
			}
		}
		return config;
	}
	
}

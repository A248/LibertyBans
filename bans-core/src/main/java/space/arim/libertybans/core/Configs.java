/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;

import space.arim.api.configure.ConfigAccessor;
import space.arim.api.configure.ConfigResult;
import space.arim.api.configure.ConfigSerialiser;
import space.arim.api.configure.Configuration;
import space.arim.api.configure.JarResources;
import space.arim.api.configure.ValueTransformer;
import space.arim.api.configure.configs.ConfigurationBuilder;
import space.arim.api.configure.configs.SingularConfig;
import space.arim.api.configure.yaml.YamlConfigSerialiser;
import space.arim.api.configure.yaml.YamlSyntaxException;

import space.arim.libertybans.bootstrap.StartupException;
import space.arim.libertybans.core.database.DatabaseManager;
import space.arim.libertybans.core.database.IOUtils;

public class Configs implements Part {
	
	private final Path langFolder;
	
	private final ExecutorService readWriteService;
	
	private final SingularConfig sql;
	private final SingularConfig config;
	private final Configuration messages;
	
	private static final List<ValueTransformer> transformers;
	
	private static final Class<?> THIS_CLASS = ThisClass.get();
	private static final Logger logger = LoggerFactory.getLogger(THIS_CLASS);
	
	Configs(Path folder) {
		langFolder = folder.resolve("lang");
		readWriteService = Executors.newFixedThreadPool(1, new IOUtils.ThreadFactoryImpl("LibertyBans-Config-"));
		try {
			Files.createDirectories(langFolder);
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to create plugin directories", ex);
		}
		ConfigSerialiser serialiser = new YamlConfigSerialiser();
		var futureSql = getFor(serialiser, "sql.yml", DatabaseManager.createConfigTransformers());
		var futureConfig = getFor(serialiser, "config.yml", transformers);
		var futureMessages = getFor(serialiser, "lang/messages_en.yml", List.of());
		sql = new SingularConfig(futureSql.join(), folder.resolve("sql.yml"));
		config = new SingularConfig(futureConfig.join(), folder.resolve("config.yml"));
		messages = futureMessages.join();
	}
	
	static {
		ValueTransformer timeTransformer = (key, value) -> {
			if (key.equals("formatting.dates")) {
				DateTimeFormatter result = null;
				if (value instanceof String) {
					String timeF = (String) value;
					try {
						result = DateTimeFormatter.ofPattern(timeF);
					} catch (IllegalArgumentException ignored) {}
				}
				if (result == null) {
					//result = DateTimeFormatter.ofPattern("dd/MM/yyyy kk:mm");
					logger.info("Config option formatting.dates invalid: {}", value);
				}
				return result;
			}
			return value;
		};
		ValueTransformer strictnessTransformer = (key, value) -> {
			if (key.equals("enforcement.address-strictness")) {
				AddressStrictness result = null;
				if (value instanceof String) {
					String addrS = (String) value;
					try {
						result = AddressStrictness.valueOf(addrS);
					} catch (IllegalArgumentException ignored) {}
				}
				if (result == null) {
					//result = AddressStrictness.NORMAL;
					logger.info("Config option enforcement.address-strictness invalid: {}", value);
				}
				return result;
			}
			return value;
		};
		transformers = List.of(timeTransformer, strictnessTransformer);
	}
	
	private CompletableFuture<? extends Configuration> getFor(ConfigSerialiser serialiser, String resourceName,
			List<ValueTransformer> transformers) {
		return new ConfigurationBuilder()
				.defaultResource(JarResources.forClassLoader(THIS_CLASS.getClassLoader(), resourceName))
				.executor(readWriteService).serialiser(serialiser)
				.addTransformers(transformers).buildMergingConfig();
	}
	
	@Override
	public void startup() {
		reloadConfigsOrInform();
	}
	
	@Override
	public void restart() {
		reloadConfigsOrInform();
	}
	
	@Override
	public void shutdown() {
		readWriteService.shutdown();
	}
	
	private void reloadConfigsOrInform() {
		if (!reloadConfigs().join()) {
			throw new StartupException("Issue while reloading configuration");
		}
	}
	
	public CompletableFuture<Boolean> reloadConfigs() {
		Set<CompletableFuture<?>> futureLangFiles = new HashSet<>();
		for (Translation translation : Translation.values()) {
			String name = translation.name().toLowerCase(Locale.ENGLISH);
			Path messagesPath = langFolder.resolve("messages_" + name + ".yml");
			if (!Files.exists(messagesPath)) {
				futureLangFiles.add(CompletableFuture.runAsync(() -> {
					Path defaultResourcePath = JarResources.forClassLoader(THIS_CLASS.getClassLoader(), "lang/" + name);
					try (ReadableByteChannel source = FileChannel.open(defaultResourcePath, StandardOpenOption.READ);
							FileChannel dest = FileChannel.open(messagesPath, StandardOpenOption.WRITE,
									StandardOpenOption.CREATE_NEW)) {
						dest.transferFrom(source, 0, Long.MAX_VALUE);
					} catch (IOException ex) {
						logger.warn("Unable to copy language file for language {}", name, ex);
					}
				}, readWriteService));
			}
		}
		var langFilesFuture = CompletableFuture.allOf(futureLangFiles.toArray(CompletableFuture[]::new));
		var readSqlFuture = throwIfFailed(sql.saveDefaultConfig()).thenCompose((copyResult) -> throwIfFailed(sql.readConfig()));
		var readConfFuture = throwIfFailed(config.saveDefaultConfig()).thenCompose((copyResult) -> throwIfFailed(config.readConfig()));
		var readMsgsFuture = readConfFuture.thenCompose((readConfResult) -> {
			String langFileOption = readConfResult.getReadData().getString("lang-file");
			if (langFileOption == null) {
				logger.warn("Unspecified language file, using default (en)");
				langFileOption = "en";
			}
			Path messagesPath = langFolder.resolve("messages_" + langFileOption + ".yml");
			return langFilesFuture.thenCompose((ignore) -> throwIfFailed(messages.readConfig(messagesPath)));
		});
		return CompletableFuture.allOf(readSqlFuture, readConfFuture, readMsgsFuture).handle((ignore, ex) -> {
			if (ex != null) {
				Throwable cause = ex.getCause();
				if (cause instanceof YamlSyntaxException) {
					logger.warn("One or more of your YML files has invalid syntax. "
							+ "Please use a YAML validator such as https://yaml-online-parser.appspot.com/ "
							+ "and paste your config files there to check them.", cause);
				} else {
					logger.warn("An unexpected issue occurred while reloading the configuration.", ex);
				}
				return false;
			}
			return true;
		});
	}
	
	private <T extends ConfigResult> CompletableFuture<T> throwIfFailed(CompletableFuture<T> futureResult) {
		return futureResult.thenApply((result) -> {
			if (!result.getResultDefinition().isSuccess()) {
				Exception ex = result.getException();
				if (ex instanceof RuntimeException) {
					throw (RuntimeException) ex;
				}
				throw new CompletionException(ex);
			}
			return result;
		});
	}
	
	public ConfigAccessor getSql() {
		return sql.getAccessor();
	}
	
	public ConfigAccessor getConfig() {
		return config.getAccessor();
	}
	
	public ConfigAccessor getMessages() {
		return messages.getAccessor();
	}
	
	DateTimeFormatter getTimeFormatter() {
		return getConfig().getObject("formatting.dates", DateTimeFormatter.class);
	}

	public boolean strictAddressQueries() {
		return getAddressStrictness() != AddressStrictness.LENIENT;
	}
	
	AddressStrictness getAddressStrictness() {
		return getConfig().getObject("enforcement.address-strictness", AddressStrictness.class);
	}

	/**
	 * When an address based punishment is enforced, how should it be?
	 * 
	 * @author A248
	 *
	 */
	public enum AddressStrictness {
		/**
		 * Player's current address must match target address
		 * 
		 */
		LENIENT,
		/**
		 * Any of player's past addresses may match target address
		 * 
		 */
		NORMAL,
		/**
		 * Any of player's past addresses may match any address related to the target address
		 * by a common player
		 * 
		 */
		STRICT
	}
	
	private enum Translation {
		EN
	}
	
}

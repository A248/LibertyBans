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
package space.arim.libertybans.core.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import space.arim.api.configure.ConfigAccessor;
import space.arim.api.configure.ConfigSerialiser;
import space.arim.api.configure.Configuration;
import space.arim.api.configure.JarResources;
import space.arim.api.configure.ValueTransformer;
import space.arim.api.configure.configs.ConfigurationBuilder;
import space.arim.api.configure.configs.SingularConfig;
import space.arim.api.configure.yaml.YamlConfigSerialiser;

import space.arim.libertybans.bootstrap.StartupException;
import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.Part;
import space.arim.libertybans.core.database.DatabaseManager;
import space.arim.libertybans.core.database.IOUtils;

public class Configs implements Part {
	
	private final LibertyBansCore core;
	
	private volatile ConfigPackage configPackage;
	
	public Configs(LibertyBansCore core) {
		this.core = core;
	}
	
	private static CompletableFuture<? extends Configuration> getFor(ConfigSerialiser serialiser, String resourceName,
			List<ValueTransformer> transformers, Executor readWriteService) {
		return new ConfigurationBuilder()
				.defaultResource(JarResources.forCallerClass(resourceName))
				.executor(readWriteService).serialiser(serialiser)
				.addTransformers(transformers).buildMergingConfig();
	}
	
	private ConfigPackage recreateConfig(Executor existingReadWriteService) {
		Executor executor;
		if (existingReadWriteService == null) {
			executor = core.getEnvironment().getPlatformHandle().getRealExecutorFinder().findExecutor();
			if (executor instanceof ExecutorService) {
				// Danger! Don't shut down the platform's combined thread pool
				// readWriteService must NOT be an instance of ExecutorService unless shutdown is desired
				executor = new IOUtils.SafeExecutorWrapper(executor);

			} else if (executor == null) {
				executor = Executors.newFixedThreadPool(1, new IOUtils.ThreadFactoryImpl("LibertyBans-Config-"));
			}
		} else {
			executor = existingReadWriteService;
		}

		Path folder = core.getFolder();
		Path langFolder = folder.resolve("lang");
		try {
			Files.createDirectories(langFolder);
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to create plugin directories", ex);
		}
		ConfigSerialiser serialiser = new YamlConfigSerialiser();
		var futureSql = getFor(serialiser, "sql.yml", DatabaseManager.createConfigTransformers(), executor);
		var futureConfig = getFor(serialiser, "config.yml", ConfigUtil.configTransformers(), executor);
		var futureMessages = getFor(serialiser, "lang/messages_en.yml", List.of(), executor);
		SingularConfig sql = new SingularConfig(futureSql.join(), folder.resolve("sql.yml"));
		SingularConfig config = new SingularConfig(futureConfig.join(), folder.resolve("config.yml"));
		Configuration messages = futureMessages.join();

		return new ConfigPackage(sql, config, messages, executor, langFolder);
	}
	
	@Override
	public void startup() {
		ConfigPackage configPackage = recreateConfig(null);
		if (!configPackage.reloadConfigs().join()) {
			throw new StartupException("Issue while loading configuration");
		}
		this.configPackage = configPackage;
	}
	
	@Override
	public void restart() {
		ConfigPackage existing = this.configPackage;
		ConfigPackage configPackage = recreateConfig(existing.readWriteService);
		if (!configPackage.reloadConfigs().join()) {
			throw new StartupException("Issue while reloading configuration");
		}
		this.configPackage = configPackage;
	}
	
	@Override
	public void shutdown() {
		Executor executor = configPackage.readWriteService;
		if (executor instanceof ExecutorService) {
			((ExecutorService) executor).shutdown();
		}
	}
	
	public CompletableFuture<Boolean> reloadConfigs() {
		return configPackage.reloadConfigs();
	}
	
	public ConfigAccessor getSql() {
		return configPackage.sql.getAccessor();
	}
	
	public ConfigAccessor getConfig() {
		return configPackage.config.getAccessor();
	}
	
	public ConfigAccessor getMessages() {
		return configPackage.messages.getAccessor();
	}
	
	public DateTimeFormatter getTimeFormatter() {
		return getConfig().getObject("formatting.dates", DateTimeFormatter.class);
	}

	public boolean strictAddressQueries() {
		return getAddressStrictness() != AddressStrictness.LENIENT;
	}
	
	public AddressStrictness getAddressStrictness() {
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
	
}

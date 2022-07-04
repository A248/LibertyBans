/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.core.addon;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.injector.MultiBinding;
import space.arim.libertybans.core.config.ConfigHolder;
import space.arim.libertybans.core.config.ConfigResult;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Singleton
public final class StandardAddonCenter implements AddonCenter {

	private final FactoryOfTheFuture futuresFactory;
	private final Path folder;
	private final Provider<Set<Addon<?>>> addons;

	private final Map<Class<?>, ConfigWrapper<?>> configurations = new ConcurrentHashMap<>();

	@Inject
	public StandardAddonCenter(FactoryOfTheFuture futuresFactory, @Named("folder") Path folder,
							   @MultiBinding Provider<Set<Addon<?>>> addons) {
		this.futuresFactory = futuresFactory;
		this.folder = folder;
		this.addons = addons;
	}

	private <C extends AddonConfig> void startupAddon(Path addonsFolder, Addon<C> addon) {
		Class<C> configClass = addon.configInterface();

		// Install configuration
		ConfigWrapper<C> configWrapper = new ConfigWrapper<>(
				new ConfigHolder<>(configClass),
				addonsFolder.resolve(addon.identifier() + ".yml")
		);
		configurations.put(configClass, configWrapper);
		// Reload configuration
		configWrapper.reload().join();
		// Startup if enabled
		if (configWrapper.currentConfiguration().enable()) {
			addon.startup();
		}
	}

	// Visible for testing
	<C extends AddonConfig> ConfigWrapper<C> configWrapperFor(Addon<C> addon) {
		Class<C> configClass = addon.configInterface();
		ConfigWrapper<?> wrapper = configurations.get(configClass);
		if (!configClass.equals(wrapper.holder.getConfigClass())) {
			throw new IllegalStateException("Mismatched config class");
		}
		@SuppressWarnings("unchecked")
		ConfigWrapper<C> casted = (ConfigWrapper<C>) wrapper;
		return casted;
	}

	private <C extends AddonConfig> void restartAddon(Addon<C> addon) {
		ConfigWrapper<C> configWrapper = configWrapperFor(addon);
		// Shutdown if previously enabled
		if (configWrapper.currentConfiguration().enable()) {
			addon.shutdown();
		}
		configWrapper.reload().join();
		// Startup if now enabled
		if (configWrapper.currentConfiguration().enable()) {
			addon.startup();
		}
	}

	private <C extends AddonConfig> CentralisedFuture<ConfigResult> reloadAddon(Addon<C> addon) {
		ConfigWrapper<C> configWrapper = configWrapperFor(addon);

		boolean enabledPreviously = configWrapper.currentConfiguration().enable();
		return futuresFactory.copyFuture(configWrapper.reload())
				.thenApply((configResult) -> {
					boolean enabledNow = configWrapper.currentConfiguration().enable();
					if (enabledNow != enabledPreviously) {
						if (enabledPreviously) addon.shutdown();
						if (enabledNow) addon.startup();
					}
					return configResult;
				});
	}

	@Override
	public void startup() {
		Path addonsFolder = folder.resolve("addons");
		addons.get().forEach((addon) -> startupAddon(addonsFolder, addon));
	}

	@Override
	public void restart() {
		addons.get().forEach(this::restartAddon);
	}

	@Override
	public void shutdown() {
		for (Addon<?>  addon : addons.get()) {
			if (configurationFor(addon).enable()) {
				addon.shutdown();
			}
		}
	}

	@Override
	public <C extends AddonConfig> C configurationFor(Addon<C> addon) {
		return configWrapperFor(addon).currentConfiguration();
	}

	@Override
	public CentralisedFuture<Boolean> reloadAddons() {
		Set<Addon<?>> addons = this.addons.get();
		List<CentralisedFuture<ConfigResult>> reloadFutures = new ArrayList<>(addons.size());
		for (Addon<?> addon : addons) {
			reloadFutures.add(reloadAddon(addon));
		}
		return futuresFactory.allOf(reloadFutures).thenApply((ignore) -> {
			List<ConfigResult> configResults = new ArrayList<>(reloadFutures.size());
			reloadFutures.forEach((reloadFuture) -> configResults.add(reloadFuture.join()));
			return ConfigResult.combinePessimistically(configResults).isSuccess();
		});
	}

	@Override
	public Stream<String> allIdentifiers() {
		return this.addons.get().stream().map(Addon::identifier);
	}

	@Override
	public @Nullable Addon<?> addonByIdentifier(String identifier) {
		for (Addon<?> addon : addons.get()) {
			if (identifier.equals(addon.identifier())) {
				return addon;
			}
		}
		return null;
	}

	@Override
	public CentralisedFuture<Boolean> reloadConfiguration(Addon<?> addon) {
		return reloadAddon(addon).thenApply(ConfigResult::isSuccess);
	}

	// Visible for testing
	static final class ConfigWrapper<C extends AddonConfig> {

		private final ConfigHolder<C> holder;
		private final Path path;

		private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

		private ConfigWrapper(ConfigHolder<C> holder, Path path) {
			this.holder = Objects.requireNonNull(holder, "holder");
			this.path = Objects.requireNonNull(path, "path");
		}

		C currentConfiguration() {
			return holder.getConfigData();
		}

		private CompletableFuture<ConfigResult> reload() {
			return holder.reload(path).thenApply((configResult) -> {
				if (!configResult.isSuccess()) {
					logger.warn("Failed to load addon configuration at {}. The default values will be used for now. \n\n" +
							"Please fix the mistake and reload the configuration.", path);
				}
				return configResult;
			});
		}

		@Override
		public String toString() {
			return "ConfigWrapper{" +
					"holder=" + holder +
					", path=" + path +
					'}';
		}
	}
}

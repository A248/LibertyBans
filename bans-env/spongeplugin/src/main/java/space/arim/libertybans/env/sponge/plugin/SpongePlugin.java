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

package space.arim.libertybans.env.sponge.plugin;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent;
import org.spongepowered.api.event.lifecycle.RefreshGameEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.Instantiator;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;
import space.arim.libertybans.bootstrap.Platforms;
import space.arim.libertybans.bootstrap.plugin.PluginInfo;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Plugin(PluginInfo.ID)
public final class SpongePlugin {

	private final PluginContainer plugin;
	private final Game game;
	private final Logger logger;
	private final Path folder;

	private CompletableFuture<BaseFoundation> initializationFuture;
	private BaseFoundation base;

	@Inject
	public SpongePlugin(PluginContainer plugin, Game game, Logger logger, @ConfigDir(sharedRoot = false) Path folder) {
		this.plugin = plugin;
		this.game = game;
		this.logger = logger;
		this.folder = folder;
	}

	@Listener
	public synchronized void onConstruct(ConstructPluginEvent event) {
		if (event.plugin().instance() != this) {
			return;
		}
		if (initializationFuture != null || base != null) {
			throw new IllegalStateException("Server initialised twice?");
		}
		initializationFuture = initialize();
	}

	private static PlatformAccess platformAccess(BaseFoundation base) {
		return (PlatformAccess) base.platformAccess();
	}

	@Listener
	public synchronized void onRegisterCommands(RegisterCommandEvent<Command.Raw> event) {
		if (initializationFuture != null) {
			try {
				base = initializationFuture.join();
			} finally {
				initializationFuture = null;
			}
		}
		if (base == null) {
			return;
		}
		Command.Raw command = platformAccess(base).commandHandler();
		event.register(plugin, command, "libertybans");
	}

	@Listener
	public synchronized void onServiceProvision(ProvideServiceEvent.EngineScoped<BanService> event) {
		if (base == null) {
			return;
		}
		base.startup();

		PlatformAccess platformAccess = platformAccess(base);
		if (platformAccess.registerBanService()) {
			event.suggest(platformAccess::banService);
		}
	}

	@Listener
	public synchronized void onReload(RefreshGameEvent event) {
		if (base == null) {
			logger.warn("LibertyBans never launched so it cannot reload.");
			return;
		}
		boolean restarted = base.fullRestart();
		if (!restarted) {
			logger.info("Not restarting because loading already in process");
		}
	}

	@Listener
	public synchronized void onStop(StoppingEngineEvent<Server> event) {
		BaseFoundation base = this.base;
		this.base = null;
		if (base == null) {
			logger.warn("LibertyBans wasn't launched; check your log for a startup error");
			return;
		}
		base.shutdown();
	}

	private CompletableFuture<BaseFoundation> initialize() {

		LibertyBansLauncher launcher = new LibertyBansLauncher.Builder()
				.folder(folder)
				.logger(new Log4jBootstrapLogger(logger))
				.platform(Platforms.sponge(Game.class.getClassLoader()))
				.executor(game.asyncScheduler().executor(plugin))
				.culpritFinder(new SpongeCulpritFinder(game))
				.build();
		return launcher.attemptLaunch().thenApply((launchLoader) -> {
			BaseFoundation base;
			try {
				base = new Instantiator("space.arim.libertybans.env.sponge.SpongeLauncher", launchLoader)
						.invoke(PluginContainer.class, plugin, Game.class, game, folder);
			} catch (IllegalArgumentException | SecurityException | ReflectiveOperationException ex) {
				logger.warn("Failed to launch LibertyBans", ex);
				return null;
			}
			return base;
		});
	}

}

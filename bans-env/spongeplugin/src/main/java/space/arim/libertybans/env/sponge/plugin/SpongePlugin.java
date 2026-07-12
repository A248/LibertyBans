/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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
import space.arim.libertybans.bootstrap.*;
import space.arim.libertybans.bootstrap.plugin.PluginInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
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
		if (initializationFuture != null) {
			game.eventManager().registerListeners(plugin, new EntryPoints());
		}
	}

	private static PlatformAccess platformAccess(BaseFoundation base) {
		return (PlatformAccess) base.platformAccess();
	}

	private BaseFoundation getBase(String purpose, boolean tryStart) {
		BaseFoundation base = this.base;
		if (initializationFuture != null) {
			try {
				this.base = base = initializationFuture.join();
			} finally {
				initializationFuture = null;
			}
		}
		if (base == null) {
			logger.warn("LibertyBans never launched so it cannot {}.", purpose);
			return null;
		}
		if (tryStart && base.getRunState() == RunState.IDLE) {
			base.startup();
		}
		if (base.getRunState() == RunState.FAILED) {
			logger.warn("Unable to {} because LibertyBans failed to start", purpose);
			return null;
		}
		return base;
	}

	public final class EntryPoints {

		@Listener
		public synchronized void onRegisterCommands(RegisterCommandEvent<Command.Raw> event) {
			BaseFoundation base = getBase("register commands", false);
			if (base == null) {
				return;
			}
			Command.Raw command = platformAccess(base).commandHandler();
			event.register(plugin, command, "libertybans");
		}

		@Listener
		public synchronized void onServiceProvision(ProvideServiceEvent.EngineScoped<BanService> event) {
			BaseFoundation base = getBase("provide services", true);
			if (base == null) {
				return;
			}
			PlatformAccess platformAccess = platformAccess(base);
			if (platformAccess.registerBanService()) {
				event.suggest(platformAccess::banService);
			}
		}

		@Listener
		public synchronized void onReload(RefreshGameEvent event) {
			BaseFoundation base = getBase("reload", true);
			if (base == null) {
				return;
			}
			boolean restarted = base.fullRestart();
			if (!restarted) {
				logger.info("Not restarting because loading already in process");
			}
		}

		@Listener
		public synchronized void onStop(StoppingEngineEvent<Server> event) {
			BaseFoundation base = getBase("shutdown", false);
			if (base == null) {
				return;
			}
			SpongePlugin.this.base = null;
			base.shutdown();
		}
	}

	private CompletableFuture<BaseFoundation> unsupported(String msg) {
		logger.error(
                """
                \
                ERROR
                \
                Sorry, however your Sponge server is not supported. You may need to file a request on the issue tracker.
                https://github.com/A248/LibertyBans/issues\
                \
                Reason:\
                {}""",
				msg
		);
		throw new UnsupportedOperationException(msg);
	}

	private CompletableFuture<BaseFoundation> initialize() {
		SpongeVersion spongeVersion;
		{
			OptionalInt optDataVersion = game.platform().minecraftVersion().dataVersion();
			if (optDataVersion.isEmpty()) {
				return unsupported("Unknown Minecraft data version (cannot detect Sponge API version)");
			}
			int dataVersion = optDataVersion.getAsInt();
			Optional<SpongeVersion> optSpongeVersion = SpongeVersion.detectVersion(dataVersion);
			if (optSpongeVersion.isEmpty()) {
				return unsupported("Unknown or unsupported Minecraft data version " + dataVersion);
			}
			spongeVersion = optSpongeVersion.get();
		}
		// The oldest API version which we do NOT support
		SpongeVersion apiLimit = SpongeVersion.API_18;
		if (spongeVersion.isAtLeast(apiLimit)) {
			return unsupported("Detected Sponge API version " + spongeVersion + " or greater");
		}
		ClassLoader platformClassLoader = Game.class.getClassLoader();

		LibertyBansLauncher launcher = new LibertyBansLauncher.Builder()
				.folder(folder)
				.logger(new Log4jBootstrapLogger(logger))
				.platform(Platform
						.builder(Platform.Category.SPONGE)
						.nameAndVersion("Sponge", spongeVersion.display())
						// Slf4j is an internal dependency
						.slf4jSupport(new LibraryDetection.ByClassResolution(ProtectedLibrary.SLF4J_API))
						.kyoriAdventureSupport(LibraryDetection.enabled())
						.caffeineProvided(LibraryDetection.enabled())
						.snakeYamlProvided(new LibraryDetection.ByClassLoaderScan(ProtectedLibrary.SNAKEYAML, platformClassLoader))
						// HikariCP is an internal dependency
						.hiddenHikariCP(new LibraryDetection.ByClassLoaderScan(ProtectedLibrary.HIKARICP, platformClassLoader))
				)
				.executor(game.asyncScheduler().executor(plugin))
				.culpritFinder(new SpongeCulpritFinder(game))
				.build();
		Payload<PluginContainer> payload = launcher.getPayloadWith(plugin, List.of(spongeVersion));
		return launcher.attemptLaunch().thenApply((launchLoader) -> {
			BaseFoundation base;
			try {
				base = new Instantiator(
						"space.arim.libertybans.env.sponge.SpongeLauncher", launchLoader
				).invoke(payload, Game.class, game);
			} catch (IllegalArgumentException | SecurityException | ReflectiveOperationException ex) {
				logger.warn("Failed to launch LibertyBans", ex);
				return null;
			}
			return base;
		});
	}

}

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

package space.arim.libertybans.env.sponge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Game;
import org.spongepowered.plugin.PluginContainer;
import space.arim.injector.Identifier;
import space.arim.injector.InjectorBuilder;
import space.arim.injector.SpecificationSupport;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.PlatformLauncher;
import space.arim.libertybans.core.ApiBindModule;
import space.arim.libertybans.core.CommandsModule;
import space.arim.libertybans.core.PillarOneBindModule;
import space.arim.libertybans.core.PillarTwoBindModule;
import space.arim.libertybans.core.env.InstanceType;
import space.arim.libertybans.env.sponge.listener.RegisterListeners;
import space.arim.libertybans.env.sponge.listener.RegisterListenersByMethodScan;
import space.arim.libertybans.env.sponge.listener.RegisterListenersStandard;
import space.arim.libertybans.env.sponge.listener.RegisterListenersWithLookup;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;

import java.nio.file.Path;

public final class SpongeLauncher implements PlatformLauncher {

	private final PluginContainer plugin;
	private final Game game;
	private final Path folder;
	private final Omnibus omnibus;

	public SpongeLauncher(PluginContainer plugin, Game game, Path folder) {
		this(plugin, game, folder, OmnibusProvider.getOmnibus());
	}

	public SpongeLauncher(PluginContainer plugin, Game game, Path folder, Omnibus omnibus) {
		this.plugin = plugin;
		this.game = game;
		this.folder = folder;
		this.omnibus = omnibus;
	}

	@Override
	public BaseFoundation launch() {
		Class<? extends RegisterListeners> registerListenersBinding;
		{
			int dataVersion = game.platform().minecraftVersion().dataVersion().orElseThrow();
			// Sponge servers beyond MC 1.16.5 (data version 2586) use API 9
			boolean spongeApi9 = dataVersion > 2586;
			Logger logger = LoggerFactory.getLogger(getClass());
			logger.info("Using Sponge API 9+: {}", spongeApi9);

			if (!spongeApi9) {
				registerListenersBinding = RegisterListenersStandard.class;
			} else if (RegisterListenersWithLookup.detectIfUsable()) {
				logger.info("Listener registration with lookup API detected and available");
				registerListenersBinding = RegisterListenersWithLookup.class;
			} else {
				// Fallback to manual method scanning
				logger.warn(
						"Proper listener registration for Sponge API 9 is not available on your outdated version. " +
								"Please update your Sponge implementation so that the latest APIs are usable.");
				registerListenersBinding = RegisterListenersByMethodScan.class;
			}
		}
		return new InjectorBuilder()
				.bindInstance(PluginContainer.class, plugin)
				.bindInstance(Game.class, game)
				.bindInstance(Identifier.ofTypeAndNamed(Path.class, "folder"), folder)
				.bindInstance(InstanceType.class, InstanceType.GAME_SERVER)
				.bindInstance(Omnibus.class, omnibus)
				.addBindModules(
						new ApiBindModule(),
						new PillarOneBindModule(),
						new PillarTwoBindModule(),
						new CommandsModule(),
						new SpongeBindModule()
				)
				.bindIdentifier(RegisterListeners.class, registerListenersBinding)
				.specification(SpecificationSupport.JAKARTA)
				.multiBindings(true)
				.build()
				.request(BaseFoundation.class);
	}

}

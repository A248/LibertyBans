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

package space.arim.libertybans.env.sponge;

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
import space.arim.libertybans.env.sponge.listener.RegisterListenersByMethodScan;
import space.arim.libertybans.env.sponge.listener.RegisterListenersStandard;
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
		boolean spongeApi9;
		{
			int dataVersion = game.platform().minecraftVersion().dataVersion().orElseThrow();
			// Sponge servers beyond MC 1.16.5 (data version 2586) use API 9
			spongeApi9 = dataVersion > 2586;
			LoggerFactory.getLogger(getClass()).info("Using Sponge API 9+: {}", spongeApi9);
		}
		return new InjectorBuilder()
				.bindInstance(PluginContainer.class, plugin)
				.bindInstance(Game.class, game)
				.bindInstance(Identifier.ofTypeAndNamed(Path.class, "folder"), folder)
				.bindInstance(Omnibus.class, omnibus)
				.addBindModules(
						new ApiBindModule(),
						new PillarOneBindModule(),
						new PillarTwoBindModule(),
						new CommandsModule(),
						new SpongeBindModule(),
						spongeApi9 ? new RegisterListenersByMethodScan.Module() : new RegisterListenersStandard.Module()
				)
				.specification(SpecificationSupport.JAKARTA)
				.multiBindings(true)
				.build()
				.request(BaseFoundation.class);
	}

}

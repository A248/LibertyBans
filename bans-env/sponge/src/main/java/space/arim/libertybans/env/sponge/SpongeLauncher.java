/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

import org.spongepowered.api.Game;
import org.spongepowered.plugin.PluginContainer;
import space.arim.injector.Identifier;
import space.arim.injector.InjectorBuilder;
import space.arim.injector.SpecificationSupport;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.Payload;
import space.arim.libertybans.bootstrap.PlatformId;
import space.arim.libertybans.bootstrap.PlatformLauncher;
import space.arim.libertybans.core.ApiBindModule;
import space.arim.libertybans.core.CommandsModule;
import space.arim.libertybans.core.PillarOneBindModule;
import space.arim.libertybans.core.PillarTwoBindModule;
import space.arim.libertybans.core.env.InstanceType;
import space.arim.libertybans.env.sponge.listener.RegisterListeners;
import space.arim.libertybans.env.sponge.listener.RegisterListenersRegular;
import space.arim.libertybans.env.sponge.listener.RegisterListenersWithLookup;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;

import java.nio.file.Path;

public final class SpongeLauncher implements PlatformLauncher {

	private final Payload<PluginContainer> payload;
	private final Game game;
	private final Omnibus omnibus;

	public SpongeLauncher(Payload<PluginContainer> payload, Game game) {
		this(payload, game, OmnibusProvider.getOmnibus());
	}

	public SpongeLauncher(Payload<PluginContainer> payload, Game game, Omnibus omnibus) {
		this.payload = payload;
		this.game = game;
		this.omnibus = omnibus;
	}

	@Override
	public BaseFoundation launch() {
		Class<? extends RegisterListeners> registerListenersBinding;
		if (RegisterListenersWithLookup.detectIfUsable()) {
			registerListenersBinding = RegisterListenersWithLookup.class;
		} else {
			// Fallback to regular method (Sponge API 8)
			registerListenersBinding = RegisterListenersRegular.class;
		}
		return new InjectorBuilder()
				.bindInstance(PluginContainer.class, payload.plugin())
				.bindInstance(Game.class, game)
				.bindInstance(PlatformId.class, payload.platformId())
				.bindInstance(Identifier.ofTypeAndNamed(Path.class, "folder"), payload.pluginFolder())
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

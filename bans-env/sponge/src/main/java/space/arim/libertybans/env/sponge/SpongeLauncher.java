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
import space.arim.libertybans.env.sponge.plugin.ChannelFacade;
import space.arim.libertybans.env.sponge.plugin.ChannelFacadeApi8;
import space.arim.libertybans.env.sponge.plugin.SpongeVersion;
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
		SpongeVersion spongeVersion = payload.getAttachment(0, SpongeVersion.class);

		Class<? extends RegisterListeners> registerListenersBinding;
		if (spongeVersion.isAtLeast(SpongeVersion.API_12) || RegisterListenersWithLookup.detectIfUsable()) {
			registerListenersBinding = RegisterListenersWithLookup.class;
		} else {
			// Fallback to regular method (Sponge API 8)
			registerListenersBinding = RegisterListenersRegular.class;
		}
		Class<? extends ChannelFacade> channelFacadeBinding;
		Class<? extends ChatListener> chatListenerBinding;
		if (spongeVersion.isAtLeast(SpongeVersion.API_12)) {
			channelFacadeBinding = ChannelFacadeApi12.class;
			chatListenerBinding = ChatListener.ChatApi12.class;
		} else {
			channelFacadeBinding = ChannelFacadeApi8.class;
			chatListenerBinding = ChatListener.ChatApi8.class;
		}
		return new InjectorBuilder()
				.bindInstance(PluginContainer.class, payload.plugin())
				.bindInstance(Game.class, game)
				.bindInstance(PlatformId.class, payload.platformId())
				.bindInstance(Identifier.ofTypeAndNamed(Path.class, "folder"), payload.pluginFolder())
				.bindInstance(SpongeVersion.class,  spongeVersion)
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
				.bindIdentifier(ChannelFacade.class, channelFacadeBinding)
				.bindIdentifier(ChatListener.class, chatListenerBinding)
				.specification(SpecificationSupport.JAKARTA)
				.multiBindings(true)
				.build()
				.request(BaseFoundation.class);
	}

}

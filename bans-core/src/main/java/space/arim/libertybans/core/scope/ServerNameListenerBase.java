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

package space.arim.libertybans.core.scope;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import space.arim.api.env.annote.PlatformPlayer;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.EnvMessageChannel;
import space.arim.libertybans.core.env.InstanceType;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.libertybans.core.env.message.GetServer;

import java.util.function.Consumer;

@Singleton
public final class ServerNameListenerBase<@PlatformPlayer P, H> implements PlatformListener {

	private final Configs configs;
	private final InternalScopeManager scopeManager;
	private final EnvEnforcer<P> envEnforcer;
	private final EnvMessageChannel<H> envMessageChannel;

	private final H handler;

	@Inject
	public ServerNameListenerBase(InstanceType instanceType, Configs configs, InternalScopeManager scopeManager,
								  EnvEnforcer<P> envEnforcer, EnvMessageChannel<H> envMessageChannel) {
		if (instanceType != InstanceType.GAME_SERVER) {
			throw new IllegalStateException("Cannot use server name listener except for backend game servers");
		}
		this.configs = configs;
		this.scopeManager = scopeManager;
		this.envEnforcer = envEnforcer;
		this.envMessageChannel = envMessageChannel;
		handler = envMessageChannel.createHandler(new ResponseHandler(), new GetServer());
	}

	@Override
	public void register() {
		if (configs.getMainConfig().platforms().gameServers().usePluginMessaging()) {
			envMessageChannel.installHandler(handler);
		}
	}

	@Override
	public void unregister() {
		// This should not throw even if the handler is not registered
		envMessageChannel.uninstallHandler(handler);
	}

	public void onJoin(P player) {
		if (configs.getMainConfig().platforms().gameServers().usePluginMessaging()
				&& configs.getScopeConfig().serverName().autoDetect()
				&& scopeManager.serverNameUndetected()) {
			envEnforcer.sendPluginMessage(player, new GetServer(), null);
		}
	}

	final class ResponseHandler implements Consumer<GetServer.Response> {

		@Override
		public void accept(GetServer.Response response) {
			scopeManager.detectServerName(response.server());
		}
	}

}

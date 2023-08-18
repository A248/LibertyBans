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

package space.arim.libertybans.env.velocity;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.omnibus.util.ThisClass;

public final class ConnectionListener implements PlatformListener {

	private final PluginContainer plugin;
	private final ProxyServer server;
	private final Guardian guardian;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public ConnectionListener(PluginContainer plugin, ProxyServer server, Guardian guardian) {
		this.plugin = plugin;
		this.server = server;
		this.guardian = guardian;
	}

	@Override
	public void register() {
		server.getEventManager().register(plugin, this);
	}

	@Override
	public void unregister() {
		server.getEventManager().unregisterListener(plugin, this);
	}

	@Subscribe(order = PostOrder.EARLY)
	public EventTask onConnect(LoginEvent event) {
		if (!event.getResult().isAllowed()) {
			logger.trace("Event {} is already blocked", event);
			return null;
		}
		Player player = event.getPlayer();
		return EventTask.resumeWhenComplete(guardian.executeAndCheckConnection(
				player.getUniqueId(), player.getUsername(), player.getRemoteAddress().getAddress()
		).thenAccept((message) -> {
			if (message == null) {
				logger.trace("Event {} will be permitted", event);
			} else {
				event.setResult(ComponentResult.denied(message));
			}
		}));
	}

	@Subscribe(order = PostOrder.EARLY)
	public EventTask onServerSwitch(ServerPreConnectEvent event) {
		if (!event.getResult().isAllowed()) {
			return null;
		}
		RegisteredServer destination = event.getResult().getServer().orElse(null);
		if (destination == null) {
			// Properly speaking, the API does not exclude this possibility
			return null;
		}
		Player player = event.getPlayer();
		return EventTask.resumeWhenComplete(guardian.checkServerSwitch(
				player.getUniqueId(), player.getRemoteAddress().getAddress(), destination.getServerInfo().getName()
		).thenAccept((message) -> {
			if (message != null) {
				event.setResult(ServerPreConnectEvent.ServerResult.denied());
				player.sendMessage(message);
			}
		}));
	}

}

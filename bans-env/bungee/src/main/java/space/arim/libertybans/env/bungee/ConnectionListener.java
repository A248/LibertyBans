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

package space.arim.libertybans.env.bungee;

import jakarta.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.omnibus.util.ThisClass;

import java.net.InetAddress;

public final class ConnectionListener implements Listener, PlatformListener {

	private final Plugin plugin;
	private final Guardian guardian;
	private final AddressReporter addressReporter;
	private final AudienceRepresenter<CommandSender> audienceRepresenter;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public ConnectionListener(Plugin plugin, Guardian guardian, AddressReporter addressReporter,
							  AudienceRepresenter<CommandSender> audienceRepresenter) {
		this.plugin = plugin;
		this.guardian = guardian;
		this.addressReporter = addressReporter;
		this.audienceRepresenter = audienceRepresenter;
	}

	@Override
	public void register() {
		plugin.getProxy().getPluginManager().registerListener(plugin, this);
	}

	@Override
	public void unregister() {
		plugin.getProxy().getPluginManager().unregisterListener(this);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onConnect(LoginEvent event) {
		if (event.isCancelled()) {
			logger.trace("Event {} is already blocked", event);
			return;
		}
		PendingConnection connection = event.getConnection();
		InetAddress address = addressReporter.getAddress(connection);

		event.registerIntent(plugin);

		guardian.executeAndCheckConnection(
				connection.getUniqueId(), connection.getName(), address
		).thenAccept((message) -> {
			if (message == null) {
				logger.trace("Event {} will be permitted", event);
			} else {
				event.setCancelled(true);
				event.setCancelReason(TextComponent.fromLegacyText(
						LegacyComponentSerializer.legacySection().serialize(message)));
			}
		}).whenComplete((ignore, ex) -> {
			if (ex != null) {
				logger.error("Exception enforcing incoming connection", ex);
			}
			event.completeIntent(plugin);
		});
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onServerSwitch(ServerConnectEvent event) {
		if (event.getReason() == ServerConnectEvent.Reason.LOBBY_FALLBACK) {
			// Don't kick players if there is no alternative server for them
			return;
		}
		ProxiedPlayer player = event.getPlayer();
		InetAddress address = addressReporter.getAddress(player);

		Component message = guardian.checkServerSwitch(
				player.getUniqueId(), address, event.getTarget().getName()
		).join();
		if (message != null) {
			event.setCancelled(true);
			audienceRepresenter.toAudience(player).sendMessage(message);
		}
	}

}

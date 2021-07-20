/* 
 * LibertyBans-env-bungee
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-bungee is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-bungee is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-bungee. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.bungee;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.libertybans.core.punish.Enforcer;
import space.arim.omnibus.util.ThisClass;

import java.net.InetAddress;
import java.util.UUID;

@Singleton
public class ConnectionListener implements Listener, PlatformListener {

	private final Plugin plugin;
	private final Enforcer enforcer;
	private final AddressReporter addressReporter;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	@Inject
	public ConnectionListener(Plugin plugin, Enforcer enforcer, AddressReporter addressReporter) {
		this.plugin = plugin;
		this.enforcer = enforcer;
		this.addressReporter = addressReporter;
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
			logger.debug("Event {} is already blocked", event);
			return;
		}
		PendingConnection connection = event.getConnection();
		UUID uuid = connection.getUniqueId();
		String name = connection.getName();
		InetAddress address = addressReporter.getAddress(connection);

		event.registerIntent(plugin);

		enforcer.executeAndCheckConnection(uuid, name, address).thenAccept((message) -> {
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
	
}

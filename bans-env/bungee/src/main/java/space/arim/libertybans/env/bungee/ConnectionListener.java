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

import java.net.InetSocketAddress;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.chat.SendableMessage;
import space.arim.api.env.chat.BungeeComponentConverter;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class ConnectionListener implements Listener {

	private final BungeeEnv env;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	ConnectionListener(BungeeEnv env) {
		this.env = env;
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onConnect(LoginEvent evt) {
		if (evt.isCancelled()) {
			logger.debug("Player '{}' is already blocked by the server or another plugin", evt.getConnection().getName());
			return;
		}
		evt.registerIntent(env.getPlugin());
		PendingConnection conn = evt.getConnection();
		UUID uuid = conn.getUniqueId();
		String name = conn.getName();
		byte[] address = ((InetSocketAddress) conn.getSocketAddress()).getAddress().getAddress();
		env.core.getSelector().executeAndCheckConnection(uuid, name, address).thenAccept((punishment) -> {
			if (punishment == null) {
				logger.trace("Letting '{}' through the gates", name);

			} else {
				CentralisedFuture<SendableMessage> message = env.core.getFormatter().getPunishmentMessage(punishment);
				assert message.isDone() : punishment;

				evt.setCancelled(true);
				evt.setCancelReason(new BungeeComponentConverter().convertFrom(message.join()));
			}
			evt.completeIntent(env.getPlugin());
		});
	}
	
}

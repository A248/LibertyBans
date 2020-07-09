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

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class ConnectionListener implements Listener {

	private final BungeeEnv env;
	
	ConnectionListener(BungeeEnv env) {
		this.env = env;
	}
	
	@EventHandler
	public void onConnect(LoginEvent evt) {
		evt.registerIntent(env.plugin);
		PendingConnection conn = evt.getConnection();
		UUID uuid = conn.getUniqueId();
		String name = conn.getName();
		byte[] address = ((InetSocketAddress) conn.getSocketAddress()).getAddress().getAddress();
		env.core.getSelector().executeAndCheckConnection(uuid, name, address).thenAccept((punishment) -> {
			if (punishment != null) {
				evt.setCancelled(true);
				evt.setCancelReason(TextComponent.fromLegacyText(env.core.getFormatter().getPunishmentMessage(punishment)));
			}
			evt.completeIntent(env.plugin);
		});
	}
	
}

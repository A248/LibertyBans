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

package space.arim.libertybans.env.velocity;

import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent.ChatResult;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import jakarta.inject.Inject;
import net.kyori.adventure.text.Component;
import space.arim.libertybans.core.punish.Guardian;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

public final class ChatListener extends VelocityAsyncListener<PlayerChatEvent, Component> {

	private final Guardian guardian;

	@Inject
	public ChatListener(PluginContainer plugin, ProxyServer server, Guardian guardian) {
		super(plugin, server);
		this.guardian = guardian;
	}

	@Override
	public Class<PlayerChatEvent> getEventClass() {
		return PlayerChatEvent.class;
	}

	@Override
	protected CentralisedFuture<Component> beginComputation(PlayerChatEvent event) {
		Player player = event.getPlayer();
		return guardian.checkChat(player.getUniqueId(), player.getRemoteAddress().getAddress(), null);
	}

	@Override
	protected void executeNonNullResult(PlayerChatEvent event, Component message) {
		event.setResult(ChatResult.denied());
		event.getPlayer().sendMessage(message);
	}

}

/* 
 * LibertyBans-env-velocity
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-velocity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-velocity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-velocity. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.velocity;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.chat.SendableMessage;
import space.arim.api.env.PlatformHandle;

import space.arim.libertybans.core.punish.Enforcer;

import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent.ChatResult;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

@Singleton
public class ChatListener extends VelocityParallelisedListener<PlayerChatEvent, SendableMessage> {

	private final Enforcer enforcer;
	private final PlatformHandle handle;

	@Inject
	public ChatListener(PluginContainer plugin, ProxyServer server, Enforcer enforcer, PlatformHandle handle) {
		super(plugin, server);
		this.enforcer = enforcer;
		this.handle = handle;
	}

	@Override
	public Class<PlayerChatEvent> getEventClass() {
		return PlayerChatEvent.class;
	}

	@Override
	protected CentralisedFuture<SendableMessage> beginComputation(PlayerChatEvent event) {
		Player player = event.getPlayer();
		return enforcer.checkChat(player.getUniqueId(), player.getRemoteAddress().getAddress(), null);
	}

	@Override
	protected void executeNonNullResult(PlayerChatEvent event, SendableMessage message) {
		event.setResult(ChatResult.denied());
		handle.sendMessage(event.getPlayer(), message);
	}

}

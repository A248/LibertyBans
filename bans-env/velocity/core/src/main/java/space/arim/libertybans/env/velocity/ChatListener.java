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
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent.ChatResult;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import jakarta.inject.Inject;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.libertybans.core.selector.Guardian;

public final class ChatListener implements PlatformListener {

	private final PluginContainer plugin;
	private final ProxyServer server;
	private final Guardian guardian;

	@Inject
	public ChatListener(PluginContainer plugin, ProxyServer server, Guardian guardian) {
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
	public EventTask onChat(PlayerChatEvent event) {
		return combinedChatEvent(event, event.getPlayer(), null, ChatResult.denied());
	}

	@Subscribe(order = PostOrder.EARLY)
	public EventTask onCommand(CommandExecuteEvent event) {
		if (!(event.getCommandSource() instanceof Player player)) {
			return null;
		}
		return combinedChatEvent(event, player, event.getCommand(), CommandExecuteEvent.CommandResult.denied());
	}

	private <E extends ResultedEvent<R>, R extends ResultedEvent.Result> EventTask combinedChatEvent(
			E event, Player player, String command, R deniedResult) {
		if (!event.getResult().isAllowed()) {
			return null;
		}
		return EventTask.resumeWhenComplete(guardian.checkChat(
				player.getUniqueId(), player.getRemoteAddress().getAddress(), command
		).thenAccept((message) -> {
			if (message != null) {
				event.setResult(deniedResult);
				player.sendMessage(message);
			}
		}));
	}

}

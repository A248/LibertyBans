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

package space.arim.libertybans.env.spigot;

import jakarta.inject.Inject;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.util.concurrent.TimeUnit;

public final class ChatListener implements PlatformListener, Listener {

	private final Plugin plugin;
	private final Guardian guardian;
	private final AudienceRepresenter<CommandSender> audienceRepresenter;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public ChatListener(Plugin plugin, Guardian guardian, AudienceRepresenter<CommandSender> audienceRepresenter) {
		this.plugin = plugin;
		this.guardian = guardian;
		this.audienceRepresenter = audienceRepresenter;
	}

	@Override
	public void register() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@Override
	public void unregister() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onChat(AsyncPlayerChatEvent event) {
		combinedChatEvent(event, null);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		String command = event.getMessage();
		combinedChatEvent(event, (command.charAt(0) == '/') ? command.substring(1) : command);
	}

	private <E extends PlayerEvent & Cancellable> void combinedChatEvent(E event, String command) {
		if (event.isCancelled()) {
			return;
		}
		Player player = event.getPlayer();
		CentralisedFuture<Component> futureMessage = guardian.checkChat(
				player.getUniqueId(), player.getAddress().getAddress(), command
		);
		Component message;
		if (futureMessage.isDone()) {
			message = futureMessage.join();

		} else if (event.isAsynchronous()) {
			logger.warn(
					"Cached mute unavailable for asynchronous chat/command event. Perhaps {} logged off?",
					player.getName()
			);
			message = futureMessage.orTimeout(4L, TimeUnit.SECONDS).join();
		} else {
			throw new IllegalStateException("Cached mute unavailable for synchronous chat/command event");
		}
		if (message == null) {
			return;
		}
		event.setCancelled(true);
		audienceRepresenter.toAudience(player).sendMessage(message);
	}

}

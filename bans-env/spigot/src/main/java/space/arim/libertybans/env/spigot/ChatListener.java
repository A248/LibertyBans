/* 
 * LibertyBans-env-spigot
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-spigot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-spigot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-spigot. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.spigot;

import java.net.InetAddress;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.core.punish.Guardian;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.libertybans.core.selector.SyncEnforcement;
import space.arim.libertybans.core.service.FuturePoster;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public class ChatListener extends SpigotParallelisedListener<PlayerEvent, Component> {

	private final FuturePoster futurePoster;
	private final Guardian guardian;
	private final Configs configs;
	private final AudienceRepresenter<CommandSender> audienceRepresenter;

	@Inject
	public ChatListener(JavaPlugin plugin, FuturePoster futurePoster, Guardian guardian,
			Configs configs, AudienceRepresenter<CommandSender> audienceRepresenter) {
		super(plugin);
		this.futurePoster = futurePoster;
		this.guardian = guardian;
		this.configs = configs;
		this.audienceRepresenter = audienceRepresenter;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onChatLow(AsyncPlayerChatEvent event) {
		combinedBegin(event, null);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onCommandLow(PlayerCommandPreprocessEvent event) {
		String command = event.getMessage();
		combinedBegin(event, (command.charAt(0) == '/') ? command.substring(1) : command);
	}

	private <E extends PlayerEvent & Cancellable> void combinedBegin(E event, String command) {
		if (event.isCancelled()) {
			debugPrematurelyDenied(event);
			return;
		}
		Player player = event.getPlayer();
		InetAddress address = player.getAddress().getAddress();
		begin(event, guardian.checkChat(player.getUniqueId(), address, command));
	}
	
	@Override
	protected boolean isAllowed(PlayerEvent event) {
		return !((Cancellable) event).isCancelled();
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onChatHigh(AsyncPlayerChatEvent event) {
		combinedWithdraw(event);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onCommandHigh(PlayerCommandPreprocessEvent event) {
		combinedWithdraw(event);
	}
	
	private <E extends PlayerEvent & Cancellable> void combinedWithdraw(E event) {
		CentralisedFuture<Component> futureMessage = withdrawRaw(event);
		if (futureMessage == null) {
			absentFutureHandler(event);
			return;
		}
		Component message;
		if (event.isAsynchronous() || futureMessage.isDone()) {
			message = futureMessage.join();
		} else {
			SyncEnforcement strategy = configs.getMainConfig().enforcement().syncEnforcement();
			switch (strategy) {
			case WAIT:
				message = futureMessage.join();
				break;
			case ALLOW:
				futurePoster.postFuture(futureMessage);
				return;
			case DENY:
				futurePoster.postFuture(futureMessage);
				message = configs.getMessagesConfig().misc().syncDenialMessage();
				break;
			default:
				throw MiscUtil.unknownSyncEnforcement(strategy);
			}
		}
		if (message == null) {
			debugResultPermitted(event);
			return;
		}
		event.setCancelled(true);
		audienceRepresenter.toAudience(event.getPlayer()).sendMessage(message);
	}
	
}

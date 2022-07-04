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
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AbstractEnvEnforcer;
import space.arim.libertybans.core.env.TargetMatcher;
import space.arim.morepaperlib.adventure.MorePaperLibAdventure;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.net.InetAddress;
import java.util.UUID;
import java.util.function.Consumer;

@Singleton
public class SpigotEnforcer extends AbstractEnvEnforcer<CommandSender, Player> {

	private final FactoryOfTheFuture futuresFactory;
	private final Server server;
	private final MorePaperLibAdventure morePaperLibAdventure;

	@Inject
	public SpigotEnforcer(InternalFormatter formatter,
						  AudienceRepresenter<CommandSender> audienceRepresenter,
						  FactoryOfTheFuture futuresFactory, Server server,
						  MorePaperLibAdventure morePaperLibAdventure) {
		super(formatter, audienceRepresenter);
		this.futuresFactory = futuresFactory;
		this.server = server;
		this.morePaperLibAdventure = morePaperLibAdventure;
	}
	
	private void runSyncNow(Runnable command) {
		futuresFactory.runSync(command).join();
	}
	
	@Override
	protected void sendToThoseWithPermissionNoPrefix(String permission, Component message) {
		runSyncNow(() -> {
			for (Player player : server.getOnlinePlayers()) {
				if (player.hasPermission(permission)) {
					audienceRepresenter().toAudience(player).sendMessage(message);
				}
			}
		});
	}

	@Override
	public void kickPlayer(Player player, Component message) {
		morePaperLibAdventure.kickPlayer(player, message);
	}

	@Override
	public void doForPlayerIfOnline(UUID uuid, Consumer<Player> callback) {
		runSyncNow(() -> {
			Player player = server.getPlayer(uuid);
			if (player != null) {
				callback.accept(player);
			}
		});
	}

	@Override
	public void enforceMatcher(TargetMatcher<Player> matcher) {
		runSyncNow(() -> {
			for (Player player : server.getOnlinePlayers()) {
				if (matcher.matches(player.getUniqueId(), player.getAddress().getAddress())) {
					matcher.callback().accept(player);
				}
			}
		});
	}

	@Override
	public UUID getUniqueIdFor(Player player) {
		return player.getUniqueId();
	}

	@Override
	public InetAddress getAddressFor(Player player) {
		return player.getAddress().getAddress();
	}

	@Override
	public void executeConsoleCommand(String command) {
		runSyncNow(() -> server.dispatchCommand(server.getConsoleSender(), command));
	}

}

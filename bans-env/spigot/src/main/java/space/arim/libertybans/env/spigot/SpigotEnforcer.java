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
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.net.InetAddress;
import java.util.UUID;
import java.util.function.Consumer;

@Singleton
public class SpigotEnforcer extends AbstractEnvEnforcer<Player> {

	private final Server server;
	private final MorePaperLibAdventure morePaperLibAdventure;

	@Inject
	public SpigotEnforcer(InternalFormatter formatter,
						  AudienceRepresenter<CommandSender> audienceRepresenter,
						  FactoryOfTheFuture futuresFactory, Server server,
						  MorePaperLibAdventure morePaperLibAdventure) {
		super(futuresFactory, formatter, audienceRepresenter);
		this.server = server;
		this.morePaperLibAdventure = morePaperLibAdventure;
	}

	@SuppressWarnings("unchecked")
	private CentralisedFuture<Void> runSync(Runnable command) {
		// Technically an inaccurate cast, but it will never matter
		return (CentralisedFuture<Void>) futuresFactory().runSync(command);
	}

	@Override
	protected CentralisedFuture<Void> sendToThoseWithPermissionNoPrefix(String permission, Component message) {
		return runSync(() -> {
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
	public CentralisedFuture<Void> doForPlayerIfOnline(UUID uuid, Consumer<Player> callback) {
		return runSync(() -> {
			Player player = server.getPlayer(uuid);
			if (player != null) {
				callback.accept(player);
			}
		});
	}

	@Override
	public CentralisedFuture<Void> enforceMatcher(TargetMatcher<Player> matcher) {
		return runSync(() -> {
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
	public CentralisedFuture<Void> executeConsoleCommand(String command) {
		return runSync(() -> server.dispatchCommand(server.getConsoleSender(), command));
	}

}

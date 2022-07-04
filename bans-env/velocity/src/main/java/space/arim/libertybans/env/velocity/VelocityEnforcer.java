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

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AbstractEnvEnforcer;
import space.arim.libertybans.core.env.TargetMatcher;

import java.net.InetAddress;
import java.util.UUID;
import java.util.function.Consumer;

@Singleton
public class VelocityEnforcer extends AbstractEnvEnforcer<CommandSource, Player> {

	private final ProxyServer server;
	
	@Inject
	public VelocityEnforcer(InternalFormatter formatter, ProxyServer server) {
		super(formatter, AudienceRepresenter.identity());
		this.server = server;
	}

	@Override
	protected void sendToThoseWithPermissionNoPrefix(String permission, Component message) {
		for (Player player : server.getAllPlayers()) {
			if (player.hasPermission(permission)) {
				player.sendMessage(message);
			}
		}
	}

	@Override
	public void kickPlayer(Player player, Component message) {
		player.disconnect(message);
	}

	@Override
	public void doForPlayerIfOnline(UUID uuid, Consumer<Player> callback) {
		server.getPlayer(uuid).ifPresent(callback);
	}

	@Override
	public void enforceMatcher(TargetMatcher<Player> matcher) {
		for (Player player : server.getAllPlayers()) {
			if (matcher.matches(player.getUniqueId(), player.getRemoteAddress().getAddress())) {
				matcher.callback().accept(player);
			}
		}
	}

	@Override
	public UUID getUniqueIdFor(Player player) {
		return player.getUniqueId();
	}

	@Override
	public InetAddress getAddressFor(Player player) {
		return player.getRemoteAddress().getAddress();
	}

	@Override
	public void executeConsoleCommand(String command) {
		server.getCommandManager()
				.executeAsync(server.getConsoleCommandSource(), command)
				.exceptionally((ex) -> {
					Logger logger = LoggerFactory.getLogger(VelocityEnforcer.class);
					logger.warn("Exception occurred while executing console command {}", command, ex);
					return null;
				});
	}

}

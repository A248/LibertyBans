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

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AbstractCmdSender;
import space.arim.libertybans.core.env.Interlocutor;

import java.util.stream.Stream;

public abstract class VelocityCmdSender extends AbstractCmdSender<CommandSource> {

	private final ProxyServer server;

	VelocityCmdSender(InternalFormatter formatter, Interlocutor interlocutor,
					  CommandSource sender, Operator operator, ProxyServer server) {
		super(formatter, interlocutor, AudienceRepresenter.identity(), sender, operator);
		this.server = server;
	}
	
	@Override
	public final boolean hasPermission(String permission) {
		return getRawSender().hasPermission(permission);
	}

	@Override
	public final Stream<String> getPlayerNames() {
		return server.getAllPlayers().stream().map(Player::getUsername);
	}

	static class PlayerSender extends VelocityCmdSender {

		PlayerSender(InternalFormatter formatter, Interlocutor interlocutor,
					 Player player, ProxyServer server) {
			super(formatter, interlocutor, player, PlayerOperator.of(player.getUniqueId()), server);
		}

		@Override
		public Player getRawSender() {
			return (Player) super.getRawSender();
		}

		@Override
		public Stream<String> getPlayerNamesOnSameServer() {
			Player player = getRawSender();
			ServerConnection serverConnection = player.getCurrentServer().orElse(null);
			if (serverConnection == null) {
				return Stream.empty();
			}
			return serverConnection.getServer().getPlayersConnected().stream().map(Player::getUsername);
		}
		
	}

	static class ConsoleSender extends VelocityCmdSender {

		ConsoleSender(InternalFormatter formatter, Interlocutor interlocutor,
					  CommandSource sender, ProxyServer server) {
			super(formatter, interlocutor, sender, ConsoleOperator.INSTANCE, server);
		}

		@Override
		public Stream<String> getPlayerNamesOnSameServer() {
			return getPlayerNames();
		}
		
	}

}

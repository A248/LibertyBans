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

import java.util.stream.Stream;

public abstract class VelocityCmdSender extends AbstractCmdSender<CommandSource> {

	VelocityCmdSender(InternalFormatter formatter, CommandSource sender, Operator operator) {
		super(formatter, AudienceRepresenter.identity(), sender, operator);
	}
	
	@Override
	public boolean hasPermission(String permission) {
		return getRawSender().hasPermission(permission);
	}
	
	static class PlayerSender extends VelocityCmdSender {

		PlayerSender(InternalFormatter formatter, Player player) {
			super(formatter, player, PlayerOperator.of(player.getUniqueId()));
		}
		
		@Override
		public Player getRawSender() {
			return (Player) super.getRawSender();
		}
		
		@Override
		public Stream<String> getPlayersOnSameServer() {
			Player player = getRawSender();
			ServerConnection serverConnection = player.getCurrentServer().orElse(null);
			if (serverConnection == null) {
				return Stream.empty();
			}
			return serverConnection.getServer().getPlayersConnected().stream().map(Player::getUsername);
		}
		
	}

	static class ConsoleSender extends VelocityCmdSender {

		private final ProxyServer server;
		
		ConsoleSender(InternalFormatter formatter, CommandSource sender, ProxyServer server) {
			super(formatter, sender, ConsoleOperator.INSTANCE);
			this.server = server;
		}
		
		@Override
		public Stream<String> getPlayersOnSameServer() {
			return server.getAllPlayers().stream().map(Player::getUsername);
		}
		
	}

}

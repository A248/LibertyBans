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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AbstractCmdSender;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;

public abstract class VelocityCmdSender extends AbstractCmdSender<CommandSource> {

	VelocityCmdSender(InternalFormatter formatter, CommandSource sender, Operator operator) {
		super(formatter, AudienceRepresenter.identity(), sender, operator);
	}
	
	@Override
	public boolean hasPermission(String permission) {
		return getRawSender().hasPermission(permission);
	}
	
	Set<String> playersToNames(Collection<Player> players, Player exclude) {
		Set<String> result = new HashSet<>(players.size());
		for (Player player : players) {
			if (exclude != null && exclude.getUniqueId().equals(player.getUniqueId())) {
				continue;
			}
			result.add(player.getUsername());
		}
		return result;
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
		public Set<String> getOtherPlayersOnSameServer() {
			Player player = getRawSender();
			ServerConnection serverConnection = player.getCurrentServer().orElse(null);
			if (serverConnection == null) {
				return Set.of();
			}
			Collection<Player> players = serverConnection.getServer().getPlayersConnected();
			return playersToNames(players, player);
		}
		
	}

	static class ConsoleSender extends VelocityCmdSender {

		private final ProxyServer server;
		
		ConsoleSender(InternalFormatter formatter, CommandSource sender, ProxyServer server) {
			super(formatter, sender, ConsoleOperator.INSTANCE);
			this.server = server;
		}
		
		@Override
		public Set<String> getOtherPlayersOnSameServer() {
			return playersToNames(server.getAllPlayers(), null);
		}
		
	}

}

/* 
 * LibertyBans-env-bungee
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-bungee is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-bungee is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-bungee. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Plugin;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AbstractCmdSender;

import java.util.stream.Stream;

public abstract class BungeeCmdSender extends AbstractCmdSender<CommandSender> {

	BungeeCmdSender(InternalFormatter formatter, AudienceRepresenter<CommandSender> audienceRepresenter,
					CommandSender sender, Operator operator) {
		super(formatter, audienceRepresenter, sender, operator);
	}

	@Override
	public boolean hasPermission(String permission) {
		return getRawSender().hasPermission(permission);
	}
	
	static class PlayerSender extends BungeeCmdSender {

		PlayerSender(InternalFormatter formatter, AudienceRepresenter<CommandSender> audienceRepresenter,
					 ProxiedPlayer player) {
			super(formatter, audienceRepresenter,
					player, PlayerOperator.of(player.getUniqueId()));
		}
		
		@Override
		public ProxiedPlayer getRawSender() {
			return (ProxiedPlayer) super.getRawSender();
		}

		@Override
		public Stream<String> getPlayersOnSameServer() {
			ProxiedPlayer player = getRawSender();
			Server server = player.getServer();
			if (server == null) { // There is no documented contract whether this is null
				return Stream.empty();
			}
			return server.getInfo().getPlayers().stream().map(ProxiedPlayer::getName);
		}
		
	}
	
	static class ConsoleSender extends BungeeCmdSender {

		private final Plugin plugin;
		
		ConsoleSender(InternalFormatter formatter, AudienceRepresenter<CommandSender> audienceRepresenter,
					  CommandSender sender, Plugin plugin) {
			super(formatter, audienceRepresenter, sender, ConsoleOperator.INSTANCE);
			this.plugin = plugin;
		}

		@Override
		public Stream<String> getPlayersOnSameServer() {
			return plugin.getProxy().getPlayers().stream().map(ProxiedPlayer::getName);
		}

	}
	
}

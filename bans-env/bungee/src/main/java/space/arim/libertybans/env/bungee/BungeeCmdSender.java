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
import space.arim.libertybans.core.env.Interlocutor;

import java.util.stream.Stream;

public abstract class BungeeCmdSender extends AbstractCmdSender<CommandSender> {

	private final Plugin plugin;

	BungeeCmdSender(InternalFormatter formatter, Interlocutor interlocutor,
					AudienceRepresenter<CommandSender> audienceRepresenter,
					CommandSender sender, Operator operator, Plugin plugin) {
		super(formatter, interlocutor, audienceRepresenter, sender, operator);
		this.plugin = plugin;
	}

	@Override
	public final Stream<String> getPlayerNames() {
		return plugin.getProxy().getPlayers().stream().map(ProxiedPlayer::getName);
	}

	@Override
	public final boolean hasPermission(String permission) {
		return getRawSender().hasPermission(permission);
	}

	static class PlayerSender extends BungeeCmdSender {

		PlayerSender(InternalFormatter formatter, Interlocutor interlocutor,
					 AudienceRepresenter<CommandSender> audienceRepresenter,
					 ProxiedPlayer player, Plugin plugin) {
			super(formatter, interlocutor, audienceRepresenter,
					player, PlayerOperator.of(player.getUniqueId()), plugin);
		}

		@Override
		public ProxiedPlayer getRawSender() {
			return (ProxiedPlayer) super.getRawSender();
		}

		@Override
		public Stream<String> getPlayerNamesOnSameServer() {
			ProxiedPlayer player = getRawSender();
			Server server = player.getServer();
			if (server == null) { // There is no documented contract whether this is null
				return Stream.empty();
			}
			return server.getInfo().getPlayers().stream().map(ProxiedPlayer::getName);
		}

	}

	static class ConsoleSender extends BungeeCmdSender {

		ConsoleSender(InternalFormatter formatter, Interlocutor interlocutor,
					  AudienceRepresenter<CommandSender> audienceRepresenter,
					  CommandSender sender, Plugin plugin) {
			super(formatter, interlocutor, audienceRepresenter, sender, ConsoleOperator.INSTANCE, plugin);
		}

		@Override
		public Stream<String> getPlayerNamesOnSameServer() {
			return getPlayerNames();
		}

	}
	
}

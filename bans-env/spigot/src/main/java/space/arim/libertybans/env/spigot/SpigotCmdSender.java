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

package space.arim.libertybans.env.spigot;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AbstractCmdSender;
import space.arim.libertybans.core.env.Interlocutor;

import java.util.stream.Stream;

public abstract class SpigotCmdSender extends AbstractCmdSender<CommandSender> {

	private final Plugin plugin;

	SpigotCmdSender(InternalFormatter formatter, Interlocutor interlocutor,
					AudienceRepresenter<CommandSender> audienceRepresenter,
					CommandSender sender, Operator operator, Plugin plugin) {
		super(formatter, interlocutor, audienceRepresenter, sender, operator);
		this.plugin = plugin;
	}

	@Override
	public final Stream<String> getPlayerNames() {
		return plugin.getServer().getOnlinePlayers().stream().map(Player::getName);
	}

	@Override
	public final Stream<String> getPlayerNamesOnSameServer() {
		return getPlayerNames();
	}

	@Override
	public final boolean hasPermission(String permission) {
		return getRawSender().hasPermission(permission);
	}

	static class PlayerSender extends SpigotCmdSender {

		PlayerSender(InternalFormatter formatter, Interlocutor interlocutor,
					 AudienceRepresenter<CommandSender> audienceRepresenter,
					 Player player, Plugin plugin) {
			super(formatter, interlocutor, audienceRepresenter,
					player, PlayerOperator.of(player.getUniqueId()), plugin);
		}

	}

	static class ConsoleSender extends SpigotCmdSender {

		ConsoleSender(InternalFormatter formatter, Interlocutor interlocutor,
					  AudienceRepresenter<CommandSender> audienceRepresenter,
					  CommandSender sender, Plugin plugin) {
			super(formatter, interlocutor, audienceRepresenter,
					sender, ConsoleOperator.INSTANCE, plugin);
		}

	}
	
}

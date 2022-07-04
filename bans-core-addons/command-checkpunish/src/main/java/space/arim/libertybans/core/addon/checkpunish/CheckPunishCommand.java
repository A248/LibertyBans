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

package space.arim.libertybans.core.addon.checkpunish;

import jakarta.inject.Inject;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.commands.AbstractSubCommandGroup;
import space.arim.libertybans.core.commands.CommandExecution;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.CmdSender;

import java.util.stream.Stream;

public final class CheckPunishCommand extends AbstractSubCommandGroup {

	private final PunishmentSelector selector;
	private final InternalFormatter formatter;
	private final CheckPunishAddon addon;

	@Inject
	public CheckPunishCommand(Dependencies dependencies,
							  PunishmentSelector selector, InternalFormatter formatter, CheckPunishAddon addon) {
		super(dependencies, "checkpunish");
		this.selector = selector;
		this.formatter = formatter;
		this.addon = addon;
	}

	@Override
	public Stream<String> suggest(CmdSender sender, String arg, int argIndex) {
		return Stream.empty();
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		CheckPunishConfig config = addon.config();
		return () -> {
			if (!sender.hasPermission("libertybans.addon.checkpunish.use")) {
				sender.sendMessage(config.noPermission());
				return null;
			}
			if (!command.hasNext()) {
				sender.sendMessage(config.usage());
				return null;
			}
			long id;
			try {
				id = Long.parseLong(command.next());
			} catch (NumberFormatException ex) {
				sender.sendMessage(config.usage());
				return null;
			}
			return selector.getActivePunishmentById(id).thenCompose((optPunishment) -> {
				if (optPunishment.isEmpty()) {
					sender.sendMessage(config.doesNotExist());
					return completedFuture(null);
				}
				Punishment punishment = optPunishment.get();
				return formatter.formatWithPunishment(config.layout(), punishment).thenAccept(sender::sendMessage);
			});
		};
	}
}

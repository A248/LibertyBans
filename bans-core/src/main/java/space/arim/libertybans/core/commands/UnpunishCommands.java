/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.commands;

import java.util.Arrays;
import java.util.Locale;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.chat.SendableMessage;
import space.arim.api.configure.ConfigAccessor;

import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentSelection;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.MiscUtil;
import space.arim.libertybans.core.env.CmdSender;

public class UnpunishCommands extends SubCommandGroup {
	
	UnpunishCommands(Commands commands) {
		super(commands, Arrays.stream(MiscUtil.punishmentTypes()).filter((type) -> type != PunishmentType.KICK)
				.map((type) -> type.name().toLowerCase(Locale.ENGLISH)).toArray(String[]::new));
	}

	@Override
	void execute(CmdSender sender, CommandPackage command, String arg) {
		execute(commands.core, commands.messages(), sender, command, PunishmentType.valueOf(arg.substring(2)));
	}
	
	private void execute(LibertyBansCore core, ConfigAccessor messages, CmdSender sender, CommandPackage command, PunishmentType type) {
		if (!sender.hasPermission("libertybans." + type.getLowercaseName() + ".undo")) { // libertybans.ban.undo
			sender.parseThenSend(messages.getString(
					"removals." + type.getLowercaseNamePlural() + ".permission.command")); // removals.bans.permission.command
			return;
		}
		if (!command.hasNext()) {
			sender.parseThenSend(messages.getString(
					"removals." + type.getLowercaseNamePlural() + ".usage")); // removals.bans.usage
			return;
		}
		String name = command.next();
		core.getUUIDMaster().fullLookupUUID(name).thenCompose((uuid) -> {
			if (uuid == null) {
				sender.parseThenSend(messages.getString("all.not-found.uuid").replace("%TARGET%", name));
				return core.getFuturesFactory().completedFuture(null);
			}
			PunishmentSelection selection = new PunishmentSelection.Builder().type(type)
					.victim(PlayerVictim.of(uuid)).build();
			return core.getSelector().getFirstSpecificPunishment(selection).thenApply((nullIfNotFound) -> {
				if (nullIfNotFound == null) {
					String configPath = "removals." + type.getLowercaseNamePlural() + ".not-found"; // removals.bans.not-found
					sender.parseThenSend(messages.getString(configPath).replace("%TARGET%", name));
				}
				return nullIfNotFound;
			});
		}).thenAccept((punishment) -> {
			if (punishment == null) {
				return;
			}
			// Success message
			String rawMsg = messages.getString(
					"removals." + type.getLowercaseNamePlural() + ".successful.message"); // removals.bans.successful.message
			CentralisedFuture<SendableMessage> futureMsg = core.getFormatter().formatWithPunishment(rawMsg, punishment);
			assert futureMsg.isDone();
			sender.sendMessage(futureMsg.join());

			// Notification
			String rawNotify = messages.getString(
					"removals." + type.getLowercaseNamePlural() + ".successful.notification"); // removals.bans.successful.notification
			CentralisedFuture<SendableMessage> futureNotify = core.getFormatter().formatWithPunishment(rawNotify, punishment);
			assert futureNotify.isDone();
			core.getEnvironment().getEnforcer().sendToThoseWithPermission(
					"libertybans." + type.getLowercaseName() + ".unnotify", futureNotify.join()); // libertybans.ban.unnotify
		});
	}

}

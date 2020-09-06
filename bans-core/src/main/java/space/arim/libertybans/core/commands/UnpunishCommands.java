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
import java.util.concurrent.CompletableFuture;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.chat.SendableMessage;

import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.MiscUtil;
import space.arim.libertybans.core.env.CmdSender;

public class UnpunishCommands extends AbstractSubCommandGroup {
	
	UnpunishCommands(Commands commands) {
		super(commands, Arrays.stream(MiscUtil.punishmentTypesExcludingKick())
				.map((type) -> "un" + type.name().toLowerCase(Locale.ENGLISH)).toArray(String[]::new));
	}

	@Override
	public void execute(CmdSender sender, CommandPackage command, String arg) {
		execute(sender, command, PunishmentType.valueOf(arg.substring(2).toUpperCase(Locale.ENGLISH)));
	}
	
	private void execute(CmdSender sender, CommandPackage command,
			PunishmentType type) {
		if (!sender.hasPermission("libertybans." + type.getLowercaseName() + ".undo")) { // libertybans.ban.undo
			sender.parseThenSend(messages().getString(
					"removals." + type.getLowercaseNamePlural() + ".permission.command")); // removals.bans.permission.command
			return;
		}
		if (!command.hasNext()) {
			sender.parseThenSend(messages().getString(
					"removals." + type.getLowercaseNamePlural() + ".usage")); // removals.bans.usage
			return;
		}
		String targetArg = command.next();
		commands.parseVictim(sender, targetArg).thenCompose((victim) -> {
			if (victim == null) {
				return completedFuture(null);
			}
			CentralisedFuture<Punishment> futureUndo = null;
			final int finalId;
			if (type == PunishmentType.WARN) {

				if (!command.hasNext()) {
					sender.parseThenSend(messages().getString("removals.warns.usage"));
					return completedFuture(null);
				}
				String idArg = command.next();
				int id;
				try {
					id = Integer.parseInt(idArg);
				} catch (NumberFormatException ignored) {
					sender.parseThenSend(messages().getString("removals.warns.not-a-number").replace("%ID_ARG%", idArg));
					return completedFuture(null);
				}
				futureUndo = core().getEnactor().undoAndGetPunishmentByIdAndType(id, type);
				finalId = id;
			} else {
				assert type.isSingular() : type;
				futureUndo = core().getEnactor().undoAndGetPunishmentByTypeAndVictim(type, victim);
				finalId = -1;
			}
			return futureUndo.thenApply((nullIfNotFound) -> {
				if (nullIfNotFound == null) {
					String configPath = "removals." + type.getLowercaseNamePlural() + ".not-found"; // removals.bans.not-found
					String rawMessage = messages().getString(configPath).replace("%TARGET%", targetArg);
					if (type == PunishmentType.WARN) {
						rawMessage = rawMessage.replace("%ID%", Integer.toString(finalId));
					}
					sender.parseThenSend(rawMessage);
				}
				return nullIfNotFound;
			});
		}).thenCompose((punishment) -> {
			if (punishment == null) {
				return completedFuture(null);
			}
			// Success message
			String rawMsg = messages().getString(
					"removals." + type.getLowercaseNamePlural() + ".successful.message"); // removals.bans.successful.message
			CentralisedFuture<SendableMessage> futureSuccessMessage = core().getFormatter().formatWithPunishment(rawMsg, punishment);

			// Notification
			CentralisedFuture<SendableMessage> futureNotify = core().getFormatter().formatOperator(sender.getOperator())
					.thenCompose((operatorFormatted) -> {
						String rawNotify = messages()
								.getString("removals." + type.getLowercaseNamePlural() + ".successful.notification"); // removals.bans.successful.notification
						rawNotify = rawNotify.replace("%UNOPERATOR%", operatorFormatted);
						return core().getFormatter().formatWithPunishment(rawNotify, punishment);
					});

			// Conclusion
			return CompletableFuture.allOf(futureSuccessMessage, futureNotify).thenAccept((ignore) -> {
				sender.sendMessage(futureSuccessMessage.join());

				core().getEnvironment().getEnforcer().sendToThoseWithPermission(
							"libertybans." + type.getLowercaseName() + ".unnotify", futureNotify.join()); // libertybans.ban.unnotify
			});
		}).whenComplete(core()::debugFuture);
	}

}

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
import space.arim.api.chat.manipulator.SendableMessageManipulator;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.config.RemovalsSection.PunishmentRemoval;
import space.arim.libertybans.core.config.RemovalsSection.WarnRemoval;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.punish.MiscUtil;

public class UnpunishCommands extends AbstractSubCommandGroup {
	
	UnpunishCommands(Commands commands) {
		super(commands, Arrays.stream(MiscUtil.punishmentTypesExcludingKick())
				.map((type) -> "un" + type.name().toLowerCase(Locale.ROOT)));
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		return new Execution(sender, command, PunishmentType.valueOf(arg.substring(2).toUpperCase(Locale.ROOT)));
	}
	
	private class Execution extends TypeSpecificExecution {
		
		private final PunishmentRemoval section;

		Execution(CmdSender sender, CommandPackage command, PunishmentType type) {
			super(sender, command, type);
			section = messages().removals().forType(type);
		}

		@Override
		public void execute() {
			if (!sender().hasPermission("libertybans.un" + type().getLowercaseName() + ".command")) { // libertybans.unban.command
				sender().sendMessage(section.permissionCommand());
				return;
			}
			if (!command().hasNext()) {
				sender().sendMessage(section.usage());
				return;
			}
			execute0();
		}
		
		private void execute0() {
			String targetArg = command().next();
			CentralisedFuture<?> future = commands.parseVictim(sender(), targetArg).thenCompose((victim) -> {
				if (victim == null) {
					return completedFuture(null);
				}
				return performUndo(victim, targetArg);

			}).thenCompose((punishment) -> {
				if (punishment == null) {
					return completedFuture(null);
				}
				return sendSuccess(punishment);
			});
			core().postFuture(future);
		}
		
		private CentralisedFuture<Punishment> performUndo(Victim victim, String targetArg) {
			CmdSender sender = sender();
			CommandPackage command = command();
			PunishmentType type = type();

			CentralisedFuture<Punishment> futureUndo = null;
			final int finalId;
			if (type == PunishmentType.WARN) {

				if (!command.hasNext()) {
					sender.sendMessage(section.usage());
					return completedFuture(null);
				}
				String idArg = command.next();
				int id;
				try {
					id = Integer.parseInt(idArg);
				} catch (NumberFormatException ignored) {
					sender.sendMessage(((WarnRemoval) section).notANumber().replaceText("%ID_ARG%", idArg));
					return completedFuture(null);
				}
				futureUndo = core().getRevoker().revokeByIdAndType(id, type).undoAndGetPunishmentWithoutUnenforcement();
				finalId = id;
			} else {
				assert type.isSingular() : type;
				futureUndo = core().getRevoker().revokeByTypeAndVictim(type, victim).undoAndGetPunishmentWithoutUnenforcement();
				finalId = -1;
			}
			return futureUndo.thenApply((nullIfNotFound) -> {
				if (nullIfNotFound == null) {
					SendableMessage notFound = section.notFound().replaceText("%TARGET%", targetArg);
					if (type == PunishmentType.WARN) {
						notFound = SendableMessageManipulator.create(notFound).replaceText("%ID%", Integer.toString(finalId));
					}
					sender.sendMessage(notFound);
				}
				return nullIfNotFound;
			});
		}
		
		private CentralisedFuture<?> sendSuccess(Punishment punishment) {

			CentralisedFuture<?> unenforcement = punishment.unenforcePunishment();
			CentralisedFuture<SendableMessage> successMessage = core().getFormatter().formatWithPunishment(
					section.successMessage(), punishment);
			CentralisedFuture<SendableMessage> successNotify = core().getFormatter().formatWithPunishmentAndUnoperator(
					section.successNotification(), punishment, sender().getOperator());

			return core().getFuturesFactory().allOf(unenforcement, successMessage, successNotify).thenAccept((ignore) -> {
				sender().sendMessage(successMessage.join());

				SendableMessage fullNotify = core().getFormatter().prefix(successNotify.join());
				String notifyPerm = "libertybans." + type().getLowercaseName() + ".unnotify";
				core().getEnvironment().getEnforcer().sendToThoseWithPermission(notifyPerm, fullNotify);
			});
		}
	}

}

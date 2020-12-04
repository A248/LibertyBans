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

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import space.arim.api.chat.SendableMessage;
import space.arim.api.chat.manipulator.SendableMessageManipulator;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.revoke.PunishmentRevoker;
import space.arim.libertybans.api.revoke.RevocationOrder;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.config.RemovalsSection.PunishmentRemoval;
import space.arim.libertybans.core.config.RemovalsSection.WarnRemoval;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.EnvEnforcer;

abstract class UnpunishCommands extends AbstractSubCommandGroup implements PunishUnpunishCommands {
	
	private final PunishmentRevoker revoker;
	private final InternalFormatter formatter;
	private final EnvEnforcer envEnforcer;
	
	UnpunishCommands(Dependencies dependencies, Stream<String> matches,
			PunishmentRevoker revoker, InternalFormatter formatter, EnvEnforcer envEnforcer) {
		super(dependencies, matches);
		this.revoker = revoker;
		this.formatter = formatter;
		this.envEnforcer = envEnforcer;
	}

	@Override
	public final CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		return new Execution(sender, command, parseType(arg.toUpperCase(Locale.ROOT)));
	}
	
	@Override
	public final Collection<String> suggest(CmdSender sender, String arg, int argIndex) {
		switch (argIndex) {
		case 0:
			return getMatches();
		case 1:
			PunishmentType type = parseType(arg.toUpperCase(Locale.ROOT));
			if (type == PunishmentType.MUTE || type == PunishmentType.WARN) {
				return sender.getOtherPlayersOnSameServer();
			}
			break;
		default:
			break;
		}
		return Set.of();
	}
	
	private class Execution extends TypeSpecificExecution {
		
		private final PunishmentRemoval section;

		Execution(CmdSender sender, CommandPackage command, PunishmentType type) {
			super(sender, command, type);
			section = messages().removals().forType(type);
		}

		@Override
		public void execute() {
			if (!sender().hasPermission("libertybans." + type() + ".undo")) {
				sender().sendMessage(section.permissionCommand());
				return;
			}
			String additionalPermission = getAdditionalPermission(type());
			if (!additionalPermission.isEmpty() && !sender().hasPermission(additionalPermission)) {
				sender().sendMessage(section.permissionIpAddress());
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
			CentralisedFuture<?> future = parseVictim(sender(), targetArg).thenCompose((victim) -> {
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
			postFuture(future);
		}

		private ReactionStage<Punishment> performUndo(Victim victim, String targetArg) {
			CmdSender sender = sender();
			CommandPackage command = command();
			PunishmentType type = type();

			RevocationOrder revocationOrder;
			final int id;
			if (type == PunishmentType.WARN) {

				if (!command.hasNext()) {
					sender.sendMessage(section.usage());
					return completedFuture(null);
				}
				String idArg = command.next();
				try {
					id = Integer.parseInt(idArg);
				} catch (NumberFormatException ignored) {
					sender.sendMessage(((WarnRemoval) section).notANumber().replaceText("%ID_ARG%", idArg));
					return completedFuture(null);
				}
				revocationOrder = revoker.revokeByIdAndType(id, type);
			} else {
				assert type.isSingular() : type;
				revocationOrder = revoker.revokeByTypeAndVictim(type, victim);
				id = -1;
			}
			return revocationOrder.undoAndGetPunishmentWithoutUnenforcement().thenApply((optPunishment) -> {
				if (optPunishment.isEmpty()) {
					SendableMessage notFound = section.notFound().replaceText("%TARGET%", targetArg);
					if (type == PunishmentType.WARN) {
						notFound = SendableMessageManipulator.create(notFound).replaceText("%ID%", Integer.toString(id));
					}
					sender.sendMessage(notFound);
					return null;
				}
				return optPunishment.get();
			});
		}

		private CentralisedFuture<?> sendSuccess(Punishment punishment) {

			CentralisedFuture<?> unenforcement = punishment.unenforcePunishment().toCompletableFuture();
			CentralisedFuture<SendableMessage> successMessage = formatter.formatWithPunishment(
					section.successMessage(), punishment);
			CentralisedFuture<SendableMessage> successNotify = formatter.formatWithPunishmentAndUnoperator(
					section.successNotification(), punishment, sender().getOperator());

			return futuresFactory().allOf(unenforcement, successMessage, successNotify).thenRun(() -> {
				sender().sendMessage(successMessage.join());

				SendableMessage fullNotify = formatter.prefix(successNotify.join());
				String notifyPerm = "libertybans." + type() + ".unnotify";
				envEnforcer.sendToThoseWithPermission(notifyPerm, fullNotify);
			});
		}
	}

}

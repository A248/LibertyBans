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

import java.time.Duration;
import java.util.Locale;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.chat.SendableMessage;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.config.AdditionsSection.PunishmentAddition;
import space.arim.libertybans.core.config.AdditionsSection.ExclusivePunishmentAddition;
import space.arim.libertybans.core.config.MainConfig;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.punish.MiscUtil;

public class PunishCommands extends AbstractSubCommandGroup {

	PunishCommands(Commands commands) {
		super(commands, MiscUtil.punishmentTypes().stream().map((type) -> type.name().toLowerCase(Locale.ROOT)));
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		return new Execution(sender, command, PunishmentType.valueOf(arg.toUpperCase(Locale.ROOT)));
	}
	
	private class Execution extends TypeSpecificExecution {

		private final PunishmentAddition section;
		
		Execution(CmdSender sender, CommandPackage command, PunishmentType type) {
			super(sender, command, type);
			section = messages().additions().forType(type);
		}

		@Override
		public void execute() {
			if (!sender().hasPermission("libertybans." + type().getLowercaseName() + ".command")) { // libertybans.ban.command
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
				return performEnact(victim, targetArg);

			}).thenCompose((punishment) -> {
				if (punishment == null) {
					return completedFuture(null);
				}
				return enforceAndSendSuccess(punishment);
			});
			core().postFuture(future);
		}
		
		private CentralisedFuture<Punishment> performEnact(Victim victim, String targetArg) {
			final Duration duration;
			if (type() != PunishmentType.KICK && command().hasNext()) {
				String time = command().peek();
				duration = new DurationParser(time, messages().formatting().permanentArguments()).parse();
				if (!duration.isZero()) {
					command().next();
				}
			} else {
				duration = Duration.ZERO;
			}
			String reason;
			MainConfig.Reasons reasonsCfg;
			if (command().hasNext()) {
				reason = command().allRemaining();
			} else if ((reasonsCfg = core().getMainConfig().reasons()).permitBlank()) {
				reason = "";
			} else {
				reason = reasonsCfg.defaultReason();
			}
			DraftPunishment draftPunishment = core().getDrafter().draftBuilder()
					.victim(victim).operator(sender().getOperator()).type(type()).reason(reason)
					.duration(duration)
					.build();

			return draftPunishment.enactPunishmentWithoutEnforcement().thenApply((nullIfConflict) -> {
				if (nullIfConflict == null) {
					sendConflict(targetArg);
				}
				return nullIfConflict;
			});
		}
		
		private void sendConflict(String targetArg) {
			SendableMessage message;
			if (type().isSingular()) {
				message = ((ExclusivePunishmentAddition) section).conflicting().replaceText("%TARGET%", targetArg);
			} else {
				message = messages().misc().unknownError();
			}
			sender().sendMessage(message);
		}
		
		private CentralisedFuture<?> enforceAndSendSuccess(Punishment punishment) {

			CentralisedFuture<?> enforcement = punishment.enforcePunishment();
			CentralisedFuture<SendableMessage> successMessage = core().getFormatter().formatWithPunishment(
					section.successMessage(), punishment);
			CentralisedFuture<SendableMessage> successNotify = core().getFormatter().formatWithPunishment(
					section.successNotification(), punishment);

			return core().getFuturesFactory().allOf(enforcement, successMessage, successNotify).thenAccept((ignore) -> {
				sender().sendMessage(successMessage.join());

				SendableMessage fullNotify = core().getFormatter().prefix(successNotify.join());
				String notifyPerm = "libertybans." + type().getLowercaseName() + ".notify";
				core().getEnvironment().getEnforcer().sendToThoseWithPermission(notifyPerm, fullNotify);
			});
		}
		
	}

}

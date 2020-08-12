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

import space.arim.libertybans.api.DraftPunishment;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.env.CmdSender;

public class PunishCommands extends SubCommandGroup {

	PunishCommands(Commands commands) {
		super(commands, Arrays.stream(PunishmentType.values()).map((type) -> type.name().toLowerCase(Locale.ENGLISH))
				.toArray(String[]::new));
	}

	@Override
	void execute(CmdSender sender, CommandPackage command, String arg) {
		execute(commands.core, commands.messages, sender, command, PunishmentType.valueOf(arg));
	}
	
	private void execute(LibertyBansCore core, ConfigAccessor messages, CmdSender sender, CommandPackage command, PunishmentType type) {
		if (!sender.hasPermission("libertybans." + type.getLowercaseName() + ".do")) { // libertybans.ban.do
			sender.parseThenSend(
					messages.getString(
							"additions." + type.getLowercaseNamePlural() + ".permission.command")); // additions.bans.permission.command
			return;
		}
		if (!command.hasNext()) {
			sender.parseThenSend(messages.getString(
					"additions." + type.getLowercaseNamePlural() + ".usage")); // additions.bans.usage
			return;
		}
		String targetArg = command.next();
		core.getUUIDMaster().fullLookupUUID(targetArg).thenCompose((uuid) -> {
			if (uuid == null) {
				sender.parseThenSend(messages.getString("all.not-found.uuid").replace("%TARGET%", targetArg));
				return core.getFuturesFactory().completedFuture(null);
			}
			String reason;
			if (command.hasNext()) {
				reason = command.allRemaining();
			} else if (core.getConfigs().getConfig().getBoolean("reasons.permit-blank")) {
				reason = "";
			} else {
				reason = core.getConfigs().getConfig().getString("reasons.default-reason");
			}
			DraftPunishment draftBan = new DraftPunishment.Builder().victim(PlayerVictim.of(uuid))
					.operator(sender.getOperator()).type(type).reason(reason)
					.scope(core.getScopeManager().globalScope()).build();

			return core.getEnactor().enactPunishment(draftBan).thenApply((nullIfConflict) -> {
				assert type == PunishmentType.BAN || type == PunishmentType.MUTE : type;
				if (nullIfConflict == null) {
					String configPath = "additions." + type.getLowercaseNamePlural() + ".error.conflicting"; // additions.bans.error.conflicting
					sender.parseThenSend(messages.getString(configPath).replace("%TARGET%", targetArg));
				}
				return nullIfConflict;
			});
		}).thenAccept((punishment) -> {
			if (punishment == null) {
				return;
			}
			// Success message
			String rawMsg = messages.getString(
					"additions." + type.getLowercaseNamePlural() + ".successful.message"); // additions.bans.successful.message
			CentralisedFuture<SendableMessage> futureMsg = core.getFormatter().formatWithPunishment(rawMsg, punishment);
			assert futureMsg.isDone();
			sender.sendMessage(futureMsg.join());

			// Enforcement
			core.getEnforcer().enforce(punishment);

			// Notification
			String notifyPerm = "libertybans." + type.getLowercaseName() + ".notify"; // libertybans.ban.notify
			String configMsgPath = "addition." + type.getLowercaseNamePlural() + ".successful.notification"; // addition.bans.successful.notification
			String rawNotify = messages.getString(configMsgPath);
			CentralisedFuture<SendableMessage> futureNotify = core.getFormatter().formatWithPunishment(rawNotify, punishment);
			assert futureNotify.isDone();
			core.getEnvironment().getEnforcer().sendToThoseWithPermission(notifyPerm, futureNotify.join());
		});
	}

}

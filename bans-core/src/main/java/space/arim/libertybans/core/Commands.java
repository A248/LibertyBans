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
package space.arim.libertybans.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.universal.util.ThisClass;
import space.arim.universal.util.concurrent.CentralisedFuture;

import space.arim.api.util.config.Config;
import space.arim.api.util.config.ConfigLoadValuesFromFileException;

import space.arim.libertybans.api.DraftPunishment;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentSelection;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.commands.PrefixedCmdSender;
import space.arim.libertybans.core.env.CmdSender;

public class Commands {

	private final LibertyBansCore core;
	
	// Cached for convenience
	private final Config config;
	private final Config messages;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	Commands(LibertyBansCore core) {
		this.core = core;
		config = core.getConfigs().getConfig();
		messages = core.getConfigs().getMessages();
	}
	
	public void execute(CmdSender sender, CommandPackage command) {
		if (messages.getBoolean("all.prefix.use")) {
			sender = new PrefixedCmdSender(sender, messages.getString("all.prefix.value"));
		}
		if (!sender.hasPermission("libertybans.commands")) {
			sender.sendMessage("No permission!");
			return;
		}
		if (config.getBoolean("json.enable")) {
			// Prevent JSON injection
			String args = command.clone().allRemaining();
			if (args.indexOf('|') != -1) {
				sender.sendMessage(config.getString("json.illegal-char"));
				return;
			}
		}
		if (!command.hasNext()) {
			sender.sendMessage(messages.getString("all.usage"));
			return;
		}
		String firstArg = command.next();
		switch (firstArg) {
		case "restart":
			boolean restarted = core.getEnvironment().restart();
			sender.sendMessage((restarted) ? config.getString("all.restarted") : "Not restarting because loading already in process");
			return;
		case "reload":
			try {
				core.getConfigs().reload();
				sender.sendMessage(config.getString("all.reloaded"));
			} catch (ConfigLoadValuesFromFileException ex) {
				sender.sendMessage("Failed to reload config files: Please check your server console for the full error.");
				logger.warn("Failed to reload config files", ex);
			}
			return;
		default:
			break;
		}
		for (PunishmentType type : MiscUtil.punishmentTypes()) {
			if (type.name().equalsIgnoreCase(firstArg)) {
				punishCommand(sender, type, command);
			}
		}
		for (PunishmentType type : MiscUtil.punishmentTypes()) {
			if (type == PunishmentType.KICK) {
				// Cannot undo kicks
				continue;
			}
			if (("un" + type.getLowercaseName()).equalsIgnoreCase(firstArg)) {
				unbanCmd(sender, type, command);
			}
		}
	}

	private void punishCommand(CmdSender sender, PunishmentType type, CommandPackage command) {
		if (!sender.hasPermission("libertybans." + type.getLowercaseName() + ".do")) { // libertybans.ban.do
			sender.sendMessage(
					messages.getString("additions." + type.getLowercaseNamePlural() + ".permission.command")); // additions.bans.permission.command
			return;
		}
		if (!command.hasNext()) {
			sender.sendMessage(messages.getString("additions." + type.getLowercaseNamePlural() + ".usage")); // additions.bans.usage
			return;
		}
		String targetArg = command.next();
		core.getUUIDMaster().fullLookupUUID(targetArg).thenCompose((uuid) -> {
			if (uuid == null) {
				sender.sendMessage(messages.getString("all.not-found.uuid").replace("%TARGET%", targetArg));
				return core.getFuturesFactory().completedFuture(null);
			}
			String reason;
			if (command.hasNext()) {
				reason = command.allRemaining();
			} else if (config.getBoolean("reasons.permit-blank")) {
				reason = "";
			} else {
				reason = config.getString("reasons.default-reason");
			}
			DraftPunishment draftBan = new DraftPunishment.Builder().victim(PlayerVictim.of(uuid))
					.operator(sender.getOperator()).type(type).reason(reason)
					.scope(core.getScopeManager().globalScope()).build();

			return core.getEnactor().enactPunishment(draftBan).thenApply((nullIfConflict) -> {
				assert type == PunishmentType.BAN || type == PunishmentType.MUTE : type;
				if (nullIfConflict == null) {
					String configPath = "additions." + type.getLowercaseNamePlural() + ".error.conflicting"; // additions.bans.error.conflicting
					sender.sendMessage(messages.getString(configPath).replace("%TARGET%", targetArg));
				}
				return nullIfConflict;
			});
		}).thenAccept((punishment) -> {
			if (punishment == null) {
				return;
			}
			// Success message
			String rawMsg = messages.getString("additions." + type.getLowercaseNamePlural() + ".successful.message"); // additions.bans.successful.message
			CentralisedFuture<String> futureMsg = core.getFormatter().formatWithPunishment(rawMsg, punishment);
			assert futureMsg.isDone();
			sender.sendMessage(futureMsg.join());

			// Enforcement
			core.getEnforcer().enforce(punishment);

			// Notification
			String notifyPerm = "libertybans." + type.getLowercaseName() + ".notify"; // libertybans.ban.notify
			String configMsgPath = "addition." + type.getLowercaseNamePlural() + ".successful.notification"; // addition.bans.successful.notification
			String rawNotify = messages.getString(configMsgPath);
			CentralisedFuture<String> futureNotify = core.getFormatter().formatWithPunishment(rawNotify, punishment);
			assert futureNotify.isDone();
			core.getEnvironment().sendToThoseWithPermission(notifyPerm, futureNotify.join());
		});
	}
	
	private void unbanCmd(CmdSender sender, PunishmentType type, CommandPackage command) {
		if (!sender.hasPermission("libertybans." + type.getLowercaseName() + ".undo")) { // libertybans.ban.undo
			sender.sendMessage(messages.getString("removals." + type.getLowercaseNamePlural() + ".permission.command")); // removals.bans.permission.command
			return;
		}
		if (!command.hasNext()) {
			sender.sendMessage(messages.getString("removals." + type.getLowercaseNamePlural() + ".usage")); // removals.bans.usage
			return;
		}
		String name = command.next();
		core.getUUIDMaster().fullLookupUUID(name).thenCompose((uuid) -> {
			if (uuid == null) {
				sender.sendMessage(messages.getString("all.not-found.uuid").replace("%TARGET%", name));
				return core.getFuturesFactory().completedFuture(null);
			}
			PunishmentSelection selection = new PunishmentSelection.Builder().type(type)
					.victim(PlayerVictim.of(uuid)).build();
			return core.getSelector().getFirstSpecificPunishment(selection).thenApply((nullIfNotFound) -> {
				if (nullIfNotFound == null) {
					String configPath = "removals." + type.getLowercaseNamePlural() + ".not-found"; // removals.bans.not-found
					sender.sendMessage(messages.getString(configPath).replace("%TARGET%", name));
				}
				return nullIfNotFound;
			});
		}).thenAccept((punishment) -> {
			if (punishment == null) {
				return;
			}
			// Success message
			String rawMsg = messages.getString("removals." + type.getLowercaseNamePlural() + ".successful.message"); // removals.bans.successful.message
			CentralisedFuture<String> futureMsg = core.getFormatter().formatWithPunishment(rawMsg, punishment);
			assert futureMsg.isDone();
			sender.sendMessage(futureMsg.join());

			// Notification
			String rawNotify = messages.getString("removals." + type.getLowercaseNamePlural() + ".successful.notification"); // removals.bans.successful.notification
			CentralisedFuture<String> futureNotify = core.getFormatter().formatWithPunishment(rawNotify, punishment);
			assert futureNotify.isDone();
			core.getEnvironment().sendToThoseWithPermission("libertybans." + type.getLowercaseName() + ".unnotify", futureNotify.join()); // libertybans.ban.unnotify
		});
	}
	
}

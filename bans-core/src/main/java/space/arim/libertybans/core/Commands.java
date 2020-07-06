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

import java.util.concurrent.CompletableFuture;

import space.arim.api.util.config.Config;

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
	
	Commands(LibertyBansCore core) {
		this.core = core;
		config = core.getConfigs().getConfig();
		messages = core.getConfigs().getMessages();
	}
	
	public void execute(CmdSender sender, CommandPackage command) {
		if (messages.getBoolean("all.prefix.use")) {
			sender = new PrefixedCmdSender(sender, messages.getString("all.prefix.value"));
		}
		if (!sender.hasPermission("libertybans.command.use")) {
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
		switch (command.next()) {
		case "restart":
			core.getEnvironment().restart();
			sender.sendMessage(config.getString("all.restarted"));
			break;
		case "reload":
			core.getConfigs().reload();
			sender.sendMessage(config.getString("all.reloaded"));
			break;
		case "ban":
			banCmd(sender, command);
			break;
		case "unban":
			unbanCmd(sender, command);
			break;
		default:
			break;
		}
	}
	
	private void banCmd(CmdSender sender, CommandPackage command) {
		if (!sender.hasPermission("libertybans.ban.do")) {
			sender.sendMessage(messages.getString("additions.bans.permission.command"));
			return;
		}
		if (!command.hasNext()) {
			sender.sendMessage(messages.getString("additions.bans.usage"));
			return;
		}
		String name = command.next();
		core.getUUIDMaster().fullLookupUUID(name).thenCompose((uuid) -> {
			if (uuid == null) {
				sender.sendMessage(messages.getString("all.not-found.uuid").replace("%TARGET%", name));
				return CompletableFuture.completedFuture(null);
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
					.operator(sender.getOperator()).type(PunishmentType.BAN).start(System.currentTimeMillis())
					.permanent().reason(reason).scope(core.getScopeManager().globalScope()).build();

			return core.getEnactor().enactPunishment(draftBan, true).thenApply((nullIfConflict) -> {
				if (nullIfConflict == null) {
					sender.sendMessage(messages.getString("additions.bans.error.conflicting"));
				}
				return nullIfConflict;
			});

		}).thenAccept((punishment) -> {
			if (punishment == null) {
				return;
			}
			sender.sendMessage(core.getFormatter()
					.formatWithPunishment(messages.getString("additions.bans.successful.message"), punishment));
			core.getEnvironment().enforcePunishment(punishment);
			core.getEnvironment().sendToThoseWithPermission("libertybans.ban.notify", core.getFormatter()
					.formatWithPunishment(messages.getString("addition.bans.successful.notification"), punishment));
		});
	}
	
	private void unbanCmd(CmdSender sender, CommandPackage command) {
		if (!sender.hasPermission("libertybans.ban.undo")) {
			sender.sendMessage(messages.getString("removals.bans.permission.command"));
			return;
		}
		if (!command.hasNext()) {
			sender.sendMessage(messages.getString("removals.bans.usage"));
			return;
		}
		String name = command.next();
		core.getUUIDMaster().fullLookupUUID(name).thenCompose((uuid) -> {
			if (uuid == null) {
				sender.sendMessage(messages.getString("all.not-found.uuid").replace("%TARGET%", name));
				return CompletableFuture.completedFuture(null);
			}
			PunishmentSelection selection = new PunishmentSelection.Builder().type(PunishmentType.BAN)
					.victim(PlayerVictim.of(uuid)).build();
			return core.getSelector().getFirstSpecificPunishment(selection).thenApply((nullIfNotFound) -> {
				if (nullIfNotFound == null) {
					sender.sendMessage(messages.getString("removals.bans.not-found").replace("%TARGET%", name));
				}
				return nullIfNotFound;
			});
		}).thenAccept((punishment) -> {
			if (punishment == null) {
				return;
			}
			sender.sendMessage(core.getFormatter()
					.formatWithPunishment(messages.getString("removals.bans.successful.message"), punishment));
			core.getEnvironment().sendToThoseWithPermission("libertybans.ban.unnotify", core.getFormatter()
					.formatWithPunishment(messages.getString("removals.bans.successful.notification"), punishment));
		});
	}
	
}

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

import java.util.List;
import java.util.Locale;

import space.arim.omnibus.util.ArraysUtil;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.bootstrap.plugin.PluginInfo;
import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.env.CmdSender;

public class Commands {

	final LibertyBansCore core;
	
	private final List<SubCommandGroup> subCommands;
	
	public static final String BASE_COMMAND_NAME = "libertybans";
	
	public Commands(LibertyBansCore core) {
		this.core = core;
		subCommands = List.of(new PunishCommands(this), new UnpunishCommands(this), new AdminCommands(this));
	}
	
	/*
	 * Main command handler
	 */
	
	public void execute(CmdSender sender, CommandPackage command) {
		if (!sender.hasPermission("libertybans.commands")) {
			sender.sendMessage(core.getMessagesConfig().all().basePermissionMessage());
			return;
		}
		if (!command.hasNext()) {
			sender.sendLiteralMessage("&7&lLibertyBans version " +  PluginInfo.VERSION);
			return;
		}
		String firstArg = command.next().toLowerCase(Locale.ROOT);
		for (SubCommandGroup subCommand : subCommands) {
			if (subCommand.matches(firstArg)) {
				CommandExecution execution = subCommand.execute(sender, command, firstArg);
				execution.execute();
				return;
			}
		}
		sendUsage(sender, command, firstArg.equals("usage"));
	}
	
	/*
	 * Usage
	 */
	
	private void sendUsage(CmdSender sender, CommandPackage command, boolean explicit) {
		if (!explicit) {
			sender.sendMessage(core.getMessagesConfig().all().usage());
		}
		UsageSection[] sections = UsageSection.values();
		int page = 1;
		if (command.hasNext()) {
			try {
				page = Integer.parseInt(command.next());
			} catch (NumberFormatException ignored) {}
			if (page <= 0 || page > sections.length) {
				page = 1;
			}
		}
		UsageSection section = sections[page - 1];
		sender.sendMessage(section.getContent());
		sender.sendLiteralMessage("&ePage " + page + "/4. &7Use /libertybans usage <page> to navigate");
	}
	
	/*
	 * Tab completion
	 */
	
	public List<String> suggest(CmdSender sender, String[] args) {
		if (args.length >= 1 && sender.hasPermission("libertybans.commands")) {
			String firstArg = args[0];
			for (SubCommandGroup subCommand : subCommands) {
				if (subCommand.matches(firstArg)) {
					return subCommand.suggest(sender, ArraysUtil.contractAndRemove(args, 0));
				}
			}
		}
		return List.of();
	}
	
	/*
	 * Helpers
	 */
	
	CentralisedFuture<Victim> parseVictim(CmdSender sender, String targetArg) {
		if (!core.getUUIDMaster().validateNameArgument(targetArg)) {
			return core.getFuturesFactory().completedFuture(null);
		}
		return core.getUUIDMaster().fullLookupUUID(targetArg).thenApply((uuid) -> {
			if (uuid == null) {
				sender.sendMessage(core.getMessagesConfig().all().notFound().uuid().replaceText("%TARGET%", targetArg));
				return null;
			}
			return PlayerVictim.of(uuid);
		});
	}
	
}

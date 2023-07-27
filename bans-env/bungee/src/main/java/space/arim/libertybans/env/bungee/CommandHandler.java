/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

package space.arim.libertybans.env.bungee;

import jakarta.inject.Inject;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.Interlocutor;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.omnibus.util.ArraysUtil;

public final class CommandHandler extends Command implements TabExecutor, PlatformListener {

	private final CommandHelper commandHelper;
	private final boolean alias;
	
	CommandHandler(CommandHelper commandHelper, String command, boolean alias) {
		super(command);
		this.commandHelper = commandHelper;
		this.alias = alias;
	}
	
	public static class CommandHelper {

		private final InternalFormatter formatter;
		private final Interlocutor interlocutor;
		private final AudienceRepresenter<CommandSender> audienceRepresenter;
		private final Commands commands;
		private final Plugin plugin;

		@Inject
		public CommandHelper(InternalFormatter formatter, Interlocutor interlocutor,
							 AudienceRepresenter<CommandSender> audienceRepresenter,
							 Commands commands, Plugin plugin) {
			this.formatter = formatter;
			this.interlocutor = interlocutor;
			this.audienceRepresenter = audienceRepresenter;
			this.commands = commands;
			this.plugin = plugin;
		}

		private CmdSender adaptSender(CommandSender platformSender) {
			if (platformSender instanceof ProxiedPlayer player) {
				return new BungeeCmdSender.PlayerSender(
						formatter, interlocutor, audienceRepresenter, player, plugin);
			}
			return new BungeeCmdSender.ConsoleSender(
					formatter, interlocutor, audienceRepresenter, platformSender, plugin);
		}

	}

	@Override
	public void register() {
		Plugin plugin = commandHelper.plugin;
		plugin.getProxy().getPluginManager().registerCommand(plugin, this);
	}

	@Override
	public void unregister() {
		commandHelper.plugin.getProxy().getPluginManager().unregisterCommand(this);
	}

	private String[] adaptArgs(String[] args) {
		if (alias) {
			return ArraysUtil.expandAndInsert(args, getName(), 0);
		}
		return args;
	}

	@Override
	public void execute(CommandSender platformSender, String[] args) {
		commandHelper.commands.execute(
				commandHelper.adaptSender(platformSender),
				ArrayCommandPackage.create(adaptArgs(args))
		);
	}

	@Override
	public Iterable<String> onTabComplete(CommandSender platformSender, String[] args) {
		return commandHelper.commands.suggest(
				commandHelper.adaptSender(platformSender),
				adaptArgs(args)
		);
	}

	@Override
	public boolean hasPermission(CommandSender platformSender) {
		return commandHelper.commands.hasPermissionFor(
				commandHelper.adaptSender(platformSender),
				getName()
		);
	}

}

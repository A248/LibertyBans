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

import java.util.concurrent.ForkJoinPool;

import space.arim.api.chat.SendableMessage;

import space.arim.libertybans.core.env.CmdSender;

public class ReloadCommands extends SubCommandGroup {

	ReloadCommands(Commands commands) {
		super(commands, "reload", "restart");
	}
	
	private SendableMessage ellipses() {
		return commands.core.getFormatter().parseMessage(commands.messages().getString("admin.ellipses"));
	}

	@Override
	void execute(CmdSender sender, CommandPackage command, String arg) {
		switch (arg) {
		case "restart":
			ForkJoinPool.commonPool().execute(() -> {
				sender.sendMessage(ellipses());
				boolean restarted = commands.core.getEnvironment().fullRestart();
				sender.parseThenSend((restarted) ? commands.messages().getString("admin.restarted")
						: "Not restarting because loading already in process");
			});
			break;
		case "reload":
			sender.sendMessage(ellipses());
			commands.core.getConfigs().reloadConfigs().thenAccept((result) -> {
				if (result) {
					sender.parseThenSend(commands.messages().getString("admin.reloaded"));
				}
			});
			break;
		default:
			throw new IllegalStateException("Command mismatch");
		}
	}

}

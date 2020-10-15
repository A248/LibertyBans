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

import java.util.concurrent.CompletableFuture;

import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.env.CmdSender;

public class AdminCommands extends AbstractSubCommandGroup {

	AdminCommands(Commands commands) {
		super(commands, "reload", "restart");
	}
	
	private MessagesConfig.Admin cfg() {
		return messages().admin();
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		return new Execution(sender, command, arg);
	}
	
	private class Execution extends AbstractCommandExecution {

		private final String arg;
		
		Execution(CmdSender sender, CommandPackage command, String arg) {
			super(sender, command);
			this.arg = arg;
		}
		
		@Override
		public void execute() {
			if (!sender().hasPermission("libertybans.admin." + arg)) {
				sender().sendMessage(cfg().noPermission());
				return;
			}
			switch (arg) {
			case "restart":
				restartCmd();
				break;
			case "reload":
				reloadCmd();
				break;
			default:
				throw new IllegalArgumentException("Command mismatch");
			}
		}
		
		private void restartCmd() {
			sender().sendMessage(cfg().ellipses());
			boolean restarted = core().getEnvironment().fullRestart();
			if (restarted) {
				sender().sendMessage(cfg().restarted());
			} else {
				sender().sendLiteralMessage("Not restarting because loading already in process");
			}
		}
		
		private void reloadCmd() {
			sender().sendMessage(cfg().ellipses());
			CompletableFuture<?> reloadFuture = core().getConfigs().reloadConfigs().thenAccept((result) -> {
				if (result) {
					sender().sendMessage(cfg().reloaded());
				} else {
					sender().sendLiteralMessage(
							"&cAn error occurred reloading the configuration. Please check the server console.");
				}
			});
			core().postFuture(core().getFuturesFactory().copyFuture(reloadFuture));
		}
		
	}

}

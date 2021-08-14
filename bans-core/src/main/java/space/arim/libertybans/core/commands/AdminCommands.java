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

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import space.arim.api.env.PlatformHandle;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.plugin.PluginInfo;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.Environment;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Singleton
public class AdminCommands extends AbstractSubCommandGroup {

	private final Provider<BaseFoundation> foundation;
	private final Provider<Environment> environment;
	private final PlatformHandle envHandle;

	@Inject
	public AdminCommands(Dependencies dependencies, Provider<BaseFoundation> foundation,
			Provider<Environment> environment, PlatformHandle envHandle) {
		super(dependencies, Arrays.stream(Type.values()).map(Type::toString));
		this.foundation = foundation;
		this.environment = environment;
		this.envHandle = envHandle;
	}

	private MessagesConfig.Admin adminConfig() {
		return messages().admin();
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		return new Execution(sender, command, Type.valueOf(arg.toUpperCase(Locale.ROOT)));
	}
	
	@Override
	public Stream<String> suggest(CmdSender sender, String arg, int argIndex) {
		return Stream.empty();
	}
	
	private enum Type {
		RELOAD,
		RESTART,
		DEBUG;
		
		@Override
		public String toString() {
			return name().toLowerCase(Locale.ROOT);
		}
	}
	
	private class Execution extends AbstractCommandExecution {

		private final Type type;
		
		Execution(CmdSender sender, CommandPackage command, Type type) {
			super(sender, command);
			this.type = type;
		}
		
		@Override
		public void execute() {
			if (!sender().hasPermission("libertybans.admin." + type)) {
				sender().sendMessage(adminConfig().noPermission());
				return;
			}
			switch (type) {
			case RESTART:
				restartCmd();
				break;
			case RELOAD:
				reloadCmd();
				break;
			case DEBUG:
				debugCmd();
				break;
			default:
				throw new IllegalArgumentException("Command mismatch");
			}
		}
		
		private void restartCmd() {
			sender().sendMessage(adminConfig().ellipses());
			boolean restarted = foundation.get().fullRestart();
			if (restarted) {
				sender().sendMessage(adminConfig().restarted());
			} else {
				sender().sendLiteralMessage("Not restarting because loading already in process");
			}
		}
		
		private void reloadCmd() {
			sender().sendMessage(adminConfig().ellipses());
			CompletableFuture<?> reloadFuture = configs().reloadConfigs().thenAccept((result) -> {
				if (result) {
					sender().sendMessage(adminConfig().reloaded());
				} else {
					sender().sendLiteralMessage(
							"&cAn error occurred reloading the configuration. Please check the server console.");
				}
			});
			postFuture(futuresFactory().copyFuture(reloadFuture));
		}

		private void debugCmd() {
			String environmentImplName = environment.get().getClass().getSimpleName();
			List<String> debugInfo = List.of(
					"Version: " + PluginInfo.VERSION,
					"Platform Category: " + environmentImplName.substring(0, environmentImplName.length() - 3),
					"Platform Version: " + envHandle.getPlatformVersion()); // TODO add more debug information
			debugInfo.forEach(sender()::sendLiteralMessage);
		}
		
	}

}

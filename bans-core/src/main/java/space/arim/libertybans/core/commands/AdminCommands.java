/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.core.commands;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import space.arim.api.env.PlatformHandle;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.plugin.PluginInfo;
import space.arim.libertybans.core.addon.AddonCenter;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.Environment;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Singleton
public final class AdminCommands extends AbstractSubCommandGroup {

	private final Provider<BaseFoundation> foundation;
	private final Provider<Environment> environment;
	private final AddonCenter addonCenter;
	private final PlatformHandle envHandle;

	@Inject
	public AdminCommands(Dependencies dependencies, Provider<BaseFoundation> foundation,
						 Provider<Environment> environment, AddonCenter addonCenter, PlatformHandle envHandle) {
		super(dependencies, Arrays.stream(Type.values()).map(Type::toString));
		this.foundation = foundation;
		this.environment = environment;
		this.addonCenter = addonCenter;
		this.envHandle = envHandle;
	}

	private MessagesConfig.Admin adminConfig() {
		return messages().admin();
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		return new Execution(sender, command, Type.fromString(arg));
	}

	@Override
	public Stream<String> suggest(CmdSender sender, String arg, int argIndex) {
		return Stream.empty();
	}

	@Override
	public boolean hasTabCompletePermission(CmdSender sender, String arg) {
		return Type.fromString(arg).hasPermission(sender);
	}

	private enum Type {
		RELOAD,
		RESTART,
		DEBUG;

		boolean hasPermission(CmdSender sender) {
			return sender.hasPermission("libertybans.admin." + this);
		}

		static Type fromString(String type) {
			return valueOf(type.toUpperCase(Locale.ROOT));
		}

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
		public ReactionStage<Void> execute() {
			if (!type.hasPermission(sender())) {
				sender().sendMessage(adminConfig().noPermission());
				return null;
			}
			switch (type) {
			case RESTART:
				restartCmd();
				break;
			case RELOAD:
				return reloadCmd();
			case DEBUG:
				debugCmd();
				break;
			default:
				throw new IllegalArgumentException("Command mismatch");
			}
			return null;
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

		private ReactionStage<Void> reloadCmd() {
			sender().sendMessage(adminConfig().ellipses());
			return reloadAllConfiguration().thenAccept((result) -> {
				if (result) {
					sender().sendMessage(adminConfig().reloaded());
				} else {
					sender().sendMessage(adminConfig().reloadFailed());
				}
			});
		}

		private CentralisedFuture<Boolean> reloadAllConfiguration() {
			var reloadCoreConfigs = configs().reloadConfigs();
			var reloadAddonConfigs = addonCenter.reloadAddons();
			return reloadAddonConfigs.thenCombine(reloadCoreConfigs, (reload1, reload2) -> reload1 && reload2);
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

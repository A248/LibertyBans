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
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.core.addon.Addon;
import space.arim.libertybans.core.addon.AddonCenter;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.Locale;
import java.util.stream.Stream;

@Singleton
public class AddonCommands extends AbstractSubCommandGroup {

	private final AddonCenter addonCenter;

	@Inject
	public AddonCommands(Dependencies dependencies, AddonCenter addonCenter) {
		super(dependencies, "addon");
		this.addonCenter = addonCenter;
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		return new Execution(sender, command);
	}

	@Override
	public Stream<String> suggest(CmdSender sender, String arg, int argIndex) {
		return Stream.empty();
	}

	private class Execution extends AbstractCommandExecution {

		private final MessagesConfig.Admin.Addons conf;

		Execution(CmdSender sender, CommandPackage command) {
			super(sender, command);
			conf = messages().admin().addons();
		}

		@Override
		public @Nullable ReactionStage<Void> execute() {
			if (!command().hasNext()) {
				sender().sendMessage(conf.usage());
				return null;
			}
			String firstArg = command().next();
			switch (firstArg.toLowerCase(Locale.ROOT)) {
			case "list":
				var listing = conf.listing();
				var layout = listing.layout();
				sender().sendMessage(Component.text()
						.append(listing.message())
						.append(Component.join(
								Component.newline(),
								addonCenter.allIdentifiers()
										.map((identifier) -> layout.replaceText("%ADDON%", identifier))
										.toArray(ComponentText[]::new)
						)));
				break;
			case "reload":
				return reloadCmd();
			default:
				sender().sendMessage(conf.usage());
				break;
			}
			return null;
		}

		private ReactionStage<Void> reloadCmd() {
			var reloadConfig = conf.reloadAddon();
			if (!command().hasNext()) {
				sender().sendMessage(reloadConfig.usage());
				return completedFuture(null);
			}
			String identifier = command().next().toLowerCase(Locale.ROOT);
			Addon<?> addon = addonCenter.addonByIdentifier(identifier);
			if (addon == null) {
				sender().sendMessage(reloadConfig.doesNotExist());
				return completedFuture(null);
			}
			return addonCenter.reloadConfiguration(addon).thenAccept((result) -> {
				if (result) {
					sender().sendMessage(reloadConfig.success().replaceText("%ADDON%", identifier));
				} else {
					sender().sendMessage(reloadConfig.failed());
				}
			});
		}
	}
}

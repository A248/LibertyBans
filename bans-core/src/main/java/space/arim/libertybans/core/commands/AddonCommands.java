/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;
import space.arim.libertybans.core.addon.Addon;
import space.arim.libertybans.core.addon.AddonCenter;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.stream.Stream;

@Singleton
public final class AddonCommands extends AbstractSubCommandGroup {

	private final Path folder;
	private final AddonCenter addonCenter;

	@Inject
	public AddonCommands(Dependencies dependencies, @Named("folder") Path folder, AddonCenter addonCenter) {
		super(dependencies, "addon");
		this.folder = folder;
		this.addonCenter = addonCenter;
	}

	private Path addonsFolder() {
		return folder.resolve("addons");
	}

	private MessagesConfig.Admin.Addons addonConfig() {
		return messages().admin().addons();
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		return new Execution(sender, command);
	}

	@Override
	public Stream<String> suggest(CmdSender sender, String arg, int argIndex) {
		//
		// TODO Delete the "reload" command after merging addon configs with DazzleConf 2.0
		//
		if (argIndex == 0) {
			return Stream.of("install", "list", "reload");
		}
		if (argIndex == 1 && arg.equals("reload")) {
			return addonCenter.allIdentifiers();
		}
		return Stream.empty();
    }

	@Override
	public boolean hasTabCompletePermission(CmdSender sender, String arg) {
		return hasPermission(sender);
	}

	private boolean hasPermission(CmdSender sender) {
		return sender.hasPermission("libertybans.admin.addon");
	}

	private class Execution extends AbstractCommandExecution {

		Execution(CmdSender sender, CommandPackage command) {
			super(sender, command);
		}

		@Override
		public @Nullable ReactionStage<Void> execute() {
			if (!hasPermission(sender())) {
				sender().sendMessage(messages().admin().noPermission());
				return null;
			}
			if (!command().hasNext()) {
				sender().sendMessage(addonConfig().usage());
				return null;
			}
			String firstArg = command().next();
			switch (firstArg.toLowerCase(Locale.ROOT)) {
			case "list":
				var listing = addonConfig().listing();
				var layout = listing.layout();
				sender().sendMessage(Component.text()
						.append(listing.message())
						.append(Component.newline())
						.append(Component.join(
								Component.newline(),
								addonCenter.allIdentifiers()
										.map((identifier) -> layout.replaceText("%ADDON%", identifier))
										.toArray(ComponentText[]::new)
						)));
				break;
			case "install":
				return installCmd();
			case "reload":
				return reloadCmd();
			default:
				sender().sendMessage(addonConfig().usage());
				break;
			}
			return null;
		}

		private ReactionStage<Void> installCmd() {
			var installConfig = addonConfig().install();
			if (!command().hasNext()) {
				sender().sendMessage(installConfig.usage());
				return completedFuture(null);
			}
			String identifier = command().next().toLowerCase(Locale.ROOT);
			String jarFileName = "addon-" + identifier + ".jar";
			Path addonJar = addonsFolder().resolve(jarFileName);
			if (addonCenter.addonByIdentifier(identifier) != null || Files.exists(addonJar)) {
				sender().sendMessage(installConfig.alreadyInstalled().replaceText("%ADDON%", identifier));
				return completedFuture(null);
			}
			URL jarResource = LibertyBansLauncher.class.getResource("/dependencies/addon-jars/" + jarFileName);
			if (jarResource == null) {
				sender().sendMessage(installConfig.doesNotExist().replaceText("%ADDON%", identifier));
				return completedFuture(null);
			}
			return futuresFactory().supplyAsync(() -> {
				try (InputStream resourceInput = jarResource.openStream()) {
					Files.copy(resourceInput, addonJar, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException ex) {
					throw new UncheckedIOException(ex);
				}
				sender().sendMessage(installConfig.installed().replaceText("%ADDON%", identifier));
				return null;
			});
        }

		private ReactionStage<Void> reloadCmd() {
			sender().sendLiteralMessage(
					"The '/libertybans addon reload' command is deprecated and will be removed in a " +
							"future release. Please use '/libertybans reload' instead."
			);
			var reloadConfig = addonConfig().reloadAddon();
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

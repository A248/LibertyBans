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
import space.arim.libertybans.core.alts.AltCheckFormatter;
import space.arim.libertybans.core.alts.AltDetection;
import space.arim.libertybans.core.alts.AltsSection;
import space.arim.libertybans.core.alts.WhichAlts;
import space.arim.libertybans.core.commands.extra.TabCompletion;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.stream.Stream;

@Singleton
public final class AltCommands extends AbstractSubCommandGroup {

	private final AltDetection altDetection;
	private final AltCheckFormatter altCheckFormatter;
	private final TabCompletion tabCompletion;

	@Inject
	public AltCommands(Dependencies dependencies, AltDetection altDetection, AltCheckFormatter altCheckFormatter, TabCompletion tabCompletion) {
		super(dependencies, "alts");
		this.altDetection = altDetection;
		this.altCheckFormatter = altCheckFormatter;
		this.tabCompletion = tabCompletion;
	}

	private AltsSection altsConfig() {
		return messages().alts();
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		return new Execution(sender, command);
	}

	@Override
	public Stream<String> suggest(CmdSender sender, String arg, int argIndex) {
		if (argIndex == 0) {
			return tabCompletion.completeOfflinePlayerNames(sender);
		}
		return Stream.empty();
	}

	@Override
	public boolean hasTabCompletePermission(CmdSender sender, String arg) {
		return hasPermission(sender);
	}

	private boolean hasPermission(CmdSender sender) {
		return sender.hasPermission("libertybans.alts.command");
	}

	private class Execution extends AbstractCommandExecution {

		Execution(CmdSender sender, CommandPackage command) {
			super(sender, command);
		}

		@Override
		public ReactionStage<Void> execute() {
			if (!hasPermission(sender())) {
				sender().sendMessage(altsConfig().command().permission());
				return null;
			}
			if (!command().hasNext()) {
				sender().sendMessage(altsConfig().command().usage());
				return null;
			}
			String target = command().next();
			return argumentParser().parsePlayer(sender(), target).thenCompose((userDetails) -> {
				if (userDetails == null) {
					return completedFuture(null);
				}
				return altDetection.detectAlts(userDetails, WhichAlts.ALL_ALTS).thenAccept((detectedAlts) -> {
					if (detectedAlts.isEmpty()) {
						sender().sendMessage(altsConfig().command().noneFound());
						return;
					}
					sender().sendMessageNoPrefix(
							altCheckFormatter.formatMessage(altsConfig().command().header(), target, detectedAlts));
				});
			});
		}
	}
}

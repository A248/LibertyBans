/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.uuid.UUIDManager;

import java.util.Collection;
import java.util.Set;

@Singleton
public class AltCommands extends AbstractSubCommandGroup {

	private final UUIDManager uuidManager;
	private final AltDetection altDetection;
	private final AltCheckFormatter altCheckFormatter;

	@Inject
	public AltCommands(Dependencies dependencies, UUIDManager uuidManager, AltDetection altDetection, AltCheckFormatter altCheckFormatter) {
		super(dependencies, "alts");
		this.uuidManager = uuidManager;
		this.altDetection = altDetection;
		this.altCheckFormatter = altCheckFormatter;
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		return new Execution(sender, command);
	}

	@Override
	public Collection<String> suggest(CmdSender sender, String arg, int argIndex) {
		if (argIndex == 0) {
			return sender.getOtherPlayersOnSameServer();
		}
		return Set.of();
	}

	private AltsSection section() {
		return messages().alts();
	}

	private class Execution extends AbstractCommandExecution {

		Execution(CmdSender sender, CommandPackage command) {
			super(sender, command);
		}

		@Override
		public void execute() {
			if (!sender().hasPermission("libertybans.alts.command")) {
				sender().sendMessage(section().command().permission());
				return;
			}
			if (!command().hasNext()) {
				sender().sendMessage(section().command().usage());
				return;
			}
			String target = command().next();
			var future = uuidManager.lookupPlayer(target).thenCompose((userDetails) -> {
				if (userDetails.isEmpty()) {
					sender().sendMessage(messages().all().notFound().player().replaceText("%TARGET%", target));
					return completedFuture(null);
				}
				return altDetection.detectAlts(userDetails.get()).thenAccept((detectedAlts) -> {
					if (detectedAlts.isEmpty()) {
						sender().sendMessage(section().command().noneFound());
						return;
					}
					sender().sendMessageNoPrefix(
							altCheckFormatter.formatMessage(section().command().header(), target, detectedAlts));
				});
			});
			postFuture(future);
		}
	}
}

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
import jakarta.inject.Singleton;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.alts.*;
import space.arim.libertybans.core.commands.extra.TabCompletion;
import space.arim.libertybans.core.database.pagination.InstantThenUUID;
import space.arim.libertybans.core.database.pagination.KeysetAnchor;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.UUID;
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
				int page;
				AltInfoRequest request;
				{
					KeysetAnchor<InstantThenUUID> anchor = KeysetAnchor.instantThenUUID(command());
					if (anchor == null) {
						sender().sendMessage(altsConfig().command().usage());
						return completedFuture(null);
					}
					UUID uuid = userDetails.uuid();
					NetworkAddress address = userDetails.address();
					boolean oldestFirst = altsConfig().command().oldestFirst();

					page = anchor.page();
					int pageSize = altsConfig().command().perPage();
					int skipCount = 0;
					if (anchor.borderValue() == null) {
						// Traditional pagination
						skipCount = (page - 1) * pageSize;
					}
					request = new AltInfoRequest(uuid, address, WhichAlts.ALL_ALTS, oldestFirst, pageSize, anchor, skipCount);
				}
				return altDetection.detectAlts(request).thenAccept((response) -> {
					if (response.data().isEmpty()) {
						sender().sendMessage(altsConfig().command().noneFound().replaceText(
								"%PAGE%", Integer.toString(page)));
						return;
					}
					sender().sendMessage(altCheckFormatter.formatMessage(
							altsConfig().command(), response, target, page
					));
				});
			});
		}
	}
}

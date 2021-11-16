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
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.core.alts.AccountHistory;
import space.arim.libertybans.core.alts.AccountHistoryFormatter;
import space.arim.libertybans.core.alts.AccountHistorySection;
import space.arim.libertybans.core.commands.extra.TabCompletion;
import space.arim.libertybans.core.env.CmdSender;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

@Singleton
public class AccountHistoryCommands extends AbstractSubCommandGroup {

	private final AccountHistory accountHistory;
	private final AccountHistoryFormatter accountHistoryFormatter;
	private final TabCompletion tabCompletion;

	@Inject
	public AccountHistoryCommands(Dependencies dependencies, AccountHistory accountHistory, 
								  AccountHistoryFormatter accountHistoryFormatter, TabCompletion tabCompletion) {
		super(dependencies, "accounthistory");
		this.accountHistory = accountHistory;
		this.accountHistoryFormatter = accountHistoryFormatter;
		this.tabCompletion = tabCompletion;
	}

	@Override
	public Stream<String> suggest(CmdSender sender, String arg, int argIndex) {
		switch (argIndex) {
		case 0:
			return Stream.of("delete", "list");
		case 1:
			return tabCompletion.completeOfflinePlayerNames(sender);
		default:
			return Stream.empty();
		}
	}

	private AccountHistorySection accountHistory() {
		return messages().accountHistory();
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		class UsageExecution implements CommandExecution {

			@Override
			public void execute() {
				sender.sendMessage(accountHistory().usage());
			}
		}
		if (!command.hasNext()) {
			return new UsageExecution();
		}
		String action = command.next();
		switch (action.toLowerCase(Locale.ROOT)) {
		case "delete":
			return new DeleteExecution(sender, command);
		case "list":
			return new ListExecution(sender, command);
		default:
			return new UsageExecution();
		}
	}

	private final class DeleteExecution extends AbstractCommandExecution {

		DeleteExecution(CmdSender sender, CommandPackage command) {
			super(sender, command);
		}

		private AccountHistorySection.Delete delete() {
			return accountHistory().delete();
		}

		@Override
		public void execute() {
			if (!sender().hasPermission("libertybans.alts.accounthistory.delete")) {
				sender().sendMessage(delete().permission());
				return;
			}
			if (!command().hasNext()) {
				sender().sendMessage(delete().usage());
				return;
			}
			String target = command().next();

			if (!command().hasNext()) {
				sender().sendMessage(delete().usage());
				return;
			}
			Instant timestamp;
			try {
				timestamp = Instant.ofEpochSecond(Long.parseLong(command().next()));
			} catch (NumberFormatException badTimestamp) {
				sender().sendMessage(delete().usage());
				return;
			}
			var future = argumentParser().parseVictimByName(sender(), target).thenCompose((victim) -> {
				if (victim == null) {
					return completedFuture(null);
				}
				if (!(victim instanceof PlayerVictim)) {
					sender().sendMessage(delete().notByAddress());
					return completedFuture(null);
				}
				UUID uuid = ((PlayerVictim) victim).getUUID();
				return accountHistory.deleteAccount(uuid, timestamp).thenAccept((success) -> {
					if (success) {
						sender().sendMessage(delete().success().replaceText("%TARGET%", target));
					} else {
						sender().sendMessage(delete().noSuchAccount().replaceText("%TARGET%", target));
					}
				});
			});
			postFuture(future);
		}

	}

	private final class ListExecution extends AbstractCommandExecution {

		ListExecution(CmdSender sender, CommandPackage command) {
			super(sender, command);
		}

		private AccountHistorySection.Listing listing() {
			return accountHistory().listing();
		}

		@Override
		public void execute() {
			if (!sender().hasPermission("libertybans.alts.accounthistory.list")) {
				sender().sendMessage(listing().permission());
				return;
			}
			if (!command().hasNext()) {
				sender().sendMessage(listing().usage());
				return;
			}
			String target = command().next();
			var future = argumentParser().parseVictimByName(sender(), target).thenCompose((victim) -> {
				if (victim == null) {
					return completedFuture(null);
				}
				return accountHistory.knownAccounts(victim).thenAccept((knownAccounts) -> {
					if (knownAccounts.isEmpty()) {
						sender().sendMessage(listing().noneFound());
						return;
					}
					sender().sendMessageNoPrefix(
							accountHistoryFormatter.formatMessage(target, knownAccounts));
				});
			});
			postFuture(future);
		}


	}
}

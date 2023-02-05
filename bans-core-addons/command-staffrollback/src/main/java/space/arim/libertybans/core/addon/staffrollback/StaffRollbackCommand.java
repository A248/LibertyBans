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

package space.arim.libertybans.core.addon.staffrollback;

import jakarta.inject.Inject;
import org.jetbrains.annotations.Nullable;
import space.arim.libertybans.core.addon.staffrollback.execute.PreparedRollback;
import space.arim.libertybans.core.addon.staffrollback.execute.RollbackExecutor;
import space.arim.libertybans.core.commands.AbstractCommandExecution;
import space.arim.libertybans.core.commands.AbstractSubCommandGroup;
import space.arim.libertybans.core.commands.CommandExecution;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.commands.extra.DurationParser;
import space.arim.libertybans.core.commands.extra.TabCompletion;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.UUIDUtil;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public final class StaffRollbackCommand extends AbstractSubCommandGroup {

	private final StaffRollbackAddon addon;
	private final RollbackExecutor rollbackExecutor;
	private final InternalFormatter formatter;
	private final TabCompletion tabCompletion;
	private final Time time;

	@Inject
	public StaffRollbackCommand(Dependencies dependencies, StaffRollbackAddon addon,
								RollbackExecutor rollbackExecutor, InternalFormatter formatter,
								TabCompletion tabCompletion, Time time) {
		super(dependencies, "staffrollback");
		this.addon = addon;
		this.rollbackExecutor = rollbackExecutor;
		this.formatter = formatter;
		this.tabCompletion = tabCompletion;
		this.time = time;
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
		return sender.hasPermission("libertybans.addon.staffrollback.use");
	}

	private final class Execution extends AbstractCommandExecution {

		private final StaffRollbackConfig config;

		private Execution(CmdSender sender, CommandPackage command) {
			super(sender, command);
			config = addon.config();
		}

		@Override
		public @Nullable ReactionStage<Void> execute() {
			if (!hasPermission(sender())) {
				sender().sendMessage(config.noPermission());
				return null;
			}
			if (!command().hasNext()) {
				sender().sendMessage(config.usage());
				return null;
			}
			boolean enableConfirmation = config.confirmation().enable();
			String target = command().next();
			if (enableConfirmation && target.startsWith("confirm:")) {
				return executeConfirmation(target.substring("confirm:".length()));
			}
			return argumentParser().parseOperator(sender(), target).thenCompose((operator) -> {
				if (operator == null) {
					return completedFuture(null);
				}
				Instant currentTime = time.currentTimestamp();
				Instant afterWhich;
				if (command().hasNext()) {
					String timeAgo = command().next();
					Duration duration = new DurationParser(Set.of()).parse(timeAgo);
					if (duration.isNegative()) {
						sender().sendMessage(config.invalidDuration());
						return completedFuture(null);
					}
					assert !duration.isZero();
					afterWhich = currentTime.minus(duration);
				} else {
					afterWhich = Instant.EPOCH;
				}
				PreparedRollback rollback = new PreparedRollback(operator, afterWhich, currentTime);
				return (enableConfirmation) ? prepareConfirmation(rollback) : executeRollback(rollback);
			});
		}

		private CentralisedFuture<Void> prepareConfirmation(PreparedRollback rollback) {
			var previewCountFuture = rollbackExecutor.previewCount(rollback);
			var formatOperatorFuture = formatter.formatOperator(rollback.operator());

			return previewCountFuture.thenAcceptBoth(formatOperatorFuture, (count, operator) -> {
				if (count == 0) {
					tellSenderThatRollbackIsUnnecessary(operator);
					return;
				}
				UUID confirmationCode = UUID.randomUUID();
				addon.confirmationCache().put(confirmationCode, rollback);

				sender().sendMessage(config.confirmation().message()
						.replaceText("%COUNT%", Integer.toString(count))
						.replaceText("%OPERATOR%", operator)
						.replaceText("%CONFIRMATION_CODE%", UUIDUtil.toShortString(confirmationCode)));
			});
		}

		private @Nullable ReactionStage<Void> executeConfirmation(String confirmationCodeString) {
			var confirmationConf = config.confirmation();
			if (confirmationCodeString.length() != 32) {
				sender().sendMessage(confirmationConf.invalidCode());
				return null;
			}
			UUID confirmationCode;
			try {
				confirmationCode = UUIDUtil.fromShortString(confirmationCodeString);
			} catch (NumberFormatException ex) {
				sender().sendMessage(confirmationConf.invalidCode());
				return null;
			}
			PreparedRollback rollback = addon.confirmationCache().getIfPresent(confirmationCode);
			if (rollback == null) {
				sender().sendMessage(confirmationConf.nonexistentCode());
				return null;
			}
			return executeRollback(rollback);
		}

		private CentralisedFuture<Void> executeRollback(PreparedRollback rollback) {
			var rollbackFuture = rollbackExecutor.executeRollback(rollback);
			var formatOperatorFuture = formatter.formatOperator(rollback.operator());
			return rollbackFuture.thenAcceptBoth(formatOperatorFuture, (count, operator) -> {
				if (count == 0) {
					tellSenderThatRollbackIsUnnecessary(operator);
					return;
				}
				sender().sendMessage(config.success()
						.replaceText("%COUNT%", Integer.toString(count))
						.replaceText("%OPERATOR%", operator));
			});
		}

		private void tellSenderThatRollbackIsUnnecessary(String operator) {
			sender().sendMessage(config.noPunishmentsToRollback().replaceText("%OPERATOR%", operator));
		}
	}

}

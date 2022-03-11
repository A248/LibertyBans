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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.core.commands.extra.DurationParser;
import space.arim.libertybans.core.commands.extra.DurationPermissionCheck;
import space.arim.libertybans.core.commands.extra.NotificationMessage;
import space.arim.libertybans.core.commands.extra.PunishmentPermissionCheck;
import space.arim.libertybans.core.commands.extra.ReasonsConfig;
import space.arim.libertybans.core.commands.extra.TabCompletion;
import space.arim.libertybans.core.config.AdditionsSection;
import space.arim.libertybans.core.config.AdditionsSection.ExclusivePunishmentAddition;
import space.arim.libertybans.core.config.AdditionsSection.PunishmentAdditionWithDurationPerm;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.event.PostPunishEventImpl;
import space.arim.libertybans.core.event.PunishEventImpl;
import space.arim.libertybans.core.punish.EnforcementOpts;
import space.arim.libertybans.core.punish.Mode;
import space.arim.libertybans.core.punish.PunishmentPermission;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

abstract class PunishCommands extends AbstractSubCommandGroup implements PunishUnpunishCommands {

	private final PunishmentDrafter drafter;
	private final InternalFormatter formatter;
	private final TabCompletion tabCompletion;

	PunishCommands(Dependencies dependencies, Stream<String> matches,
				   PunishmentDrafter drafter, InternalFormatter formatter,
				   TabCompletion tabCompletion) {
		super(dependencies, matches);
		this.drafter = drafter;
		this.formatter = formatter;
		this.tabCompletion = tabCompletion;
	}

	@Override
	public final CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		PunishmentType type = parseType(arg.toUpperCase(Locale.ROOT));
		PunishmentPermissionCheck permissionCheck;
		return new Execution(
				sender, command, type,
				(permissionCheck = new PunishmentPermissionCheck(sender, new PunishmentPermission(type, Mode.DO))),
				new NotificationMessage(permissionCheck));
	}

	@Override
	public final Stream<String> suggest(CmdSender sender, String arg, int argIndex) {
		PunishmentType type = parseType(arg.toUpperCase(Locale.ROOT));
		if (argIndex == 0) {
			if (type == PunishmentType.KICK) {
				// Can only kick online players
				return tabCompletion.completeOnlinePlayerNames(sender);
			}
			return tabCompletion.completeOfflinePlayerNames(sender);
		}
		if (argIndex == 1) {
			if (type == PunishmentType.KICK) {
				// Don't return durations for the kick command.
				return Stream.empty();
			}
			return tabCompletion.completePunishmentDurations(sender, type);
		}
		return Stream.empty();
	}
	
	private class Execution extends TypeSpecificExecution {

		private final PunishmentPermissionCheck permissionCheck;
		private final NotificationMessage notificationMessage;
		private final AdditionsSection.PunishmentAddition section;

		Execution(CmdSender sender, CommandPackage command, PunishmentType type,
				  PunishmentPermissionCheck permissionCheck, NotificationMessage notificationMessage) {
			super(sender, command, type);
			this.permissionCheck = permissionCheck;
			this.notificationMessage = notificationMessage;
			section = messages().additions().forType(type);
		}

		@Override
		public ReactionStage<Void> execute() {
			if (!command().hasNext()) {
				sender().sendMessage(section.usage());
				return null;
			}
			String targetArg = command().next();
			return parseVictim(sender(), command(), targetArg, type()).thenCompose((victim) -> {
				if (victim == null) {
					return completedFuture(null);
				}
				if (!permissionCheck.checkPermission(victim, section.permission())) {
					return completedFuture(null);
				}
				return performEnact(victim, targetArg);

			}).thenCompose((punishment) -> {
				if (punishment == null) {
					return completedFuture(null);
				}
				return enforceAndSendSuccess(punishment, targetArg);
			});
		}

		private CompletionStage<Punishment> performEnact(Victim victim, String targetArg) {
			// Parse duration, uses Duration.ZERO for permanent
			Duration duration = parseDuration();

			// Check duration permissions
			if (!new DurationPermissionCheck(sender(), config()).isDurationPermitted(type(), duration)) {
				// Sender does not have enough duration permissions
				String durationFormatted = formatter.formatDuration(duration);
				PunishmentAdditionWithDurationPerm sectionWithDurationPerm = ((PunishmentAdditionWithDurationPerm) section);
				sender().sendMessage(sectionWithDurationPerm.permission().duration().replaceText("%DURATION%", durationFormatted));
				return completedFuture(null);
			}
			// Parse reason
			String reason;
			{
				// Evaluate -s before using allRemaining
				notificationMessage.evaluate(command());

				if (command().hasNext()) {
					reason = command().allRemaining();
				} else {
					ReasonsConfig reasonsConfig = config().reasons();
					switch (reasonsConfig.effectiveUnspecifiedReasonBehavior()) {
					case USE_EMPTY_REASON:
						reason = "";
						break;
					case REQUIRE_REASON:
						sender().sendMessage(section.usage());
						return completedFuture(null);
					case SUBSTITUTE_DEFAULT:
						reason = reasonsConfig.defaultReason();
						break;
					default:
						throw new IllegalArgumentException("Unknown unspecified reason behavior "
								+ reasonsConfig.effectiveUnspecifiedReasonBehavior());
					}
				}
			}

			DraftPunishment draftPunishment = drafter.draftBuilder()
					.type(type()).victim(victim).operator(sender().getOperator())
					.reason(reason).duration(duration)
					.build();

			return fireWithTimeout(new PunishEventImpl(draftPunishment)).thenCompose((event) -> {
				if (event.isCancelled()) {
					return completedFuture(null);
				}
				// Enforce the punishment later, after we are sure it is valid
				EnforcementOptions enforcementOptions = draftPunishment
						.enforcementOptionsBuilder()
						.enforcement(EnforcementOptions.Enforcement.NONE)
						.broadcasting(EnforcementOptions.Broadcasting.NONE)
						.build();
				return draftPunishment.enactPunishment(enforcementOptions).thenApply((optPunishment) -> {
					if (optPunishment.isEmpty()) {
						sendConflict(targetArg);
						return null;
					}
					return optPunishment.get();
				});
			});
		}

		/*
		 * Argument parsing
		 */

		private Duration parseDuration() {
			if (type() == PunishmentType.KICK) {
				return Duration.ZERO; // Always permanent
			}
			if (command().hasNext()) {
				String time = command().peek();
				Duration parsed = new DurationParser(messages().formatting().permanentArguments()).parse(time);
				if (!parsed.isNegative()) {
					// Successful parse; consume this argument
					command().next();
					return parsed;
				}
			}
			// Fallback to permanent if unable to parse
			return Duration.ZERO;
		}

		// Outcomes

		private void sendConflict(String targetArg) {
			ComponentLike message;
			if (type().isSingular()) {
				message = ((ExclusivePunishmentAddition) section).conflicting().replaceText("%TARGET%", targetArg);
			} else {
				message = messages().misc().unknownError();
			}
			sender().sendMessage(message);
		}

		private CentralisedFuture<Void> enforceAndSendSuccess(Punishment punishment, String targetArg) {

			EnforcementOptions enforcementOptions = EnforcementOpts
					.builder()
					.enforcement(EnforcementOptions.Enforcement.GLOBAL)
					.broadcasting(notificationMessage.isSilent() ?
							EnforcementOptions.Broadcasting.SILENT : EnforcementOptions.Broadcasting.NORMAL
					)
					.targetArgument(targetArg)
					.build();
			CentralisedFuture<?> enforcement = punishment
					.enforcePunishment(enforcementOptions)
					.toCompletableFuture();
			CentralisedFuture<Component> futureMessage = formatter.formatWithPunishment(
					section.successMessage().replaceText("%TARGET%", targetArg), punishment);

			return futuresFactory().allOf(enforcement, futureMessage).thenCompose((ignore) -> {
				return fireWithTimeout(new PostPunishEventImpl(punishment));
			}).thenRun(() -> {
				sender().sendMessage(futureMessage.join());
			});
		}
		
	}

}

/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.event.BasePunishEvent;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.commands.extra.DurationParser;
import space.arim.libertybans.core.commands.extra.ParseScope;
import space.arim.libertybans.core.commands.extra.ReasonsConfig;
import space.arim.libertybans.core.commands.extra.TabCompletion;
import space.arim.libertybans.core.config.AdditionAssistant;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.config.PunishmentAdditionSection;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.event.PunishEventImpl;
import space.arim.libertybans.core.punish.Mode;
import space.arim.libertybans.core.punish.permission.DurationPermissionCheck;
import space.arim.libertybans.core.punish.permission.PermissionBase;
import space.arim.libertybans.core.punish.permission.PunishmentPermission;
import space.arim.libertybans.core.punish.permission.VictimPermissionCheck;
import space.arim.libertybans.core.punish.permission.VictimTypeCheck;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Duration;
import java.util.Locale;
import java.util.stream.Stream;

abstract class PunishCommands extends AbstractSubCommandGroup implements PunishUnpunishCommands {

	private final PunishmentDrafter drafter;
	private final InternalFormatter formatter;
	private final AdditionAssistant additionAssistant;
	private final TabCompletion tabCompletion;

	PunishCommands(Dependencies dependencies, Stream<String> subCommands,
				   PunishmentDrafter drafter, InternalFormatter formatter,
				   AdditionAssistant additionAssistant, TabCompletion tabCompletion) {
		super(dependencies, subCommands);
		this.drafter = drafter;
		this.formatter = formatter;
		this.additionAssistant = additionAssistant;
		this.tabCompletion = tabCompletion;
	}

	@Override
	public final CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		PunishmentType type = parseType(arg.toUpperCase(Locale.ROOT));
		PunishmentAdditionSection section = messages().additions().forType(type);
		var client = new AdditionAssistant.Client<DraftPunishment, Duration>() {

			@Override
			public ReactionStage<Victim> parseVictim(String targetArg) {
				return PunishCommands.this.parseVictim(sender, command, targetArg, type);
			}

			@Override
			public Duration parseImplement() {
				if (type == PunishmentType.KICK) {
					return Duration.ZERO; // Always permanent
				}
				if (command.hasNext()) {
					String time = command.peek();
					Duration parsed = new DurationParser(messages().formatting().permanentArguments()).parse(time);
					if (!parsed.isNegative()) {
						// Successful parse; consume this argument
						command.next();
						return parsed;
					}
				}
				// Fallback to permanent if unable to parse
				return Duration.ZERO;
			}

			@Override
			public PermissionBase createPermission(Duration duration) {
				return new PunishmentPermission(type, Mode.DO);
			}

			@Override
			public VictimPermissionCheck createPermissionCheck(Duration duration) {
				return VictimPermissionCheck.combine(
						new VictimTypeCheck(sender, createPermission(duration)),
						new DurationPermissionCheck(sender, config().durationPermissions(), formatter, type, duration)
				);
			}

			@Override
			public String exemptionCategory() {
				return type.name().toLowerCase(Locale.ROOT);
			}

			@Override
			public @Nullable DraftPunishment buildDraftSanction(Victim victim, Duration duration, String targetArg) {

				ServerScope serverScope = argumentParser().parseScope(
						sender, command, ParseScope.fallbackToDefaultPunishingScope()
				);
				if (serverScope == null) {
					return null;
				}
				String reason;
				if (command.hasNext()) {
					reason = command.allRemaining();
				} else {
					ReasonsConfig reasonsConfig = config().reasons();
					reason = switch (reasonsConfig.effectiveUnspecifiedReasonBehavior()) {
						case USE_EMPTY_REASON -> "";
						case REQUIRE_REASON -> null; // Exit below
						case SUBSTITUTE_DEFAULT -> reasonsConfig.defaultReason();
					};
					if (reason == null) {
						sender.sendMessage(section.usage());
						return null;
					}
				}
				return drafter.draftBuilder()
						.type(type)
						.victim(victim)
						.operator(sender.getOperator())
						.reason(reason)
						.duration(duration)
						.scope(serverScope)
						.build();
			}

			@Override
			public BasePunishEvent<DraftPunishment> constructEvent(DraftPunishment draftSanction) {
				return new PunishEventImpl(draftSanction, sender);
			}
		};
		return additionAssistant.new Execution<>(sender, command, section, client);
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

	@Override
	public boolean hasTabCompletePermission(CmdSender sender, String arg) {
		PunishmentType type = parseType(arg.toUpperCase(Locale.ROOT));
		VictimTypeCheck permissionCheck = new VictimTypeCheck(sender, new PunishmentPermission(type, Mode.DO));
		return hasTabCompletePermission(permissionCheck);
	}

	public abstract boolean hasTabCompletePermission(VictimTypeCheck permissionCheck);

}

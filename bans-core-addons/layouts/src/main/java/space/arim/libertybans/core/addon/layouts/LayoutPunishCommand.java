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

package space.arim.libertybans.core.addon.layouts;

import jakarta.inject.Inject;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.event.BasePunishEvent;
import space.arim.libertybans.api.punish.CalculablePunishment;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.core.commands.AbstractSubCommandGroup;
import space.arim.libertybans.core.commands.CommandExecution;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.commands.extra.ParsePlayerVictimDynamicallyComposite;
import space.arim.libertybans.core.commands.extra.TabCompletion;
import space.arim.libertybans.core.config.AdditionAssistant;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.event.CalculatedPunishEventImpl;
import space.arim.libertybans.core.punish.permission.PermissionBase;
import space.arim.libertybans.core.punish.permission.VictimPermissionCheck;
import space.arim.libertybans.core.punish.permission.VictimTypeCheck;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.Locale;
import java.util.stream.Stream;

public final class LayoutPunishCommand extends AbstractSubCommandGroup {

	private final PunishmentDrafter drafter;
	private final ScopeManager scopeManager;
	private final AdditionAssistant additionAssistant;
	private final TabCompletion tabCompletion;
	private final LayoutsAddon addon;

	@Inject
	public LayoutPunishCommand(Dependencies dependencies, PunishmentDrafter drafter, ScopeManager scopeManager,
							   AdditionAssistant additionAssistant, TabCompletion tabCompletion, LayoutsAddon addon) {
		super(dependencies, "punish");
		this.drafter = drafter;
		this.scopeManager = scopeManager;
		this.additionAssistant = additionAssistant;
		this.tabCompletion = tabCompletion;
		this.addon = addon;
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		LayoutsConfig config = addon.config();
		if (!hasPermission(sender)) {
			sender.sendMessage(config.permission().layoutsGenerally());
			return () -> null;
		}
		var client = new AdditionAssistant.Client<CalculablePunishment, Track>() {

			@Override
			public ReactionStage<Victim> parseVictim(String targetArg) {
				return argumentParser().parseVictim(
						sender, targetArg, new ParsePlayerVictimDynamicallyComposite(configs(), command)
				);
			}

			@Override
			public @Nullable Track parseImplement() {
				if (!command.hasNext()) {
					sender.sendMessage(config.usage());
					return null;
				}
				String id = command.next().toLowerCase(Locale.ROOT);
				Track.Ladder ladder = config.tracks().get(id);
				if (ladder == null) {
					sender.sendMessage(config.trackDoesNotExist().replaceText("%TRACK_ARG%", id));
					return null;
				}
				return new Track(id, ladder);
			}

			@Override
			public PermissionBase createPermission(Track track) {
				return new LayoutPermission(track);
			}

			@Override
			public VictimPermissionCheck createPermissionCheck(Track track) {
				return new VictimTypeCheck(sender, createPermission(track));
			}

			@Override
			public String exemptionCategory() {
				return "layout";
			}

			@Override
			public CalculablePunishment buildDraftSanction(Victim victim, Track track, String targetArg) {
				return drafter
						.calculablePunishmentBuilder()
						.victim(victim)
						.operator(sender.getOperator())
						.escalationTrack(EscalationTrack.createDefault(track.id()))
						.calculator(new TrackCalculator(scopeManager, track.ladder()))
						.build();
			}

			@Override
			public BasePunishEvent<CalculablePunishment> constructEvent(CalculablePunishment draftSanction) {
				return new CalculatedPunishEventImpl(draftSanction);
			}

		};
		return additionAssistant.new Execution<>(sender, command, config, client);
	}

	@Override
	public Stream<String> suggest(CmdSender sender, String arg, int argIndex) {
		if (argIndex == 0) {
			return tabCompletion.completeOfflinePlayerNames(sender);
		}
		if (argIndex == 1) {
			return addon.config().tracks().keySet().stream();
		}
		return Stream.empty();
	}

	@Override
	public boolean hasTabCompletePermission(CmdSender sender, String arg) {
		return hasPermission(sender);
	}

	private boolean hasPermission(CmdSender sender) {
		return sender.hasPermission("libertybans.addon.layout.command");
	}

}

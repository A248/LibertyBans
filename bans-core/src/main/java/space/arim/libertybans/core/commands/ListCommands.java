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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.checkerframework.checker.nullness.qual.NonNull;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.api.select.SelectionBase;
import space.arim.libertybans.api.select.SelectionBuilderBase;
import space.arim.libertybans.api.select.SelectionOrderBuilder;
import space.arim.libertybans.api.select.SelectionPredicate;
import space.arim.libertybans.core.commands.extra.AsCompositeWildcard;
import space.arim.libertybans.core.commands.extra.ParseScope;
import space.arim.libertybans.core.commands.extra.ParseVictim;
import space.arim.libertybans.core.commands.extra.TabCompletion;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.config.ListSection;
import space.arim.libertybans.core.config.ListSection.ListType;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static space.arim.libertybans.api.select.SelectionPredicate.matchingAnyOf;

@Singleton
public class ListCommands extends AbstractSubCommandGroup {

	private final PunishmentSelector selector;
	private final InternalFormatter formatter;
	private final TabCompletion tabCompletion;

	@Inject
	public ListCommands(Dependencies dependencies, PunishmentSelector selector,
						InternalFormatter formatter, TabCompletion tabCompletion) {
		super(dependencies, "banlist", "mutelist", "history", "warns", "blame");
		this.selector = selector;
		this.formatter = formatter;
		this.tabCompletion = tabCompletion;
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		return new Execution(sender, command, ListType.fromString(arg));
	}

	@Override
	public Stream<String> suggest(CmdSender sender, String arg, int argIndex) {
		if (argIndex == 0) {
			ListType listType = ListType.fromString(arg);
			if (listType.requiresTarget()) {
				return tabCompletion.completeOfflinePlayerNames(sender);
			}
		}
		return Stream.empty();
	}

	@Override
	public boolean hasTabCompletePermission(CmdSender sender, String arg) {
		return ListType.fromString(arg).hasPermission(sender);
	}

	private class Execution extends AbstractCommandExecution {

		private final ListType listType;
		private final ListSection.PunishmentList section;

		private String target;

		Execution(CmdSender sender, CommandPackage command, ListType listType) {
			super(sender, command);
			this.listType = listType;
			section = messages().lists().forType(listType);
		}

		@Override
		public ReactionStage<Void> execute() {
			if (!listType.hasPermission(sender())) {
				sender().sendMessage(section.permissionCommand());
				return null;
			}
			return switch (listType) {
			case BANLIST ->
					parsePageThenExecute(selector.selectionBuilder().type(PunishmentType.BAN));
			case MUTELIST ->
					parsePageThenExecute(selector.selectionBuilder().type(PunishmentType.MUTE));
			case HISTORY, WARNS -> {
				if (!command().hasNext()) {
					sender().sendMessage(section.usage());
					yield null;
				}
				target = command().next();

				if (config().commands().showApplicableForHistory()) {
					yield historyOrWarns(
							argumentParser().parsePlayer(sender(), target),
							(uuidAndAddress) -> selector.selectionByApplicabilityBuilder(
									uuidAndAddress.uuid(), uuidAndAddress.address()
							)
					);
				} else {
					yield historyOrWarns(
							argumentParser().parseVictim(
									sender(), target, ParseVictim.ofPreferredType(Victim.VictimType.PLAYER)
							),
							(victim) -> {
								// Select punishments made against this user OR against the composite user
								CompositeVictim compositeWildcard = new AsCompositeWildcard().apply(victim);
								SelectionPredicate<Victim> victimSelection = matchingAnyOf(victim, compositeWildcard);
								return selector.selectionBuilder().victims(victimSelection);
							}
					);
				}
			}
			case BLAME -> {
				if (!command().hasNext()) {
					sender().sendMessage(section.usage());
					yield null;
				}
				target = command().next();

				SelectionOrderBuilder selectionBuilder = selector.selectionBuilder();
				selectionBuilder.selectActiveOnly(config().commands().blameShowsActiveOnly());
				yield argumentParser().parseOperator(
						sender(), target
				).thenCompose((operator) -> {
					if (operator == null) {
						return completedFuture(null);
					}
					return parsePageThenExecute(selectionBuilder.operator(operator));
				});
			}
			};
		}

		private <V> ReactionStage<Void> historyOrWarns(CentralisedFuture<V> parseVictim,
													   Function<@NonNull V, SelectionBuilderBase<?, ?>> createSelection) {
			return parseVictim.thenCompose((victim) -> {
				if (victim == null) {
					return completedFuture(null);
				}
				SelectionBuilderBase<?, ?> selectionBuilder = createSelection.apply(victim);
				if (listType == ListType.HISTORY) {
					selectionBuilder.selectAll();
				} else {
					assert listType == ListType.WARNS;
					selectionBuilder.type(PunishmentType.WARN);
				}
				return parsePageThenExecute(selectionBuilder);
			});
		}

		private ReactionStage<Void> parsePageThenExecute(SelectionBuilderBase<?, ?> selectionBuilder) {
			int selectedPage = parsePage();
			if (selectedPage <= 0) {
				sender().sendMessage(section.usage());
				return completedFuture(null);
			}
			SelectionPredicate<ServerScope> scopeSelection = argumentParser().parseScope(
					sender(), command(), ParseScope.selectionPredicate()
			);
			if (scopeSelection == null) {
				return completedFuture(null);
			}
			int perPage = section.perPage();
			SelectionBase selection = selectionBuilder
					.scopes(scopeSelection)
					.skipFirstRetrieved(perPage * (selectedPage - 1))
					.limitToRetrieve(perPage)
					.build();
			return continueWithPageAndSelection(selection, selectedPage);
		}

		private int parsePage() {
			int page = 1;
			if (command().hasNext()) {
				String rawPage = command().next();
				try {
					page = Integer.parseInt(rawPage);
				} catch (NumberFormatException ex) {
					page = -1;
				}
			}
			return page;
		}

		private ReactionStage<Void> continueWithPageAndSelection(SelectionBase selection, int page) {
			return selection.getAllSpecificPunishments().thenCompose((punishments) -> {
				return showPunishmentsOnPage(punishments, page);
			});
		}

		private String replaceTargetIn(String str) {
			return (target == null) ? str : str.replace("%TARGET%", target);
		}

		private void noPunishmentsOnThisPage(int page) {
			if (page == 1) { // No pages whatsoever
				ComponentText noPages = section.noPages();
				ComponentLike message = (target == null) ? noPages : noPages.replaceText("%TARGET%", target);
				sender().sendMessage(message);

			} else { // Page does not exist
				String pageString = Integer.toString(page);
				sender().sendMessage(section.maxPages().replaceText((str) -> {
					str = str.replace("%PAGE%", pageString);
					return replaceTargetIn(str);
				}));
			}
		}

		private CentralisedFuture<Void> showPunishmentsOnPage(List<Punishment> punishments, int page) {
			if (punishments.isEmpty()) {
				noPunishmentsOnThisPage(page);
				return completedFuture(null);
			}

			ComponentText body = section.layoutBody();
			Map<Punishment, CentralisedFuture<Component>> entries = new HashMap<>(punishments.size());
			for (Punishment punishment : punishments) {
				entries.put(punishment, formatter.formatWithPunishment(body, punishment));
			}

			String pageString = Integer.toString(page);
			String nextPageString = Integer.toString(page + 1);
			String previousPageString = Integer.toString(page - 1);
			class HeaderFooterReplacer implements UnaryOperator<String> {
				@Override
				public String apply(String str) {
					str = str.replace("%PAGE%", pageString)
							.replace("%NEXTPAGE%", nextPageString)
							.replace("%PREVIOUSPAGE%", previousPageString);
					return replaceTargetIn(str);
				}
			}
			UnaryOperator<String> headerFooterReplacer = new HeaderFooterReplacer();
			ComponentLike header = section.layoutHeader().replaceText(headerFooterReplacer);
			ComponentLike footer = section.layoutFooter().replaceText(headerFooterReplacer);

			return futuresFactory().allOf(entries.values()).thenRun(() -> {

				List<Component> punishmentMessages = new ArrayList<>(punishments.size());
				for (Punishment punishment : punishments) {
					punishmentMessages.add(entries.get(punishment).join());
				}
				Component joinedMessage = Component.join(Component.newline(),
						header,
						Component.join(Component.newline(), punishmentMessages),
						footer
				);
				sender().sendMessage(joinedMessage);
			});
		}
		
	}

}

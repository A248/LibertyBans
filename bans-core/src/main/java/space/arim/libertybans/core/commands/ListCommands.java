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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.api.select.SelectionOrder;
import space.arim.libertybans.api.select.SelectionOrderBuilder;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.config.ListSection;
import space.arim.libertybans.core.config.ListSection.ListType;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@Singleton
public class ListCommands extends AbstractSubCommandGroup {

	private final PunishmentSelector selector;
	private final InternalFormatter formatter;
	
	@Inject
	public ListCommands(Dependencies dependencies, PunishmentSelector selector, InternalFormatter formatter) {
		super(dependencies, "banlist", "mutelist", "history", "warns", "blame");
		this.selector = selector;
		this.formatter = formatter;
	}
	
	@Override
	public Stream<String> suggest(CmdSender sender, String arg, int argIndex) {
		if (argIndex == 0) {
			ListType listType = ListType.valueOf(arg.toUpperCase(Locale.ROOT));
			if (listType.requiresTarget()) {
				return sender.getPlayersOnSameServer();
			}
		}
		return Stream.empty();
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		ListType listType = ListType.valueOf(arg.toUpperCase(Locale.ROOT));
		return new Execution(sender, command, listType);
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
		public void execute() {
			if (!sender().hasPermission("libertybans.list." + listType)) {
				sender().sendMessage(section.permissionCommand());
				return;
			}
			SelectionOrderBuilder selectionOrderBuilder = selector.selectionBuilder();
			switch (listType) {
			case BANLIST:
				parsePageThenExecute(selectionOrderBuilder.type(PunishmentType.BAN));
				return;
			case MUTELIST:
				parsePageThenExecute(selectionOrderBuilder.type(PunishmentType.MUTE));
				return;
			case WARNS:
				selectionOrderBuilder.type(PunishmentType.WARN);
				break;
			case HISTORY:
				selectionOrderBuilder.selectAll();
				break;
			case BLAME:
				break;
			default:
				throw new IllegalArgumentException("Could not recognise " + listType);
			}
			assert listType.requiresTarget() : listType;

			if (!command().hasNext()) {
				sender().sendMessage(section.usage());
				return;
			}
			target = command().next();
			CentralisedFuture<SelectionOrderBuilder> parseTargetFuture;
			switch (listType) {
			case HISTORY:
			case WARNS:
				parseTargetFuture = argumentParser().parseVictimByName(sender(), target).thenApply(selectionOrderBuilder::victim);
				break;
			case BLAME:
				parseTargetFuture = argumentParser().parseOperatorByName(sender(), target).thenApply(selectionOrderBuilder::operator);
				break;
			default:
				throw new IllegalArgumentException("Could not recognise " + listType);
			}
			postFuture(parseTargetFuture.thenCompose(this::parsePageThenExecuteFuture));
		}
		
		private void parsePageThenExecute(SelectionOrderBuilder selectionOrderBuilder) {
			postFuture(parsePageThenExecuteFuture(selectionOrderBuilder));
		}
		
		private ReactionStage<?> parsePageThenExecuteFuture(SelectionOrderBuilder selectionOrderBuilder) {
			int selectedPage = parsePage();
			if (selectedPage < 0) {
				sender().sendMessage(section.usage());
				return completedFuture(null);
			}
			int perPage = section.perPage();
			SelectionOrder selection = selectionOrderBuilder
					.skipFirstRetrieved(perPage * (selectedPage - 1))
					.maximumToRetrieve(selectedPage * perPage)
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

		private ReactionStage<?> continueWithPageAndSelection(SelectionOrder selectionOrder, int page) {
			return selectionOrder.getAllSpecificPunishments().thenCompose((punishments) -> {
				return showPunishmentsOnPage(punishments, page);
			});
		}

		private String replaceTargetIn(String str) {
			return (target == null) ? str : str.replace("%TARGET%", target);
		}

		private void noPunishmentsOnThisPage(int page) {
			if (page == 0) { // No pages whatsoever
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

		private CentralisedFuture<?> showPunishmentsOnPage(List<Punishment> punishments, int page) {
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
			class HeaderFooterReplacer implements UnaryOperator<String> {
				@Override
				public String apply(String str) {
					str = str.replace("%PAGE%", pageString)
							.replace("%NEXTPAGE%", nextPageString);
					return replaceTargetIn(str);
				}
			}
			UnaryOperator<String> headerFooterReplacer = new HeaderFooterReplacer();
			ComponentLike header = section.layoutHeader().replaceText(headerFooterReplacer);
			ComponentLike footer = section.layoutFooter().replaceText(headerFooterReplacer);

			return futuresFactory().allOf(entries.values()).thenRun(() -> {
				sender().sendMessage(header);
				for (Punishment punishment : punishments) {
					sender().sendMessage(entries.get(punishment).join());
				}
				sender().sendMessage(footer);
			});
		}
		
	}

}

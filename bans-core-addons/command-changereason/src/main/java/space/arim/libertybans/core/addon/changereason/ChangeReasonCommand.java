/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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

package space.arim.libertybans.core.addon.changereason;

import jakarta.inject.Inject;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.commands.AbstractCommandExecution;
import space.arim.libertybans.core.commands.AbstractSubCommandGroup;
import space.arim.libertybans.core.commands.CommandExecution;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.commands.extra.DurationParser;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.config.displayid.AbacusForIds;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Duration;
import java.util.stream.Stream;

public final class ChangeReasonCommand extends AbstractSubCommandGroup {

	private final ChangeReasonAddon addon;
	private final PunishmentSelector selector;
	private final AbacusForIds abacusForIds;
	private final InternalFormatter formatter;

	@Inject
	public ChangeReasonCommand(Dependencies dependencies, ChangeReasonAddon addon,
	                           PunishmentSelector selector, AbacusForIds abacusForIds, InternalFormatter formatter) {
		super(dependencies, "changereason");
		this.addon = addon;
		this.selector = selector;
		this.abacusForIds = abacusForIds;
		this.formatter = formatter;
    }

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		return new Execution(sender, command);
	}

	@Override
	public Stream<String> suggest(CmdSender sender, String arg, int argIndex) {
		return Stream.empty();
	}

	@Override
	public boolean hasTabCompletePermission(CmdSender sender, String arg) {
		return hasPermission(sender);
	}

	private boolean hasPermission(CmdSender sender) {
		return sender.hasPermission("libertybans.addon.changereason.use");
	}

	private final class Execution extends AbstractCommandExecution {

		private final ChangeReasonConfig config;

		private Execution(CmdSender sender, CommandPackage command) {
			super(sender, command);
			this.config = addon.config();
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
			String idString = command().next();
			Long id = abacusForIds.parseId(idString);
			if (id == null) {
				sender().sendMessage(config.usage());
				return null;
			}
			if (!command().hasNext()) {
				sender().sendMessage(config.usage());
				return null;
			}
			String newReason = command().next();
			return selector.getHistoricalPunishmentById(id).thenCompose((optPunishment) -> {
				if (optPunishment.isEmpty()) {
					sender().sendMessage(config.notFound().replaceText("%ID%", idString));
					return completedFuture(null);
				}
				Punishment oldPunishment = optPunishment.get();
				String oldReason = oldPunishment.getReason();
				return oldPunishment.modifyPunishment((editor) -> {
					editor.setReason(newReason);
				}).thenCompose((newPunishment) -> {
					if (newPunishment.isEmpty()) {
						return futuresFactory().completedFuture(config.notFound().replaceText("%ID%", idString).asComponent());
					}
					return formatter.formatWithPunishment(
							config.success().replaceText("%OLD_REASON%", oldReason), newPunishment.get()
					);
				}).thenAccept(sender()::sendMessage);
			});
		}
	}

}

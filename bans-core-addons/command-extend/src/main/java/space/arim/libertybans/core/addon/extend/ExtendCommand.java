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

package space.arim.libertybans.core.addon.extend;

import jakarta.inject.Inject;
import org.jetbrains.annotations.Nullable;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.commands.AbstractCommandExecution;
import space.arim.libertybans.core.commands.AbstractSubCommandGroup;
import space.arim.libertybans.core.commands.CommandExecution;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.commands.extra.DurationParser;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Duration;
import java.util.stream.Stream;

public final class ExtendCommand extends AbstractSubCommandGroup {

	private final ExtendAddon addon;
	private final PunishmentSelector selector;
	private final InternalFormatter formatter;

	@Inject
	public ExtendCommand(Dependencies dependencies, ExtendAddon addon,
						 PunishmentSelector selector, InternalFormatter formatter) {
		super(dependencies, "extend");
		this.addon = addon;
		this.selector = selector;
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
		return sender.hasPermission("libertybans.addon.extend.use");
	}

	private final class Execution extends AbstractCommandExecution {

		private final ExtendConfig config;

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
			long id;
			try {
				id = Long.parseLong(idString);
			} catch (NumberFormatException ignored) {
				sender().sendMessage(config.usage());
				return null;
			}
			if (!command().hasNext()) {
				sender().sendMessage(config.usage());
				return null;
			}
			String durationArg = command().next();
			Duration extension = new DurationParser(messages().formatting().permanentArguments()).parse(durationArg);
			if (extension.isNegative()) {
				sender().sendMessage(config.invalidDuration().replaceText("%DURATION_ARG%", durationArg));
				return null;
			}
			return selector.getHistoricalPunishmentById(id).thenCompose((optPunishment) -> {
				if (optPunishment.isEmpty()) {
					sender().sendMessage(config.notFound().replaceText("%ID%", idString));
					return completedFuture(null);
				}
				Punishment oldPunishment = optPunishment.get();
				if (oldPunishment.getType() == PunishmentType.KICK) {
					sender().sendMessage(config.cannotExtendKicks().replaceText("%ID%", idString));
					return completedFuture(null);
				}
				return oldPunishment.modifyPunishment((editor) -> {
					if (extension.isZero()) {
						editor.setEndDate(Punishment.PERMANENT_END_DATE);
					} else {
						editor.extendEndDate(extension);
					}
				}).thenCompose((newPunishment) -> {
					if (newPunishment.isEmpty()) {
						return futuresFactory().completedFuture(config.notFound().replaceText("%ID%", idString).asComponent());
					}
					return formatter.formatWithPunishment(config.success(), newPunishment.get());
				}).thenAccept(sender()::sendMessage);
			});
		}
	}

}

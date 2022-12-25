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

package space.arim.libertybans.core.addon.expunge;

import jakarta.inject.Inject;
import org.jetbrains.annotations.Nullable;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.punish.PunishmentRevoker;
import space.arim.libertybans.core.commands.AbstractCommandExecution;
import space.arim.libertybans.core.commands.AbstractSubCommandGroup;
import space.arim.libertybans.core.commands.CommandExecution;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.stream.Stream;

public final class ExpungeCommand extends AbstractSubCommandGroup {

	private final ExpungeAddon addon;
	private final PunishmentRevoker revoker;

	@Inject
	public ExpungeCommand(Dependencies dependencies, ExpungeAddon addon, PunishmentRevoker revoker) {
		super(dependencies, "expunge");
		this.addon = addon;
		this.revoker = revoker;
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
		return sender.hasPermission("libertybans.addon.expunge.use");
	}

	private final class Execution extends AbstractCommandExecution {

		private final ExpungeConfig config;

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
			return revoker.expungePunishment(id).expunge().thenAccept((expunged) -> {
				ComponentText message = (expunged) ? config.success() : config.notFound();
				sender().sendMessage(message.replaceText("%ID%", idString));
			});
		}
	}

}

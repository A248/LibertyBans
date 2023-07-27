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

package space.arim.libertybans.env.sponge;

import jakarta.inject.Inject;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.commands.StringCommandPackage;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.Interlocutor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class CommandHandler implements Command.Raw {

	private final InternalFormatter formatter;
	private final Interlocutor interlocutor;
	private final Commands commands;
	private final Game game;

	@Inject
	public CommandHandler(InternalFormatter formatter, Interlocutor interlocutor,
						  Commands commands, Game game) {
		this.formatter = formatter;
		this.interlocutor = interlocutor;
		this.commands = commands;
		this.game = game;
	}

	private CmdSender adaptSender(CommandCause platformSender) {
		if (platformSender.root() instanceof ServerPlayer player) {
			return new SpongeCmdSender.PlayerCmdSender(formatter, interlocutor, player, game);
		}
		return new SpongeCmdSender.ConsoleSender(formatter, interlocutor, platformSender, game);
	}

	@Override
	public CommandResult process(CommandCause cause, ArgumentReader.Mutable arguments) throws CommandException {
		CommandPackage command = StringCommandPackage.create(arguments.input());
		commands.execute(adaptSender(cause), command);
		return CommandResult.success();
	}

	@Override
	public List<CommandCompletion> complete(CommandCause cause, ArgumentReader.Mutable arguments) throws CommandException {
		List<String> suggestions = commands.suggest(adaptSender(cause), arguments.input().split(" "));
		CommandCompletion[] completions = new CommandCompletion[suggestions.size()];
		for (int n = 0; n < completions.length; n++) {
			completions[n] = CommandCompletion.of(suggestions.get(n));
		}
		return Arrays.asList(completions);
	}

	@Override
	public boolean canExecute(CommandCause cause) {
		return commands.hasPermissionFor(adaptSender(cause), Commands.BASE_COMMAND_NAME);
	}

	@Override
	public Optional<Component> shortDescription(CommandCause cause) {
		return Optional.empty();
	}

	@Override
	public Optional<Component> extendedDescription(CommandCause cause) {
		return Optional.empty();
	}

	@Override
	public Component usage(CommandCause cause) {
		return Component.text("<arguments>");
	}

}

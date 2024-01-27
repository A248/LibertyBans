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

package space.arim.libertybans.core.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import space.arim.injector.MultiBinding;
import space.arim.libertybans.core.commands.usage.PluginInfoMessage;
import space.arim.libertybans.core.commands.usage.UsageGlossary;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.service.FuturePoster;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class CommandsCore implements Commands {

	private final Configs configs;
	private final FuturePoster futurePoster;
	private final UsageGlossary usage;
	private final PluginInfoMessage infoMessage;

	private final Set<SubCommandGroup> subCommandGroups;

	public static final String BASE_COMMAND_PERMISSION = "libertybans.commands";

	@Inject
	public CommandsCore(Configs configs, FuturePoster futurePoster,
						UsageGlossary usage, PluginInfoMessage infoMessage,
						@MultiBinding Set<SubCommandGroup> subCommandGroups) {
		this.configs = configs;
		this.futurePoster = futurePoster;
		this.usage = usage;
		this.infoMessage = infoMessage;
		this.subCommandGroups = subCommandGroups;
	}

	private SubCommandGroup getMatchingSubCommand(String firstArg) {
		for (SubCommandGroup subCommandGroup : subCommandGroups) {
			if (subCommandGroup.matches().contains(firstArg)) {
				return subCommandGroup;
			}
		}
		return null;
	}

	/*
	 * Main command handler
	 */

	@Override
	public void execute(CmdSender sender, CommandPackage command) {
		if (!sender.hasPermission(BASE_COMMAND_PERMISSION)) {
			sender.sendMessage(configs.getMessagesConfig().all().basePermissionMessage());
			return;
		}
		if (!command.hasNext()) {
			infoMessage.send(sender);
			sender.sendLiteralMessage("&7Use '/libertybans usage' for help");
			return;
		}
		String firstArg = command.next().toLowerCase(Locale.ROOT);
		if (firstArg.equals("version") || firstArg.equals("about")) {
			infoMessage.send(sender);
			return;
		}
		SubCommandGroup subCommand = getMatchingSubCommand(firstArg);
		if (subCommand == null) {
			usage.sendUsage(sender, command, firstArg.equals("usage") || firstArg.equals("help"));
			return;
		}
		CommandExecution execution = subCommand.execute(sender, command, firstArg);
		ReactionStage<Void> future = execution.execute();
		if (future != null) {
			futurePoster.postFuture(future);
		}
	}

	/*
	 * Tab completion
	 */

	private List<String> subCommandCompletions(CmdSender sender, Predicate<String> filter) {
		Stream<String> combined = Stream.concat(
				subCommandGroups.stream().flatMap((subCommandGroup) -> {
					return subCommandGroup.matches()
							.stream()
							.filter((subCmd) -> subCommandGroup.hasTabCompletePermission(sender, subCmd));
				}),
				Stream.of("version", "about", "usage", "help")
		);
		return combined
				.filter(filter)
				.sorted()
				.toList();
	}

	@Override
	public List<String> suggest(CmdSender sender, String[] args) {

		if (!sender.hasPermission(BASE_COMMAND_PERMISSION) || !configs.getMainConfig().commands().tabComplete()) {
			// No permission or tab complete is disabled
			return List.of();
		}
		if (args.length == 0) {
			// A length of 0 means '/libertybans ' itself is tab completed
			return subCommandCompletions(sender, (subCmd) -> true);
		}
		if (args.length == 1) {
			// Length 1 means a sub-command name like '/libertybans ban' is tab completed
			String lookingFor = args[0].toLowerCase(Locale.ROOT);
			return subCommandCompletions(sender, (subCmd) -> subCmd.startsWith(lookingFor));
		}
		String firstArg = args[0].toLowerCase(Locale.ROOT);
		SubCommandGroup subCommand = getMatchingSubCommand(firstArg);
		if (subCommand == null || !subCommand.hasTabCompletePermission(sender, firstArg)) {
			return List.of();
		}
		/*
		Subtract 2 from arguments length to determine argument index
		'/libertybans ban A248' - argIndex is 0 for the first argument, which is A248
		'/libertybans ban A248 ' - argIndex is 1 for the second argument, after A248
		'/libertybans ban A248 30d' - argIndex is 1, again
		 */
		int argIndex = args.length - 2;
		Stream<String> completions = subCommand.suggest(sender, firstArg, argIndex);
		String lastArg = args[args.length - 1].toLowerCase(Locale.ROOT);
		if (!lastArg.isEmpty()) {
			completions = completions.filter((completion) -> completion.toLowerCase(Locale.ROOT).startsWith(lastArg));
		}
		return completions.sorted().toList();
	}

	@Override
	public boolean hasPermissionFor(CmdSender sender, String command) {
		if (command.equals(BASE_COMMAND_NAME)) {
			return sender.hasPermission(BASE_COMMAND_PERMISSION);
		}
		SubCommandGroup subCommand = getMatchingSubCommand(command);
		return subCommand != null && subCommand.hasTabCompletePermission(sender, command);
	}

}

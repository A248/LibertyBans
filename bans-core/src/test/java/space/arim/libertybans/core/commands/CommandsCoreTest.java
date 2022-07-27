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

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.core.commands.usage.PluginInfoMessage;
import space.arim.libertybans.core.commands.usage.UsageGlossary;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.service.FuturePoster;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CommandsCoreTest {

	private final Configs configs;
	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();
	private final FuturePoster futurePoster;
	private final UsageGlossary usage;
	private final CmdSender sender;

	public CommandsCoreTest(@Mock Configs configs, @Mock FuturePoster futurePoster,
							@Mock UsageGlossary usage, @Mock CmdSender sender) {
		this.configs = configs;
		this.futurePoster = futurePoster;
		this.usage = usage;
		this.sender = sender;
	}

	private CommandsCore newCommandsCore(Set<SubCommandGroup> subCommands) {
		return new CommandsCore(configs, futurePoster, usage, new PluginInfoMessage(), subCommands);
	}

	@Test
	public void noPermission() {
		Component noPermMessage = Component.text("No permission");

		MessagesConfig messagesConfig = mock(MessagesConfig.class);
		MessagesConfig.All all = mock(MessagesConfig.All.class);
		when(configs.getMessagesConfig()).thenReturn(messagesConfig);
		when(messagesConfig.all()).thenReturn(all);
		when(all.basePermissionMessage()).thenReturn(noPermMessage);

		CommandPackage dummyCommand = ArrayCommandPackage.create("args");
		newCommandsCore(Set.of()).execute(sender, dummyCommand);

		verify(sender).sendMessage(noPermMessage);
	}

	private void addBasePermission() {
		when(sender.hasPermission(CommandsCore.BASE_COMMAND_PERMISSION)).thenReturn(true);
	}

	@Test
	public void usageUnknownCommand() {
		addBasePermission();

		CommandPackage dummyCommand = ArrayCommandPackage.create("args");
		newCommandsCore(Set.of()).execute(sender, dummyCommand);

		verify(usage).sendUsage(sender, dummyCommand, false);
	}

	@Test
	public void explicitUsage() {
		addBasePermission();

		CommandPackage dummyCommand = ArrayCommandPackage.create("help");
		newCommandsCore(Set.of()).execute(sender, dummyCommand);

		verify(usage).sendUsage(sender, dummyCommand, true);
	}

	@Test
	public void matchCommand() {
		addBasePermission();

		SubCommandGroup subCommandOne = mock(SubCommandGroup.class);
		CommandExecution subCommandOneExecution = mock(CommandExecution.class);
		when(subCommandOne.matches()).thenReturn(Set.of("one"));
		when(subCommandOne.execute(any(), any(), any())).thenReturn(subCommandOneExecution);
		when(subCommandOneExecution.execute()).thenReturn(futuresFactory.completedFuture(null));
		SubCommandGroup subCommandTwo = mock(SubCommandGroup.class);

		CommandPackage commandOne = ArrayCommandPackage.create("one");
		newCommandsCore(Set.of(subCommandOne, subCommandTwo)).execute(sender, commandOne);

		verify(subCommandOne).execute(sender, commandOne, "one");
		verify(subCommandOneExecution).execute();
		verify(futurePoster).postFuture(notNull());
	}

	@Test
	public void matchNoCommand() {
		addBasePermission();

		SubCommandGroup subCommandOne = mock(SubCommandGroup.class);
		when(subCommandOne.matches()).thenReturn(Set.of());
		SubCommandGroup subCommandTwo = mock(SubCommandGroup.class);
		when(subCommandTwo.matches()).thenReturn(Set.of());

		CommandPackage commandNone = ArrayCommandPackage.create("none");
		newCommandsCore(Set.of(subCommandOne, subCommandTwo)).execute(sender, commandNone);

		verify(usage).sendUsage(sender, commandNone, false);
	}

	@Test
	public void nullExecution(@Mock SubCommandGroup subCommand, @Mock CommandExecution commandExecution) {
		addBasePermission();

		when(subCommand.matches()).thenReturn(Set.of("arg"));
		when(subCommand.execute(any(), any(), any())).thenReturn(commandExecution);
		when(commandExecution.execute()).thenReturn(null);

		CommandPackage command = ArrayCommandPackage.create("arg");
		newCommandsCore(Set.of(subCommand)).execute(sender, command);

		verify(subCommand).execute(sender, command, "arg");
		verify(commandExecution).execute();
		verify(futurePoster, times(0)).postFuture(isNull());
	}
}

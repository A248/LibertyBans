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

package space.arim.libertybans.env.spigot;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.env.Environment;
import space.arim.libertybans.core.env.PlatformListener;

public class SpigotEnv implements Environment {

	private final Provider<ConnectionListener> connectionListenerProvider;
	private final Provider<ChatListener> chatListenerProvider;
	private final Provider<QuitListener> quitListenerProvider;
	private final CommandHandler.CommandHelper commandHelper;

	@Inject
	public SpigotEnv(Provider<ConnectionListener> connectionListenerProvider,
					 Provider<ChatListener> chatListenerProvider,
					 Provider<QuitListener> quitListenerProvider,
					 CommandHandler.CommandHelper commandHelper) {
		this.connectionListenerProvider = connectionListenerProvider;
		this.chatListenerProvider = chatListenerProvider;
		this.quitListenerProvider = quitListenerProvider;
		this.commandHelper = commandHelper;
	}

	@Override
	public Set<PlatformListener> createListeners() {
		return Set.of(
				connectionListenerProvider.get(),
				chatListenerProvider.get(),
				quitListenerProvider.get(),
				new CommandHandler(commandHelper, Commands.BASE_COMMAND_NAME, false));
	}

	@Override
	public PlatformListener createAliasCommand(String command) {
		return new CommandHandler(commandHelper, command, true);
	}
	
}

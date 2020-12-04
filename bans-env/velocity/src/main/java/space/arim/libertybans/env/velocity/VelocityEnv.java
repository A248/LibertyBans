/* 
 * LibertyBans-env-velocity
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-velocity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-velocity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-velocity. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.velocity;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.env.Environment;
import space.arim.libertybans.core.env.PlatformListener;

@Singleton
public class VelocityEnv implements Environment {

	private final Provider<ConnectionListener> connectionListenerProvider;
	private final Provider<ChatListener> chatListenerProvider;
	private final Provider<CommandListener> commandListenerProvider;
	private final CommandHandler.DependencyPackage commandDependencies;

	@Inject
	public VelocityEnv(Provider<ConnectionListener> connectionListenerProvider,
			Provider<ChatListener> chatListenerProvider, Provider<CommandListener> commandListenerProvider,
			CommandHandler.DependencyPackage commandDependencies) {
		this.connectionListenerProvider = connectionListenerProvider;
		this.chatListenerProvider = chatListenerProvider;
		this.commandListenerProvider = commandListenerProvider;
		this.commandDependencies = commandDependencies;
	}

	@Override
	public Set<PlatformListener> createListeners() {
		return Set.of(
				connectionListenerProvider.get(),
				chatListenerProvider.get(),
				commandListenerProvider.get(),
				new CommandHandler(commandDependencies, Commands.BASE_COMMAND_NAME, false));
	}

	@Override
	public PlatformListener createAliasCommand(String command) {
		return new CommandHandler(commandDependencies, command, true);
	}

}

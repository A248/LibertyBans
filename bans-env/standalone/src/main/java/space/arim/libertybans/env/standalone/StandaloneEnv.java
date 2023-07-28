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

package space.arim.libertybans.env.standalone;

import jakarta.inject.Inject;
import space.arim.libertybans.core.env.Environment;
import space.arim.libertybans.core.env.PlatformListener;

import java.util.Set;
import java.util.function.Consumer;

final class StandaloneEnv implements Environment {

	private final CommandDispatch commandHandler;

	@Inject
	StandaloneEnv(CommandDispatch commandHandler) {
		this.commandHandler = commandHandler;
	}

	@Override
	public Set<PlatformListener> createListeners() {
		return Set.of(commandHandler);
	}

	@Override
	public PlatformListener createAliasCommand(String command) {
		// No-op
		return new PlatformListener() {
			@Override
			public void register() {

			}

			@Override
			public void unregister() {

			}
		};
	}

	@Override
	public Consumer<String> platformAccess() {
		return commandHandler;
	}

}

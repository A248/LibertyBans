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
import jakarta.inject.Singleton;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.commands.StringCommandPackage;
import space.arim.libertybans.core.env.PlatformListener;

import java.util.function.Consumer;

@Singleton
public final class CommandDispatch implements PlatformListener, Consumer<String> {

	private final Commands commands;
	private final ConsoleSender sender;

	private volatile boolean registered;

	@Inject
	CommandDispatch(Commands commands, ConsoleSender sender) {
		this.commands = commands;
		this.sender = sender;
	}

	/**
	 * Accepts and dispatches the given console command to LibertyBans.
	 *
	 * @param command the command. No 'libertybans' prefix should be added. For example, "ban A248 have fun" is valid.
	 */
	@Override
	public void accept(String command) {
		if (!registered) {
			LoggerFactory.getLogger(getClass()).warn(
					"Skipping the following command because initialization is yet incomplete: {}",
					command
			);
			return;
		}
		commands.execute(sender, StringCommandPackage.create(command));
	}

	@Override
	public void register() {
		registered = true;
	}

	@Override
	public void unregister() {
		registered = false;
	}
}

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

import java.util.Map;
import java.util.Objects;

import jakarta.inject.Inject;

import org.bukkit.command.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;

import space.arim.morepaperlib.MorePaperLib;

import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.java.JavaPlugin;

// How ironic
public class SimpleCommandMapHelper implements CommandMapHelper {

	private final MorePaperLib morePaperLib;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	private static final String MISSING_COMMAND_MAP_STATUS = 
			"LibertyBans has limited support for this situation. The plugin will continue to work, but "
			+ "command aliases will not be registered, and the main /libertybans command will not be unregistered.";
	
	@Inject
	public SimpleCommandMapHelper(MorePaperLib morePaperLib) {
		this.morePaperLib = morePaperLib;
	}

	@Override
	public CommandMap getCommandMap() {
		return morePaperLib.commandRegistration().getServerCommandMap();
	}

	@Override
	public Map<String, Command> getKnownCommands(CommandMap commandMap) {
		Objects.requireNonNull(commandMap, "commandMap");
		if (!(commandMap instanceof SimpleCommandMap simpleCommandMap)) {
			/*
			 * CommandMap was replaced by a criminal plugin
			 */
			Class<?> replacementClass = commandMap.getClass();
			String pluginName = "Unknown";
			try {
				pluginName = JavaPlugin.getProvidingPlugin(replacementClass).getDescription().getFullName();
			} catch (IllegalArgumentException ignored) {}
			logger.warn(
					"Your server's CommandMap is not an instance of SimpleCommandMap. Rather, it is {} from plugin {}. "
							+ "This could be disastrous and you should remove the offending plugin or speak to its author(s), "
							+ "as many plugins assume SimpleCommandMap as the norm. "
							+ MISSING_COMMAND_MAP_STATUS,
					replacementClass, pluginName);
			return null;
		}
		return morePaperLib.commandRegistration().getCommandMapKnownCommands(simpleCommandMap);
	}

}

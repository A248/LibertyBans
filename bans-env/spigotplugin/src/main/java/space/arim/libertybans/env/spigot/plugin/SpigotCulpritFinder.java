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

package space.arim.libertybans.env.spigot.plugin;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import space.arim.libertybans.bootstrap.CulpritFinder;

import java.util.Optional;

final class SpigotCulpritFinder implements CulpritFinder {

	@Override
	public Optional<String> findCulprit(Class<?> libraryClass) {
		JavaPlugin plugin;
		try {
			plugin = JavaPlugin.getProvidingPlugin(libraryClass);
		} catch (IllegalArgumentException ignored) {
			return Optional.empty();
		}
		PluginDescriptionFile description = plugin.getDescription();
		return Optional.of(description.getName() + " " + description.getVersion());
	}

}

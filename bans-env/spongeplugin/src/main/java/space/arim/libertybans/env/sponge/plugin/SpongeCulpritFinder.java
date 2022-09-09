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

package space.arim.libertybans.env.sponge.plugin;

import org.spongepowered.api.Game;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.metadata.PluginMetadata;
import space.arim.libertybans.bootstrap.CulpritFinder;

import java.util.Optional;

final class SpongeCulpritFinder implements CulpritFinder {

	private final Game game;

	SpongeCulpritFinder(Game game) {
		this.game = game;
	}

	@Override
	public Optional<String> findCulprit(Class<?> libraryClass) {
		for (PluginContainer plugin : game.pluginManager().plugins()) {
			Object pluginInstance = plugin.instance();
			if (pluginInstance == null) {
				continue;
			}
			if (pluginInstance.getClass().getClassLoader() == libraryClass.getClassLoader()) {
				PluginMetadata metadata = plugin.metadata();
				return Optional.of(metadata.id() + " " + metadata.version());
			}
		}
		return Optional.empty();
	}

}

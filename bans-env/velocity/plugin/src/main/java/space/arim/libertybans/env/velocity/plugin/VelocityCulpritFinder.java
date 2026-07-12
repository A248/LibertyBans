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

package space.arim.libertybans.env.velocity.plugin;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import space.arim.libertybans.bootstrap.CulpritFinder;

import java.util.Objects;
import java.util.Optional;

final class VelocityCulpritFinder implements CulpritFinder {

	private final ProxyServer server;

	VelocityCulpritFinder(ProxyServer server) {
		this.server = Objects.requireNonNull(server, "server");
	}

	@Override
	public Optional<String> findCulprit(Class<?> libraryClass) {
		for (PluginContainer plugin : server.getPluginManager().getPlugins()) {
			Object pluginInstance = plugin.getInstance().orElse(null);
			if (pluginInstance == null) {
				continue;
			}
			if (pluginInstance.getClass().getClassLoader() == libraryClass.getClassLoader()) {
				PluginDescription description = plugin.getDescription();
				return Optional.of(description.getId() + " " + description.getVersion().orElse("<No-Version>"));
			}
		}
		return Optional.empty();
	}
}

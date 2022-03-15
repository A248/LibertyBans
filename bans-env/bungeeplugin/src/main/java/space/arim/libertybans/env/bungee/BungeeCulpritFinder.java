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

package space.arim.libertybans.env.bungee;

import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.logging.Logger;

import net.md_5.bungee.api.plugin.PluginDescription;

final class BungeeCulpritFinder implements Function<Class<?>, String> {

	private final Logger logger;

	BungeeCulpritFinder(Logger logger) {
		this.logger = logger;
	}

	private PluginDescription getProvidingPluginDescription(Class<?> clazz) {
		ClassLoader pluginClassLoader = clazz.getClassLoader();
		Field descriptionField;
		try {
			descriptionField = pluginClassLoader.getClass().getDeclaredField("desc");
		} catch (NoSuchFieldException ex) {
			logger.warning("Update needed: It seems that plugin classloading has been modified");
			return null;
		}
		try {
			descriptionField.setAccessible(true);
		} catch (RuntimeException ignored) {
			// Forget about it
			return null;
		}
		Object description;
		try {
			description = descriptionField.get(pluginClassLoader);
		} catch (IllegalAccessException ex) {
			logger.warning("Could not access PluginClassLoader.desc field: " + ex.getMessage());
			return null;
		}
		if (description instanceof PluginDescription) {
			return (PluginDescription) description;
		} else {
			logger.warning("PluginClassLoader.desc field did not return a PluginDescription");
			return null;
		}
	}

	@Override
	public String apply(Class<?> clazz) {
		PluginDescription description = getProvidingPluginDescription(clazz);
		if (description != null) {
			return description.getName() + " " + description.getVersion();
		}
		return null;
	}
}

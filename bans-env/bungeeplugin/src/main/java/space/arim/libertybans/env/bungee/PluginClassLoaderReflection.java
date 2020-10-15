/* 
 * LibertyBans-env-bungeeplugin
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-bungeeplugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-bungeeplugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-bungeeplugin. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.bungee;

import java.lang.reflect.Field;

import net.md_5.bungee.api.plugin.PluginDescription;

final class PluginClassLoaderReflection {

	static String getProvidingPlugin(Class<?> clazz) {
		try {
			ClassLoader pluginClassLoader = clazz.getClassLoader();
			Field descField = pluginClassLoader.getClass().getDeclaredField("desc");
			Object descObj = descField.get(pluginClassLoader);
			if (descObj instanceof PluginDescription) {
				PluginDescription desc = (PluginDescription) descObj;
				return desc.getName() + " v" + desc.getVersion();
			}
		} catch (IllegalArgumentException | NoSuchFieldException | SecurityException | IllegalAccessException ignored) {}
		return "";
	}
	
}

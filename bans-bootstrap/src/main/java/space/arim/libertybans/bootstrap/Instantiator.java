/* 
 * LibertyBans-bootstrap
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-bootstrap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-bootstrap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-bootstrap. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.bootstrap;

import java.nio.file.Path;

public class Instantiator {

	private final Class<? extends PlatformLauncher> clazz;

	public Instantiator(String clazzName, ClassLoader loader) throws ClassNotFoundException {
		Class<?> clazz = Class.forName(clazzName, true, loader);
		if (!PlatformLauncher.class.isAssignableFrom(clazz)) {
			throw new IllegalArgumentException("Class " + clazzName + " is not a PlatformLauncher");
		}
		this.clazz = clazz.asSubclass(PlatformLauncher.class);
	}

	public <P> BaseFoundation invoke(Class<P> pluginType, P plugin, Path folder)
			throws ReflectiveOperationException, IllegalArgumentException, SecurityException {
		return clazz.getDeclaredConstructor(pluginType, Path.class).newInstance(plugin, folder).launch();
	}

}

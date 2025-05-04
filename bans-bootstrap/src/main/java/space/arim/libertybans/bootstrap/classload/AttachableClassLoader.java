/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

package space.arim.libertybans.bootstrap.classload;

import space.arim.libertybans.bootstrap.depend.BootstrapException;
import space.arim.libertybans.bootstrap.depend.JarAttachment;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;

public final class AttachableClassLoader extends URLClassLoader implements JarAttachment {

	static {
		ClassLoader.registerAsParallelCapable();
	}

	public AttachableClassLoader(String classLoaderName, ClassLoader parent) {
		super(classLoaderName, new URL[] {}, parent);
    }

	@Override
	public void addJarPath(Path jarFile) {
		URL url;
		try {
			url = jarFile.toUri().toURL();
		} catch (MalformedURLException ex) {
			throw new BootstrapException("Unable to convert Path to URL", ex);
		}
		addURL(url);
	}

	@Override
	public String toString() {
		return getClass().getName() + '{' +
				"urls=" + Arrays.toString(getURLs()) +
				", parent=" + getParent() +
				'}';
	}
}

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

package space.arim.libertybans.bootstrap;

public final class Instantiator {

	static {
		// Prevent HSQLDB from reconfiguring JUL/Log4j2
		System.setProperty("hsqldb.reconfig_logging", "false");
		// Block JOOQ startup banner and tips
		System.setProperty("org.jooq.no-logo", "true");
		System.setProperty("org.jooq.no-tips", "true");
	}

	private final Class<? extends PlatformLauncher> clazz;

	public Instantiator(String clazzName, ClassLoader loader) throws ClassNotFoundException {
		Class<?> clazz = Class.forName(clazzName, true, loader);
		if (!PlatformLauncher.class.isAssignableFrom(clazz)) {
			throw new IllegalArgumentException("Class " + clazzName + " is not a PlatformLauncher");
		}
		this.clazz = clazz.asSubclass(PlatformLauncher.class);
	}

	public <P> BaseFoundation invoke(Payload<P> payload)
			throws ReflectiveOperationException, IllegalArgumentException, SecurityException {
		return clazz.getDeclaredConstructor(Payload.class).newInstance(payload).launch();
	}

	public <P, S> BaseFoundation invoke(Payload<P> payload, Class<S> serverType, S server)
			throws ReflectiveOperationException, IllegalArgumentException, SecurityException {
		return clazz.getDeclaredConstructor(Payload.class, serverType).newInstance(payload, server).launch();
	}

}

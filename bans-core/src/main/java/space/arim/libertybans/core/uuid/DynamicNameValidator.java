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

package space.arim.libertybans.core.uuid;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class DynamicNameValidator implements NameValidator {

	private volatile NameValidator impl;

	public String detectNamePrefix() {
		Class<?> floodgateApiClass;
		try {
			floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
		} catch (ClassNotFoundException ex) {
			return "";
		}
		// Call FloodgateApi.getInstance().getPlayerPrefix()
		String playerPrefix;
		try {
			Method getInstanceMethod = floodgateApiClass.getMethod("getInstance");
			Object instance = getInstanceMethod.invoke(null);
			Method getPlayerPrefixMethod = instance.getClass().getMethod("getPlayerPrefix");
			playerPrefix = (String) getPlayerPrefixMethod.invoke(instance);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
			throw new IllegalStateException("The Floodgate API has changed incompatibly. " +
					"LibertyBans does not yet support the newer version of the Floodgate API you are using.", ex);
		}
		return playerPrefix;
	}

	private NameValidator initImpl() {
		String namePrefix = detectNamePrefix();
		if (namePrefix.isEmpty()) {
			return StandardNameValidator.vanilla();
		} else {
			return StandardNameValidator.createFromPrefix(namePrefix);
		}
	}

	private NameValidator impl() {
		if (impl == null) {
			impl = initImpl();
		}
		return impl;
	}

	@Override
	public boolean validateNameArgument(String name) {
		return impl().validateNameArgument(name);
	}

}

/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

import jakarta.inject.Inject;
import space.arim.libertybans.core.config.Configs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public final class DynamicNameValidator implements NameValidator {

	private final Configs configs;
	private volatile NameValidator impl;

	@Inject
	public DynamicNameValidator(Configs configs) {
		this.configs = configs;
	}

	private String forcedPrefix() {
		return configs.getMainConfig().uuidResolution().forceGeyserPrefix();
	}

	public String detectNamePrefix() {
		String forcedPrefix = forcedPrefix();
		if (!forcedPrefix.isEmpty()) {
			return forcedPrefix;
		}
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

	private NameValidator initImpl(String namePrefix) {
		if (namePrefix.isEmpty()) {
			return StandardNameValidator.vanilla();
		} else {
			return StandardNameValidator.createFromPrefix(namePrefix);
		}
	}

	private NameValidator impl() {
		NameValidator impl = this.impl;
		if (impl == null) {
			impl = initImpl(detectNamePrefix());
			this.impl = impl;
		} else {
			String forcedPrefix = forcedPrefix();
			if (!forcedPrefix.isEmpty() && !forcedPrefix.equals(impl.associatedPrefix())) {
				impl = initImpl(forcedPrefix);
				this.impl = impl;
			}
		}
		return impl;
	}

	@Override
	public String associatedPrefix() {
		return impl().associatedPrefix();
	}

	@Override
	public boolean validateNameArgument(String name) {
		return impl().validateNameArgument(name);
	}

	@Override
	public boolean isVanillaName(String name) {
		return impl().isVanillaName(name);
	}

	@Override
	public boolean isVanillaUUID(UUID uuid) {
		return impl().isVanillaUUID(uuid);
	}

}

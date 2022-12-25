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

package space.arim.libertybans.core.addon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

public final class AddonLoader {

	private final ServiceLoader<AddonProvider> addonProviders;

	private AddonLoader(ServiceLoader<AddonProvider> addonProviders) {
		this.addonProviders = addonProviders;
	}

	private Set<AddonBindModule> addonBindModules() {
		Set<AddonBindModule> addonBindModules = new HashSet<>();
		int count = 0;
		Logger logger = LoggerFactory.getLogger(getClass());
		for (AddonProvider addonProvider : addonProviders) {
			switch (addonProvider.availability()) {
			case YES -> {
				addonBindModules.addAll(Arrays.asList(addonProvider.bindModules()));
				count++;
			}
			case UNSATISFIED_DEPENDENCIES -> logger.warn(
					"Skipping addon {} due to unsatisfied dependencies", addonProvider.getClass().getName());
			case UNSATISFIED_PLATFORM -> logger.warn(
					"Skipping addon {} because it does not support your server platform", addonProvider.getClass().getName());
			}
		}
		if (count == 0) {
			logger.info("No addons loaded");
		} else {
			logger.info("Loaded and verified {} addons", count);
		}
		return Collections.unmodifiableSet(addonBindModules);
	}

	public static Set<AddonBindModule> loadAddonBindModules() {
		return new AddonLoader(
				ServiceLoader.load(AddonProvider.class, AddonLoader.class.getClassLoader())
		).addonBindModules();
	}

}

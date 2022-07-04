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

import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.libertybans.core.Part;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.util.stream.Stream;

public interface AddonCenter extends Part {

	/**
	 * Gets the current configuration for a specific addon
	 *
	 * @param addon the addon
	 * @param <C> the configuration type
	 * @return the current configuration
	 */
	<C extends AddonConfig> C configurationFor(Addon<C> addon);

	/**
	 * Reloads addon configuration, starting or stopping addons as necessary
	 *
	 * @return a future completed once all configurations are reloaded
	 */
	CentralisedFuture<Boolean> reloadAddons();

	/**
	 * Enumerates the identifiers of registered addons
	 *
	 * @return the addon identifiers
	 */
	Stream<String> allIdentifiers();

	/**
	 * Attempts to find an addon by a certain identifier
	 *
	 * @param identifier the identifier
	 * @return the addon if found
	 */
	@Nullable Addon<?> addonByIdentifier(String identifier);

	/**
	 * Reloads the configuration of a specific addon
	 *
	 * @param addon the addon
	 * @return a future completed once the configuration is reloaded
	 */
	CentralisedFuture<Boolean> reloadConfiguration(Addon<?> addon);

}

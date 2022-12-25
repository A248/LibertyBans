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

import space.arim.libertybans.core.commands.SubCommandGroup;

/**
 * The starting point for an addon. Implementations of this interface are discovered using the
 * service loader.
 *
 */
public interface AddonProvider {

	/**
	 * Additional bind modules to add to the injector.
	 * <p>
	 * <b>Commands</b> may be provided by binding implementations of {@link SubCommandGroup} using
	 * multi-binding.
	 * <p>
	 * Addons which need to perform startup/shutdown operations should register an implementation
	 * of {@link Addon}.
	 *
	 * @return bind modules to add
	 */
	AddonBindModule[] bindModules();

	/**
	 * Determines whether this addon can load. For example, may return {@code UNSATISFIED_DEPENDENCIES}
	 * if required dependencies are not present.
	 *
	 * @return whether this addon can load
	 */
	default Availability availability() {
		return Availability.YES;
	}

	enum Availability {
		YES,
		UNSATISFIED_DEPENDENCIES,
		UNSATISFIED_PLATFORM
	}

}

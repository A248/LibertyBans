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

package space.arim.libertybans.core.addon.exempt.vault;

import space.arim.libertybans.core.addon.AddonBindModule;
import space.arim.libertybans.core.addon.AddonProvider;

public final class ExemptionVaultProvider implements AddonProvider {

	@Override
	public AddonBindModule[] bindModules() {
		return new AddonBindModule[] {new ExemptionVaultModule()};
	}

	@Override
	public Availability availability() {
		try {
			Class.forName("org.bukkit.Server");
		} catch (ClassNotFoundException ignored) {
			return Availability.UNSATISFIED_PLATFORM;
		}
		try {
			Class.forName("net.milkbowl.vault.permission.Permission");
			return Availability.YES;
		} catch (ClassNotFoundException ignored) {
			return Availability.UNSATISFIED_DEPENDENCIES;
		}
	}

}

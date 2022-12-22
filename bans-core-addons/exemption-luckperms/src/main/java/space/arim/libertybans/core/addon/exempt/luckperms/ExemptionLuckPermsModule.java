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

package space.arim.libertybans.core.addon.exempt.luckperms;

import space.arim.injector.MultiBinding;
import space.arim.libertybans.core.addon.Addon;
import space.arim.libertybans.core.addon.AddonBindModule;
import space.arim.libertybans.core.addon.exempt.ExemptProvider;

public final class ExemptionLuckPermsModule extends AddonBindModule {

	@MultiBinding
	public Addon<?> exemptionLuckPermsAddon(ExemptionLuckPermsAddon exemptionLuckPermsAddon) {
		return exemptionLuckPermsAddon;
	}

	@MultiBinding
	public ExemptProvider exemptProvider(LuckPermsExemptProvider exemptProvider) {
		return exemptProvider;
	}

}

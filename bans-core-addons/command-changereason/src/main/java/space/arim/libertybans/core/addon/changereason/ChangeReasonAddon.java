/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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

package space.arim.libertybans.core.addon.changereason;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import space.arim.libertybans.core.addon.AbstractAddon;
import space.arim.libertybans.core.addon.AddonCenter;

@Singleton
public final class ChangeReasonAddon extends AbstractAddon<ChangeReasonConfig> {

	@Inject
	public ChangeReasonAddon(AddonCenter addonCenter) {
		super(addonCenter);
	}

	@Override
	public void startup() {

	}

	@Override
	public void shutdown() {

	}

	@Override
	public Class<ChangeReasonConfig> configInterface() {
		return ChangeReasonConfig.class;
	}

	@Override
	public String identifier() {
		return "command-changereason";
	}
}

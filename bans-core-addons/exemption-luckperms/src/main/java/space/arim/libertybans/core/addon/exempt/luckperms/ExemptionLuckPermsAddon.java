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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.core.addon.AbstractAddon;
import space.arim.libertybans.core.addon.AddonCenter;
import space.arim.omnibus.util.ThisClass;

@Singleton
public final class ExemptionLuckPermsAddon extends AbstractAddon<ExemptionLuckPermsConfig> {

	private volatile LuckPerms luckPerms;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public ExemptionLuckPermsAddon(AddonCenter addonCenter) {
		super(addonCenter);
	}

	LuckPerms luckPerms() {
		return luckPerms;
	}

	@Override
	public void startup() {
		LuckPerms luckPerms;
		try {
			luckPerms = LuckPermsProvider.get();
			logger.info("LuckPerms detected and hooked: {}", luckPerms);
		} catch (IllegalStateException ex) {
			luckPerms = null;
			logger.warn("LuckPerms is not initialized. Exemption will not function.");
		}
		this.luckPerms = luckPerms;
	}

	@Override
	public void shutdown() {
		luckPerms = null;
	}

	@Override
	public Class<ExemptionLuckPermsConfig> configInterface() {
		return ExemptionLuckPermsConfig.class;
	}

	@Override
	public String identifier() {
		return "exemption-luckperms";
	}
}

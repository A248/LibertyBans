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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.core.addon.AbstractAddon;
import space.arim.libertybans.core.addon.AddonCenter;
import space.arim.omnibus.util.ThisClass;

@Singleton
public final class ExemptionVaultAddon extends AbstractAddon<ExemptionVaultConfig> {

	private final Server server;
	private volatile Permission permissions;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public ExemptionVaultAddon(AddonCenter addonCenter, Server server) {
		super(addonCenter);
		this.server = server;
	}

	Permission permissions() {
		return permissions;
	}

	@Override
	public void startup() {
		Permission permissions = server.getServicesManager().load(Permission.class);
		if (permissions == null) {
			logger.warn("No permission plugin found. Install a Vault-compatible permissions plugin " +
					"for exemption to function.");
		} else {
			logger.info("Detected permissions provider {}", permissions);
		}
		this.permissions = permissions;
	}

	@Override
	public void shutdown() {
		permissions = null;
	}

	@Override
	public Class<ExemptionVaultConfig> configInterface() {
		return ExemptionVaultConfig.class;
	}

	@Override
	public String identifier() {
		return "exemption-vault";
	}

}

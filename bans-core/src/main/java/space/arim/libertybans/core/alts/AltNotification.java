/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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

package space.arim.libertybans.core.alts;

import jakarta.inject.Inject;
import net.kyori.adventure.text.Component;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.env.EnvEnforcer;

import java.util.List;
import java.util.UUID;

public class AltNotification {

	private final Configs configs;
	private final AltCheckFormatter altCheckFormatter;
	private final EnvEnforcer<?> envEnforcer;

	@Inject
	public AltNotification(Configs configs, AltCheckFormatter altCheckFormatter, EnvEnforcer<?> envEnforcer) {
		this.configs = configs;
		this.altCheckFormatter = altCheckFormatter;
		this.envEnforcer = envEnforcer;
	}

	/**
	 * Notifies staff members that a joining user has detected alt accounts
	 *
	 * @param uuid the user's uuid
	 * @param name the user's name
	 * @param address the user's address
	 * @param alts the alt accounts found
	 */
	public void notifyFoundAlts(UUID uuid, String name, NetworkAddress address, List<DetectedAlt> alts) {
		if (alts.isEmpty()) {
			return;
		}
		Component notification = altCheckFormatter.formatMessage(
				configs.getMessagesConfig().alts().autoShow().header(), name, alts);
		envEnforcer.sendToThoseWithPermission("libertybans.alts.autoshow", notification);
	}
}

/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.database.pagination.InstantThenUUID;
import space.arim.libertybans.core.database.pagination.KeysetPage;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

public class AltNotification {

	private final Configs configs;
	private final FactoryOfTheFuture futuresFactory;
	private final AltCheckFormatter altCheckFormatter;
	private final EnvEnforcer<?> envEnforcer;

	@Inject
	public AltNotification(Configs configs, FactoryOfTheFuture futuresFactory,
						   AltCheckFormatter altCheckFormatter, EnvEnforcer<?> envEnforcer) {
		this.configs = configs;
        this.futuresFactory = futuresFactory;
        this.altCheckFormatter = altCheckFormatter;
		this.envEnforcer = envEnforcer;
	}

	/**
	 * Notifies staff members that a joining user has detected alt accounts
	 *
	 * @param response the alt retrieval response
	 * @param name the player name
	 */
	public CentralisedFuture<Void> notifyFoundAlts(KeysetPage<DetectedAlt, InstantThenUUID> response, String name) {
		if (response.data().isEmpty()) {
			return futuresFactory.completedFuture(null);
		}
		Component notification = altCheckFormatter.formatMessage(
				configs.getMessagesConfig().alts().autoShow(), response, name, -1
		);
		return envEnforcer.sendToThoseWithPermission("libertybans.alts.autoshow", notification);
	}

}

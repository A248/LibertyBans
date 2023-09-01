/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

package space.arim.libertybans.api.user;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Query to detect alts
 *
 */
public interface AltDetectionQuery {

	/**
	 * The uuid of the player whose alts to search for
	 *
	 * @return the uuid
	 */
	UUID uuid();

	/**
	 * The network address of the player whose alts to search for
	 *
	 * @return the address
	 */
	NetworkAddress address();

	/**
	 * The punishment types to scan alts for
	 *
	 * @return the punishment types
	 */
	Set<PunishmentType> punishmentTypes();

	/**
	 * Builder for detection queries
	 *
	 */
	interface Builder {

		/**
		 * The punishment types to scan alts for. <br>
		 * <br>
		 * If enabled, the alt detection will attempt to look for punishment types applying to the specified
		 * alts on a best effort basis.
		 *
		 * @param types the punishment types to match alts with
		 * @return this builder
		 */
		default Builder punishmentTypes(PunishmentType...types) {
			return punishmentTypes(Set.of(types));
		}

		/**
		 * The punishment types to scan alts for. <br>
		 * <br>
		 * If enabled, the alt detection will attempt to look for punishment types applying to the specified
		 * alts on a best effort basis.
		 *
		 * @param types the punishment types to match alts with
		 * @return this builder
		 */
		Builder punishmentTypes(Set<PunishmentType> types);

		/**
		 * Builds into a detection query
		 *
		 * @return the alt detection query
		 */
		AltDetectionQuery build();

	}

	/**
	 * Performs the detection
	 *
	 * @return a future yielding the alt accounts
	 */
	CentralisedFuture<List<? extends AltAccount>> detect();

}

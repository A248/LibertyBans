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

import space.arim.libertybans.api.PunishmentType;

import java.time.Instant;

/**
 * A detected alt account
 *
 */
public interface AltAccount extends AccountBase {

	/**
	 * The most recent time this alt was observed
	 *
	 * @return the last time this alt account was observed
	 */
	Instant recorded();

	/**
	 * Whether the alt account has ANY active punishments of the specified type
	 *
	 * @param type the punishment type
	 * @return true if a punishment with the type exists and is active
	 * @throws IllegalArgumentException if the type was not queried for in the {@link AltDetectionQuery}
	 * (note this exception is not guaranteed to be thrown, false may be returned instead)
	 */
	boolean hasActivePunishment(PunishmentType type);

}

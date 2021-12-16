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

package space.arim.libertybans.api.punish;

import java.time.Instant;

import space.arim.omnibus.util.concurrent.ReactionStage;

/**
 * A full punishment, identifiable by its ID. <br>
 * <br>
 * See {@link space.arim.libertybans.api.punish} for a description of active and
 * historical punishments.
 * 
 * @author A248
 *
 */
public interface Punishment extends PunishmentBase {

	/**
	 * A useful constant for the {@code Instant} representing a permanent punishment's
	 * end date. See {@link #getEndDate()}. <br>
	 * <br>
	 * This constant is provided for readability purposes, but it is interchangeable
	 * with {@link Instant#MAX}.
	 *
	 */
	Instant PERMANENT_END_DATE = Instant.MAX;

	/**
	 * Gets the unique ID of this punishment
	 * 
	 * @return the ID of the punishment
	 * @deprecated Use {@link #getIdentifier()}, as punishment IDs now use {@code long}
	 */
	@Deprecated
	int getID();

	/**
	 * Gets the unique ID of this punishment
	 *
	 * @return the ID of the punishment
	 */
	long getIdentifier();

	/**
	 * Gets the start time of the punishment
	 * 
	 * @return the state date
	 */
	Instant getStartDate();

	/**
	 * Gets the start date of the punishment, in unix seconds
	 * 
	 * @return the start date seconds
	 */
	default long getStartDateSeconds() {
		return getStartDate().getEpochSecond();
	}

	/**
	 * Gets the end time of the punishment. {@link #PERMANENT_END_DATE} is used for a
	 * permanent punishment
	 * 
	 * @return the end date
	 */
	Instant getEndDate();

	/**
	 * Gets the end time of the punishment, in unix seconds. {@code 0} is used for a
	 * permanent punishment
	 * 
	 * @return the end date seconds
	 */
	default long getEndDateSeconds() {
		Instant endDate = getEndDate();
		return endDate.equals(PERMANENT_END_DATE) ? 0L : endDate.getEpochSecond();
	}

	/**
	 * Convenience method to determine if this punishment is permanent
	 * 
	 * @return true if this punishment is permanent, false otherwise
	 */
	default boolean isPermanent() {
		return PERMANENT_END_DATE.equals(getEndDate());
	}

	/**
	 * Convenience method to determine if this punishment is temporary (opposite of
	 * {@link #isPermanent()})
	 * 
	 * @return true if this punishment is temporary, false otherwise
	 */
	default boolean isTemporary() {
		return !isPermanent();
	}

	/**
	 * Convenience method to determine if this punishment has expired. If permanent,
	 * will always be {@code false}
	 * 
	 * @return true if this is a temporary punishment which has expired
	 */
	default boolean isExpired() {
		if (isPermanent()) {
			return false;
		}
		return Instant.now().compareTo(getEndDate()) > 0;
	}

	/**
	 * Enforces this punishment. <br>
	 * <br>
	 * For bans and mutes, this will kick players matching the punishment's victim.
	 * For mutes and warn, the players will be sent a warning message. <br>
	 * Additionally for mutes, the player will be unable to chat (depending on the
	 * implementation) until the mute cache expires, at which point the database is
	 * re-queried for a mute.
	 * 
	 * @return a future completed when enforcement has been conducted
	 */
	ReactionStage<?> enforcePunishment();

	/**
	 * Undoes and "unenforces" this punishment assuming it active and in the
	 * database. <br>
	 * If the punishment was active then was removed, the future yields
	 * {@code true}, else {@code false}. <br>
	 * <br>
	 * Unenforcement implies purging of this punishment from any local caches.
	 * 
	 * @return a future which yields {@code true} if this punishment existed and was
	 *         removed and unenforced, {@code false} otherwise
	 */
	ReactionStage<Boolean> undoPunishment();

	/**
	 * Undoes this punishment assuming it is in the database. <br>
	 * If the punishment was active then was removed, the future yields
	 * {@code true}, else {@code false}. <br>
	 * <br>
	 * Most callers will want to use {@link #undoPunishment()} instead, which has
	 * the added effect of "unenforcing" the punishment. Unenforcement implies
	 * purging of this punishment from any local caches.
	 * 
	 * @return a future which yields {@code true} if this punishment existed and was
	 *         removed, {@code false} otherwise
	 */
	ReactionStage<Boolean> undoPunishmentWithoutUnenforcement();

	/**
	 * "Unenforces" this punishment. <br>
	 * <br>
	 * This will update local punishment caches, such as the mute cache.
	 * 
	 * @return a future completed when unenforcement has been conducted
	 */
	ReactionStage<?> unenforcePunishment();

	/**
	 * Whether this punishment is equal to another. The other punishment must
	 * represent the same punishment stored in the database. <br>
	 * <br>
	 * Implementations need only check {@link #getIdentifier()} since IDs must always be
	 * unique.
	 * 
	 * @param object the other object
	 * @return true if the objects are equal, false otherwise
	 */
	@Override
	boolean equals(Object object);

}

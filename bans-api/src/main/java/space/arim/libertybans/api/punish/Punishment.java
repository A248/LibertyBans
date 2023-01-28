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

import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A full punishment, identifiable by its ID. <br>
 * <br>
 * See {@link space.arim.libertybans.api.punish} for a description of active and
 * historical punishments.
 * 
 * @author A248
 *
 */
public interface Punishment extends PunishmentBase, EnforcementOptionsFactory {

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
	 * @deprecated Use {@link #getIdentifier()} instead
	 */
	@Deprecated
	default int getID() {
		return (int) getIdentifier();
	}

	/**
	 * Gets the unique ID of this punishment
	 *
	 * @return the ID of the punishment
	 */
	long getIdentifier();

	/**
	 * Gets the start time of the punishment, which is when the punishment was created
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
	 * Convenience method to determine if this punishment has expired. If permanent,
	 * will always be {@code false}
	 *
	 * @param clock the clock to use
	 * @return true if this is a temporary punishment which has expired
	 */
	default boolean isExpired(Clock clock) {
		if (isPermanent()) {
			return false;
		}
		return clock.instant().compareTo(getEndDate()) > 0;
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
	default ReactionStage<?> enforcePunishment() {
		return enforcePunishment(enforcementOptionsBuilder().build());
	}

	/**
	 * Enforces this punishment according to the given options. <br>
	 * <br>
	 * For bans and mutes, this will kick players matching the punishment's victim.
	 * For mutes and warn, the players will be sent a warning message. <br>
	 * Additionally for mutes, the player will be unable to chat (depending on the
	 * implementation) until the mute cache expires, at which point the database is
	 * re-queried for a mute.
	 *
	 * @param enforcementOptions the enforcement options. Can be used to disable unenforcement entirely
	 * @return a future completed when enforcement has been conducted
	 */
	ReactionStage<?> enforcePunishment(EnforcementOptions enforcementOptions);

	/**
	 * Undoes and "unenforces" this punishment assuming it active and in the
	 * database. <br>
	 * If the punishment was active then was removed, the future yields
	 * {@code true}, else {@code false}. <br>
	 * <br>
	 * Unenforcement implies purging of this punishment from any local caches.
	 * Additionally, any relevant broadcast messages will be sent to players.
	 * 
	 * @return a future which yields {@code true} if this punishment existed and was
	 *         removed and unenforced, {@code false} otherwise
	 */
	default ReactionStage<Boolean> undoPunishment() {
		return undoPunishment(enforcementOptionsBuilder().build());
	}

	/**
	 * Undoes and "unenforces" this punishment assuming it active and in the
	 * database. <br>
	 * If the punishment was active then was removed, the future yields
	 * {@code true}, else {@code false}. <br>
	 * <br>
	 * Unenforcement implies purging of this punishment from any local caches.
	 * Additionally, any relevant broadcast messages will be sent to players.
	 *
	 * @param enforcementOptions the enforcement options. Can be used to disable unenforcement entirely
	 * @return a future which yields {@code true} if this punishment existed and was
	 *         removed and unenforced, {@code false} otherwise
	 */
	ReactionStage<Boolean> undoPunishment(EnforcementOptions enforcementOptions);

	/**
	 * "Unenforces" this punishment. <br>
	 * <br>
	 * Unenforcement may mean purging caches such as the mute cache.
	 *
	 * @return a future completed when unenforcement has been conducted
	 */
	default ReactionStage<?> unenforcePunishment() {
		return unenforcePunishment(enforcementOptionsBuilder().build());
	}

	/**
	 * "Unenforces" this punishment according to the given options. <br>
	 * <br>
	 * Unenforcement may mean purging caches such as the mute cache.
	 *
	 * @param enforcementOptions the enforcement options. Can be used to disable unenforcement entirely
	 * @return a future completed when unenforcement has been conducted
	 */
	ReactionStage<?> unenforcePunishment(EnforcementOptions enforcementOptions);

	/**
	 * Modifies this punishment and yields the new instance. <br>
	 * <br>
	 * This punishment instance is invalidated by use of this method. The new punishment
	 * returned should be used as soon as it becomes available.
	 *
	 * @param editorConsumer the actions applied to modify this punishment. Note that the editor
	 *                          is made invalid as soon as the consumer exits scope.
	 * @return the new punishment after changes have been propagated to the database, or
	 * an empty optional if the punishment has since been expunged
	 */
	ReactionStage<Optional<Punishment>> modifyPunishment(Consumer<PunishmentEditor> editorConsumer);

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

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

import java.util.List;
import java.util.Optional;

import space.arim.omnibus.util.concurrent.ReactionStage;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;

/**
 * An order to undo a punishment by some specific details. Obtained from
 * {@link PunishmentRevoker}. <br>
 * <br>
 * See {@link space.arim.libertybans.api.punish} for a description of active and
 * historical punishments.
 * 
 * @author A248
 *
 */
public interface RevocationOrder extends EnforcementOptionsFactory {

	/**
	 * Gets the ID of the punishment which will be revoked, or none if no ID is
	 * known
	 * 
	 * @return the ID of the punishment to be revoked if there is one
	 */
	Optional<Long> getID();

	/**
	 * Gets the punishment type of the punishment which will be revoked, or none if
	 * the type is not known
	 * 
	 * @return the type of the punishment to be revoked if there is one
	 */
	Optional<PunishmentType> getType();

	/**
	 * Gets the victims, one of whose punishment will be revoked, or none if the list of victims
	 * does not exist
	 *
	 * @return the victims if specified
	 */
	Optional<List<Victim>> getVictims();

	/**
	 * Revokes the punishment matching this revocation order, and "unenforces" it.
	 * <br>
	 * <br>
	 * If no active punishment in the database matched this revocation order, the
	 * future yields {@code false}. <br>
	 * <br>
	 * Unenforcement implies purging of this punishment from any local caches.
	 * 
	 * @return a future which yields {@code true} if the punishment existed and has
	 *         been revoked, {@code false} otherwise
	 */
	default ReactionStage<Boolean> undoPunishment() {
		return undoPunishment(enforcementOptionsBuilder().build());
	}

	/**
	 * Revokes the punishment matching this revocation order, and "unenforces" it according to
	 * the given options.
	 * <br>
	 * <br>
	 * If no active punishment in the database matched this revocation order, the
	 * future yields {@code false}. <br>
	 * <br>
	 * Unenforcement implies purging of this punishment from any local caches.
	 *
	 * @param enforcementOptions the enforcement options. Can be used to disable unenforcement entirely
	 * @return a future which yields {@code true} if the punishment existed and has
	 *         been revoked, {@code false} otherwise
	 */
	ReactionStage<Boolean> undoPunishment(EnforcementOptions enforcementOptions);

	/**
	 * Revokes the punishment matching this revocation order, "unenforces" it, and
	 * gets the punishment revoked. <br>
	 * <br>
	 * If no active punishment in the database matched this revocation order, the
	 * future yields an empty optional. <br>
	 * <br>
	 * Unenforcement implies purging of this punishment from any local caches.
	 * 
	 * @return a future which yields the punishment if it existed and was revoked,
	 *         nothing otherwise
	 */
	default ReactionStage<Optional<Punishment>> undoAndGetPunishment() {
		return undoAndGetPunishment(enforcementOptionsBuilder().build());
	}

	/**
	 * Revokes the punishment matching this revocation order, "unenforces" it according to
	 * the given options, and gets the punishment revoked. <br>
	 * <br>
	 * If no active punishment in the database matched this revocation order, the
	 * future yields an empty optional. <br>
	 * <br>
	 * Unenforcement implies purging of this punishment from any local caches.
	 *
	 * @param enforcementOptions the enforcement options. Can be used to disable unenforcement entirely
	 * @return a future which yields the punishment if it existed and was revoked,
	 *         nothing otherwise
	 */
	ReactionStage<Optional<Punishment>> undoAndGetPunishment(EnforcementOptions enforcementOptions);

}

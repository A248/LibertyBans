/* 
 * LibertyBans-api
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-api. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.api.revoke;

import java.util.Optional;

import space.arim.omnibus.util.concurrent.ReactionStage;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;

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
public interface RevocationOrder {

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
	 * Gets the victim whose punishment will be revoked, or none if the victim is
	 * not known
	 * 
	 * @return the victim if there is one
	 */
	Optional<Victim> getVictim();

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
	ReactionStage<Boolean> undoPunishment();

	/**
	 * Revokes the punishment matching this revocation order. <br>
	 * <br>
	 * If no active punishment in the database matched this revocation order, the
	 * future yields {@code false}. <br>
	 * <br>
	 * Most callers will want to use {@link #undoPunishment()} instead, which has
	 * the added effect of "unenforcing" the punishment. Unenforcement implies
	 * purging of this punishment from any local caches.
	 * 
	 * @return a future which yields {@code true} if the punishment existed and has
	 *         been revoked, {@code false} otherwise
	 */
	ReactionStage<Boolean> undoPunishmentWithoutUnenforcement();

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
	ReactionStage<Optional<Punishment>> undoAndGetPunishment();

	/**
	 * Revokes the punishment matching this revocation order, and gets the
	 * punishment revoked. <br>
	 * <br>
	 * If no active punishment in the database matched this revocation order, the
	 * future yields an empty optional. <br>
	 * <br>
	 * Most callers will want to use {@link #undoAndGetPunishment()} instead, which
	 * has the added effect of "unenforcing" the punishment. Unenforcement implies
	 * purging of this punishment from any local caches.
	 * 
	 * @return a future which yields the punishment if it existed and was revoked,
	 *         nothing otherwise
	 */
	ReactionStage<Optional<Punishment>> undoAndGetPunishmentWithoutUnenforcement();

}

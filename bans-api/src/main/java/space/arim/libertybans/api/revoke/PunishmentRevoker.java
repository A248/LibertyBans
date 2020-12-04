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

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;

/**
 * Agent of removing active punishments in accordance with constraints. For
 * example, one victim cannot have more than 1 ban, therefore a ban may be
 * undone if the victim is known. <br>
 * <br>
 * Only active punishments can be undone. Historical punishments cannot be
 * removed. See {@link space.arim.libertybans.api.punish} for a description of
 * active and historical punishments. <br>
 * <br>
 * Note that {@link Punishment#undoPunishment()} should be used instead of a
 * revocation order if a punishment instance is already obtained.
 * 
 * @author A248
 *
 */
public interface PunishmentRevoker {

	/**
	 * Gets a {@link RevocationOrder} to undo a punishment by its ID and type.
	 * 
	 * @param id   the id of the punishment to undo
	 * @param type the type of the punishment to undo
	 * @return a revocation order using the ID and type
	 */
	RevocationOrder revokeByIdAndType(int id, PunishmentType type);

	/**
	 * Gets a {@link RevocationOrder} to undo a punishment according to its ID. <br>
	 * <br>
	 * When the punishment type is known,
	 * {@link #revokeByIdAndType(int, PunishmentType)} should be used.
	 * 
	 * @param id the id of the punishment to undo
	 * @return a revocation order using the ID
	 */
	RevocationOrder revokeById(int id);

	/**
	 * Gets a {@link RevocationOrder} to undo a punishment by its type and victim.
	 * <b>This may only be used for singular punishments (bans and mutes),</b> since
	 * it relies on the fact that a single victim cannot have more than 1 such
	 * punishment.
	 * 
	 * @param type   the punishment type, must be singular per
	 *               {@link PunishmentType#isSingular()}
	 * @param victim the victim whose punishment to undo
	 * @return a revocation order using the type and victim
	 * @throws IllegalArgumentException if {@code type} is not singular (BAN or
	 *                                  MUTE)
	 */
	RevocationOrder revokeByTypeAndVictim(PunishmentType type, Victim victim);

}

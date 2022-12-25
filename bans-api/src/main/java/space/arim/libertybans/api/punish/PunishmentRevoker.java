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

package space.arim.libertybans.api.punish;

import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;

import java.util.List;

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
	RevocationOrder revokeByIdAndType(long id, PunishmentType type);

	/**
	 * Gets a {@link RevocationOrder} to undo a punishment according to its ID. <br>
	 * <br>
	 * When the punishment type is known,
	 * {@link #revokeByIdAndType(long, PunishmentType)} should be used.
	 * 
	 * @param id the id of the punishment to undo
	 * @return a revocation order using the ID
	 */
	RevocationOrder revokeById(long id);

	/**
	 * Gets a {@link RevocationOrder} to undo a punishment by its type and victim.
	 * This is commonly be used for singular punishments (bans and mutes), relying on
	 * the fact that a single victim cannot have more than 1 such punishment. <br>
	 * <br>
	 * For non-singular punishments, or for a {@code CompositeVictim} with wildcards
	 * ({@link CompositeVictim#WILDCARD_UUID} or {@link CompositeVictim#WILDCARD_ADDRESS})
	 * multiple punishments may match the given criteria. In this case, it is explicitly
	 * unspecified which punishment is revoked.
	 * 
	 * @param type   the punishment type
	 * @param victim the victim whose punishment to undo
	 * @return a revocation order using the type and victim
	 */
	RevocationOrder revokeByTypeAndVictim(PunishmentType type, Victim victim);

	/**
	 * Gets a {@link RevocationOrder} to undo a punishment by its type and victim,
	 * trying multiple victims until one of them is found to be punished. <br>
	 * <br>
	 * The process for undoing the punishment is identical to
	 * {@link #revokeByTypeAndVictim(PunishmentType, Victim)}, except that multiple
	 * victims are tried: if the first victim is punished, that punishment is undone,
	 * if second victim is punished, <i>that</i> punishment is undone, et cetera.
	 *
	 * @param type   the punishment type
	 * @param victims the victims, one of whose punishments will be undone
	 * @return a revocation order using the type and victim
	 * @throws IllegalArgumentException if the list of victims is empty
	 */
	RevocationOrder revokeByTypeAndPossibleVictims(PunishmentType type, List<Victim> victims);

	/**
	 * Totally expunges a punishment according to its ID. This uses the most efficient means to completely
	 * remove a punishment from the database without any trace leftover. <b>Punishments deleted through
	 * this method are not recoverable.</b>
	 *
	 * @param id the id of the punishment to expunge
	 * @return an expunction order
	 */
	ExpunctionOrder expungePunishment(long id);

}

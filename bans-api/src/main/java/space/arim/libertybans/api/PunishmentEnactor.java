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
package space.arim.libertybans.api;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

/**
 * Guardkeeper of adding punishments while enforcing constraints and reporting such enforcement.
 * For example, one victim cannot have more than 1 ban.
 * 
 * @author A248
 *
 */
public interface PunishmentEnactor {
	
	/**
	 * Enacts a punishment, adding it to the database. <br>
	 * If the punishment type is a ban or mute, and there is already a ban or mute for the user,
	 * the future will yield {@code null}. <br>
	 * <br>
	 * Assuming the caller wants the punishment to be enforced, {@link PunishmentEnforcer#enforce(Punishment)}
	 * should be called after enaction.
	 * 
	 * @param draftPunishment the draft punishment to enact
	 * @return a centralised future which yields the punishment or {@code null} if there was a conflict
	 */
	CentralisedFuture<Punishment> enactPunishment(DraftPunishment draftPunishment);
	
	/**
	 * Undoes an existing punishment in the database. <br>
	 * If the punishment existed and was removed, the future yields {@code true}, else {@code false}.
	 * 
	 * @param punishment the punishment to undo
	 * @return a centralised future which yields {@code true} if the punishment existing and was removed, {@code false} otherwise
	 */
	CentralisedFuture<Boolean> undoPunishment(Punishment punishment);
	
	/**
	 * Undoes a punishment according to its ID. <br>
	 * If the punishment with the ID existed and was removed, the future yields {@code true}, else {@code false}. <br>
	 * <br>
	 * <b>This operation may be less efficient than {@link #undoPunishment(Punishment)}. When a full {@code Punishment}
	 * is known, the former method should be used instead.</b>
	 * 
	 * @param id the id of the punishment to undo
	 * @return a centralised future which yields {@code true} if the punishment existing and was removed, {@code false} otherwise
	 */
	CentralisedFuture<Boolean> undoPunishmentById(int id);
	
	/**
	 * Undoes a single punishment by its type and victim. <b>This may only be used for bans and mutes,</b>
	 * since it relies on the fact that a single victim cannot have more than 1 ban or mute.
	 * 
	 * @param type the punishment type, must be either BAN or MUTE
	 * @param victim the victim whose punishment to undo
	 * @return a centralised future which yields {@code true} if the punishment existing and was removed, {@code false} otherwise
	 * @throws IllegalArgumentException if {@code type} is not BAN or MUTE
	 */
	CentralisedFuture<Boolean> undoPunishmentByTypeAndVictim(PunishmentType type, Victim victim);

}

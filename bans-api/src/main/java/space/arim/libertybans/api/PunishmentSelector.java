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

import java.util.Set;
import java.util.UUID;

import space.arim.universal.util.concurrent.CentralisedFuture;

/**
 * A manager for selecting punishments with specific details from the database. This,
 * along with {@link PunishmentSelection}, essentially provide an API for efficient
 * database queries.
 * 
 * @author A248
 *
 */
public interface PunishmentSelector {

	/**
	 * Gets the first punishment matching the given punishment selection, i.e. with the specified details. <br>
	 * <br>
	 * A punishment is said to match if it meets ALL of the following: <br>
	 * Its type matches that of the selection, or the selection's type is unspecified (null) <br>
	 * Its victim matches that of the selection, or the selection's victim is unspecified (null) <br>
	 * Its operator matches that of the selection, or the selection's operator is unspecified (null) <br>
	 * Its scope matches that of the selection, or the selection's scope is unspecified (null)
	 * 
	 * @param selection the punishment selection whose details to match against
	 * @return a future which yields the first punishment matching the selection, or {@code null} if none matched
	 */
	CentralisedFuture<Punishment> getFirstSpecificPunishment(PunishmentSelection selection);
	
	/**
	 * Gets all punishments matching the given punishment selection, i.e. with the specified details. <br>
	 * <br>
	 * A punishment is said to match if it meets ALL of the following: <br>
	 * Its type matches that of the selection, or the selection's type is unspecified (null) <br>
	 * Its victim matches that of the selection, or the selection's victim is unspecified (null) <br>
	 * Its operator matches that of the selection, or the selection's operator is unspecified (null) <br>
	 * Its scope matches that of the selection, or the selection's scope is unspecified (null)
	 * 
	 * @param selection the punishment selection whose details to match against
	 * @return a future which yields all punishments matching the selection, or an empty set if none matched
	 */
	CentralisedFuture<Set<Punishment>> getSpecificPunishments(PunishmentSelection selection);
	
	/**
	 * Gets all punishments regarding a specific {@link Victim}
	 * 
	 * @param victim the victim
	 * @return a future which yields all punishments applying to the victim, or an empty set
	 */
	CentralisedFuture<Set<Punishment>> getHistoryForVictim(Victim victim);
	
	/**
	 * Gets all active punishments of a certain punishment type. <br>
	 * The punishment must not be expired.
	 * 
	 * @param type the punishment type
	 * @return a future which yields all applicable punishments matching the type, or an empty set
	 */
	CentralisedFuture<Set<Punishment>> getActivePunishmentsForType(PunishmentType type);

	/**
	 * Gets the first punishment, of a certain type, which is <i>applicable</i> to a UUID and IP address,
	 * where the UUID and address typically represent from connected player. <br>
	 * <br>
	 * Applicability is synonymous with enforceability. For example, if a ban would prevent a player from joining
	 * the server, the ban is said to be applicable to the player's UUID and IP. It may be, the player's IP is
	 * banned, the player's UUID is banned, or the player has played on a banned IP and that IP is banned while strict
	 * address enforcement is enabled. That is, this method takes into account configuration options related to enforcement.
	 * 
	 * @param uuid the UUID
	 * @param address the IP address
	 * @param type the punishment type
	 * @return a future which yields the first applicable punishment for the id and address or {@code null}
	 */
	CentralisedFuture<Punishment> getApplicablePunishment(UUID uuid, byte[] address, PunishmentType type);
	
	/**
	 * Gets a cached mute for an online player, including the player's UUID and address. <br>
	 * The mute will be <i>applicable</i> to the combination of UUID and address, see
	 * {@link #getApplicablePunishment(UUID, byte[], PunishmentType)} for a description of applicability. <br>
	 * <br>
	 * If the mute is cached, a completed future is returned. Else, an in-progress future is returned.
	 * 
	 * @param uuid the player's uuid
	 * @param address the player's current address
	 * @return a future yielding either a cached value or the queried mute
	 */
	CentralisedFuture<Punishment> getCachedMute(UUID uuid, byte[] address);
	
}

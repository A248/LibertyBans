/*
 * LibertyBans
 * Copyright © 2024 Anand Beh
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

package space.arim.libertybans.api.select;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;

import space.arim.omnibus.util.concurrent.ReactionStage;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;

/**
 * A starting point for selecting punishments with specific details from the database.
 * This provides a magnificent API for efficient database queries which is able to reach
 * astounding and unimaginable heights through dynamic query construction. <br>
 * <br>
 * It is possible to select active and historical punishments. See
 * {@link space.arim.libertybans.api.punish} for a description of active and
 * historical punishments.
 * 
 * @author A248
 *
 */
public interface PunishmentSelector {

	/**
	 * Begins creating a selection order to retrieve punishments. This may be used to retrieve punishments
	 * for a certain victim, group of victims, or all victims except a list of excluded.
	 * 
	 * @return a selection order builder
	 */
	SelectionOrderBuilder selectionBuilder();

	/**
	 * Begins creating a selection to retrieve punishments applicable to a certain user
	 * represented by a UUID and current IP address. <br>
	 * <br>
	 * Applicability is synonymous with enforceability, taking into account address strictness.
	 * For example, if a ban would prevent a player from joining the server, the ban is said to be applicable to
	 * the player's UUID and IP. It may be, the player's IP is banned, the player's UUID is banned,
	 * or the player has played on a banned IP and that IP is banned  while strict address enforcement is enabled. <br>
	 * <br>
	 * By default, the server's configured address strictness is used, but this may be changed if desired.
	 *
	 * @param uuid the uuid of the user for whom to select applicable punishments
	 * @param address the current or most recent address of the same user
	 * @return a selection by applicability builder
	 */
	SelectionByApplicabilityBuilder selectionByApplicabilityBuilder(UUID uuid, NetworkAddress address);

	/**
	 * Begins creating a selection to retrieve punishments applicable to a certain user
	 * represented by a UUID and current IP address. <br>
	 * <br>
	 * Applicability is synonymous with enforceability, taking into account address strictness.
	 * For example, if a ban would prevent a player from joining the server, the ban is said to be applicable to
	 * the player's UUID and IP. It may be, the player's IP is banned, the player's UUID is banned,
	 * or the player has played on a banned IP and that IP is banned  while strict address enforcement is enabled. <br>
	 * <br>
	 * For maximum accuracy, the UUID and IP address pair should be taken from an actual user who has logged on before.
	 * Due to specifics in terms of how applicability is computed, some punishments
	 * <br>
	 * By default, the server's configured address strictness is used, but this may be changed if desired.
	 *
	 * @param uuid the uuid of the user for whom to select applicable punishments
	 * @param address the current or most recent address of the same user
	 * @return a selection by applicability builder
	 */
	default SelectionByApplicabilityBuilder selectionByApplicabilityBuilder(UUID uuid, InetAddress address) {
		return selectionByApplicabilityBuilder(uuid, NetworkAddress.of(address));
	}

	/**
	 * Gets an active punishment matching a specific ID, if a punishment with such
	 * ID exists. <br>
	 * <br>
	 * When the type of the punishment is known,
	 * {@link #getActivePunishmentByIdAndType(long, PunishmentType)} should be
	 * preferred.
	 * 
	 * @param id the punishment ID
	 * @return a future which yields the active punishment with the ID, if there is
	 *         one
	 */
	ReactionStage<Optional<Punishment>> getActivePunishmentById(long id);

	/**
	 * Gets an active punishment matching a specific ID and type, if one exists with
	 * matching type and ID. <br>
	 * <br>
	 * Unlike {@link #getActivePunishmentById(long)}, this method may be more
	 * efficient when the type of the punishment is known beforehand.
	 * 
	 * @param id   the punishment ID
	 * @param type the punishment type
	 * @return a future which yields the active punishment with the ID and type, if
	 *         there is one
	 */
	ReactionStage<Optional<Punishment>> getActivePunishmentByIdAndType(long id, PunishmentType type);

	/**
	 * Gets a historical punishment matching a specific ID, if a punishment with
	 * such ID exists.
	 * 
	 * @param id the punishment ID
	 * @return a future which yields the historical punishment with the ID, if there
	 *         is one
	 */
	ReactionStage<Optional<Punishment>> getHistoricalPunishmentById(long id);

	/**
	 * Gets a historical punishment matching a specific ID and type, if a punishment with
	 * such ID  and typeexists.
	 *
	 * @param id the punishment ID
	 * @param type the punishment type
	 * @return a future which yields the historical punishment with the ID, if there
	 *         is one
	 */
	ReactionStage<Optional<Punishment>> getHistoricalPunishmentByIdAndType(long id, PunishmentType type);

	/**
	 * Gets the first punishment, of a certain type, which is <i>applicable</i> to a UUID and IP address,
	 * where the UUID and address typically represent a connected player. <br>
	 * <br>
	 * See {@link #selectionByApplicabilityBuilder(UUID, NetworkAddress)} for a description of applicability. <br>
	 * <br>
	 * If multiple punishments are applicable, the one with the latest end date is returned.
	 * 
	 * @param uuid    the player's UUID
	 * @param address the player's current address
	 * @param type    the punishment type
	 * @return a future which yields the first applicable punishment for the id and address, if there is one
	 */
	default ReactionStage<Optional<Punishment>> getApplicablePunishment(UUID uuid, NetworkAddress address, PunishmentType type) {
		return selectionByApplicabilityBuilder(uuid, address)
				.type(type)
				.build()
				.getFirstSpecificPunishment(SortPunishments.LATEST_END_DATE_FIRST);
	}

	/**
	 * Gets a cached mute for an online player, including the player's UUID and
	 * address. <br>
	 * The mute will be <i>applicable</i> to the combination of UUID and address,
	 * see {@link #getApplicablePunishment(UUID, NetworkAddress, PunishmentType)}
	 * for a description of applicability. <br>
	 * <br>
	 * If the mute (or its absence) is cached, a completed future is returned. On the certain server
	 * platforms where chat occurs on the main thread, it is guaranteed a cached result is available.
	 * 
	 * @param uuid    the player's uuid
	 * @param address the player's current address
	 * @return a future yielding the player's applicable mute if present
	 */
	ReactionStage<Optional<Punishment>> getCachedMute(UUID uuid, NetworkAddress address);

}

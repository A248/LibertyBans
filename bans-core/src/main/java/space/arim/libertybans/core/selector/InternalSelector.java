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

package space.arim.libertybans.core.selector;

import java.util.UUID;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.PunishmentSelector;

public interface InternalSelector extends PunishmentSelector {

	/**
	 * Checks a player connection's in a single connection query, enforcing any applicable bans
	 * 
	 * @param uuid the player uuidField
	 * @param name the player name
	 * @param address the player address
	 * @return a future which yields the ban itself, or null if there is none
	 */
	CentralisedFuture<Punishment> executeAndCheckConnection(UUID uuid, String name, NetworkAddress address);

	CentralisedFuture<Punishment> getApplicableMute(UUID uuid, NetworkAddress address);

}

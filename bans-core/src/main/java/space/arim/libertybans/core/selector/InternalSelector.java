/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

import net.kyori.adventure.text.Component;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.api.select.SelectionOrderBuilder;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.util.Set;
import java.util.UUID;

public interface InternalSelector extends PunishmentSelector {

	/**
	 * Creates a selection builder using the given selection resources
	 *
	 * @param resources the selection resources
	 * @return a selection order builder
	 */
	SelectionOrderBuilder selectionBuilder(SelectionResources resources);

	/**
	 * Checks a player connection's in a single connection query, enforcing any applicable bans,
	 * connection limits, and dealing out alt checks
	 * 
	 * @param uuid the player uuid
	 * @param name the player name
	 * @param address the player address
	 * @param scopes the server scopes to include in the selection query
	 * @return a future which yields the denial message, or null if there is none
	 */
	CentralisedFuture<Component> executeAndCheckConnection(UUID uuid, String name, NetworkAddress address,
														   Set<ServerScope> scopes);

}

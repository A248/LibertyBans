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

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.punish.PunishmentCreator;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

public record SelectionResources(FactoryOfTheFuture futuresFactory,
								 Provider<QueryExecutor> dbProvider,
								 InternalScopeManager scopeManager,
								 PunishmentCreator creator,
								 Time time) {

	@Inject
	public SelectionResources {
	}

}

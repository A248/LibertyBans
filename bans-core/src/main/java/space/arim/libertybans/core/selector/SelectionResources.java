/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

/*
  This class used to be a record, but when we compile on JDK 21 with --release, it breaks generic information.
  See JDK-9078340 for details. Keep this class a non-record until that issue is resolved.
 */
public final class SelectionResources {

	private final FactoryOfTheFuture futuresFactory;
	private final Provider<QueryExecutor> dbProvider;
	private final InternalScopeManager scopeManager;
	private final PunishmentCreator creator;
	private final Time time;

	@Inject
    public SelectionResources(FactoryOfTheFuture futuresFactory, Provider<QueryExecutor> dbProvider, InternalScopeManager scopeManager, PunishmentCreator creator, Time time) {
        this.futuresFactory = futuresFactory;
        this.dbProvider = dbProvider;
        this.scopeManager = scopeManager;
        this.creator = creator;
        this.time = time;
    }

	public FactoryOfTheFuture futuresFactory() {
		return futuresFactory;
	}

	public Provider<QueryExecutor> dbProvider() {
		return dbProvider;
	}

	public InternalScopeManager scopeManager() {
		return scopeManager;
	}

	public PunishmentCreator creator() {
		return creator;
	}

	public Time time() {
		return time;
	}

}

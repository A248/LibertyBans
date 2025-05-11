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

package space.arim.libertybans.it.test.database;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.it.InjectionInvocationContextProvider;

@ExtendWith(InjectionInvocationContextProvider.class)
public class PaginationMultiKeyIT {

    private static final int PER_PAGE = 5;

    private final Provider<QueryExecutor> queryExecutor;

    @Inject
    public PaginationMultiKeyIT(Provider<QueryExecutor> queryExecutor) {
        this.queryExecutor = queryExecutor;
    }


}

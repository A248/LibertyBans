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

package space.arim.libertybans.core.database.execute;

import org.jooq.DSLContext;

public interface SQLFunction<R> {

	default boolean isReadOnly() {
		return false;
	}

	R obtain(DSLContext context) throws RuntimeException;

	static <R> SQLFunction<R> readOnly(SQLFunction<R> command) {
		return new SQLFunction<>() {

			@Override
			public boolean isReadOnly() {
				return true;
			}

			@Override
			public R obtain(DSLContext context) throws RuntimeException {
				return command.obtain(context);
			}
		};
	}
}

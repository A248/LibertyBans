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

package space.arim.libertybans.core.database.jooq;

import org.jetbrains.annotations.NotNull;
import org.jooq.Converter;

abstract class OrdinalEnumConverter<E extends Enum<E>> implements Converter<Short, E> {

	@Override
	public final E from(Short databaseObject) {
		return (databaseObject == null) ? null : toType().getEnumConstants()[databaseObject];
	}

	@Override
	public final Short to(E userObject) {
		return (userObject == null) ? null : (short) userObject.ordinal();
	}

	@Override
	public final @NotNull Class<Short> fromType() {
		return Short.class;
	}
}

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

import java.time.Instant;

public final class EndInstantConverter implements Converter<Long, Instant> {
	@Override
	public Instant from(Long databaseObject) {
		if (databaseObject == null) {
			return null;
		}
		long end = databaseObject;
		if (end == 0L) {
			return Instant.MAX;
		}
		return Instant.ofEpochSecond(end);
	}

	@Override
	public Long to(Instant userObject) {
		if (userObject == null) {
			return null;
		}
		if (userObject.equals(Instant.MAX)) {
			return 0L;
		}
		return userObject.getEpochSecond();
	}

	@Override
	public @NotNull Class<Long> fromType() {
		return Long.class;
	}

	@Override
	public @NotNull Class<Instant> toType() {
		return Instant.class;
	}
}

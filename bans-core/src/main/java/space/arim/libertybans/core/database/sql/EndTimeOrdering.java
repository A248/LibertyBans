/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.core.database.sql;

import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.util.Objects;

import static org.jooq.impl.DSL.inline;

/**
 * Implements ordering by the end time of a punishment
 */
public final class EndTimeOrdering {

	private final Field<Instant> endField;

	public EndTimeOrdering(Field<Instant> endField) {
		this.endField = Objects.requireNonNull(endField, "endField");
	}

	public EndTimeOrdering(PunishmentFields punishmentFields) {
		this(punishmentFields.end());
	}

	/**
	 * Returns punishments which last for a longer period of time first. <br>
	 * <br>
	 * Permanent punishments, therefore, always come first.
	 *
	 * @return an ordering which places longer-lasting punishments first
	 */
	public OrderField<?> expiresLeastSoon() {
		// end = 0 defines a permanent punishment
		var end = endField.coerce(Long.class);
		return DSL.choose(end)
				.when(inline(0L), inline(Long.MAX_VALUE))
				.otherwise(end)
				.desc();
	}

}

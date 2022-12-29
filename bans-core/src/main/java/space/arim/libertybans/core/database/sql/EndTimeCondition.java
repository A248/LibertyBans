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

package space.arim.libertybans.core.database.sql;

import org.jooq.Condition;
import org.jooq.Field;

import java.time.Instant;
import java.util.Objects;

import static org.jooq.impl.DSL.inline;

public final class EndTimeCondition {

	private final Field<Instant> endField;

	public EndTimeCondition(Field<Instant> endField) {
		this.endField = Objects.requireNonNull(endField, "endField");
	}

	public EndTimeCondition(PunishmentFields punishmentFields) {
		this(punishmentFields.end());
	}

	public Condition isNotExpired(final Instant currentTime) {
		return endField.eq(inline(Instant.MAX)).or(endField.greaterThan(currentTime));
	}

	@Override
	public String toString() {
		return "EndTimeCondition{" +
				"endField=" + endField +
				'}';
	}
}

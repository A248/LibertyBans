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

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.Sequence;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.util.Objects;

public final class SequenceValue<R extends Number> {

	private final Sequence<R> sequence;
	private R lastValueForMySQL;

	public SequenceValue(Sequence<R> sequence) {
		this.sequence = Objects.requireNonNull(sequence, "sequence");
	}

	public Field<R> nextValue(DSLContext context) {
		if (context.family() == SQLDialect.MYSQL) {
			Table<?> emulationTable = DSL.table(sequence.getName());
			Field<R> valueField = DSL.field("value", sequence.getDataType());

			R sequenceValue = context
					.select(valueField)
					.from(emulationTable)
					.fetchSingle(valueField);
			context
					.update(emulationTable)
					.set(valueField, valueField.plus(1))
					.execute();
			lastValueForMySQL = sequenceValue;

			return DSL.val(sequenceValue, sequence.getDataType());
		} else {
			return sequence.nextval();
		}
	}

	public Field<R> lastValueInSession(DSLContext context) {
		if (context.family() == SQLDialect.MYSQL) {
			if (lastValueForMySQL == null) {
				throw new IllegalStateException("For MySQL, a value must be previously generated " +
						"with #nextValue before #lastValueInSession can be used");
			}
			return DSL.val(lastValueForMySQL, sequence.getDataType());
		} else {
			return sequence.currval();
		}
	}
}

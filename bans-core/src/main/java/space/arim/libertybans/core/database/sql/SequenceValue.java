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

	private Table<?> emulationTable() {
		return DSL.table(sequence.getName());
	}

	private Field<R> emulationTableValueField() {
		return DSL.field("value", sequence.getDataType());
	}

	public Field<R> nextValue(DSLContext context) {
		if (context.family() == SQLDialect.MYSQL) {
			Table<?> emulationTable = emulationTable();
			Field<R> valueField = emulationTableValueField();

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

	public void setValue(DSLContext context, R value) {
		switch (context.family()) {
		case MYSQL:
			context
					.update(emulationTable())
					.set(emulationTableValueField(), value)
					.execute();
			break;
		case HSQLDB:
			context
					.alterSequence(sequence)
					.restartWith(value)
					.execute();
			break;
		case MARIADB:
			context
					.query("SELECT SETVAL(" + sequence.getName() + ", " + value + ")")
					.execute();
			break;
		case POSTGRES:
			context
					.query("SELECT SETVAL('" + sequence.getName() + "', " + value + ")")
					.execute();
			break;
		default:
			throw new UnsupportedOperationException("Not supported: " + context.family());
		}
	}
}

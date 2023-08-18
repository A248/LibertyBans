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

package space.arim.libertybans.core.database.sql;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.Sequence;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.util.Objects;
import java.util.function.Consumer;

import static org.jooq.impl.DSL.val;

public class SequenceValue<R extends Number> {

	final DSLContext context;
	private final Sequence<R> sequence;
	private Field<R> lastValueForMySQL;

	public SequenceValue(DSLContext context, Sequence<R> sequence) {
		this.context = context;
		this.sequence = Objects.requireNonNull(sequence, "sequence");
	}

	private Table<?> emulationTable() {
		return DSL.table(sequence.getName());
	}

	private Field<R> emulationTableValueField() {
		return DSL.field("value", sequence.getDataType());
	}

	public Field<R> nextValue() {
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
			return (lastValueForMySQL = val(sequenceValue, sequence.getDataType()));
		} else {
			return sequence.nextval();
		}
	}

	public Field<R> lastValueInSession() {
		if (context.family() == SQLDialect.MYSQL) {
			if (lastValueForMySQL == null) {
				throw new IllegalStateException("For MySQL, a value must be previously generated " +
						"with #nextValue before #lastValueInSession can be used");
			}
			return lastValueForMySQL;
		} else {
			return sequence.currval();
		}
	}

	public void setValue(R value) {
		switch (context.family()) {
		case MYSQL -> context
				.update(emulationTable())
				.set(emulationTableValueField(), value)
				.execute();
		case HSQLDB -> context
				.alterSequence(sequence)
				.restartWith(value)
				.execute();
		case MARIADB -> context
				.query("SELECT SETVAL(" + sequence.getName() + ", " + value + ")")
				.execute();
		case POSTGRES -> context
				.query("SELECT SETVAL('" + sequence.getName() + "', " + value + ")")
				.execute();
		default -> throw new UnsupportedOperationException("Not supported: " + context.family());
		}
	}

	class RetrieveOrGenerate {

		private final Table<?> table;
		private final Field<R> sequenceValueField;
		private final Condition matchExisting;
		private final Consumer<Field<R>> insertNew;

		RetrieveOrGenerate(Table<?> table, Field<R> sequenceValueField,
						   Condition matchExisting, Consumer<Field<R>> insertNew) {
			this.table = table;
			this.sequenceValueField = sequenceValueField;
			this.matchExisting = matchExisting;
			this.insertNew = insertNew;
		}

		Field<R> execute() {
			R existingId = context
					.select(sequenceValueField)
					.from(table)
					.where(matchExisting)
					.fetchOne(sequenceValueField);
			if (existingId != null) {
				return val(existingId);
			}
			insertNew.accept(nextValue());
			return lastValueInSession();
		}

		R executeReified() {
			R existingId = context
					.select(sequenceValueField)
					.from(table)
					.where(matchExisting)
					.fetchOne(sequenceValueField);
			if (existingId != null) {
				return existingId;
			}
			insertNew.accept(nextValue());
			return context.select(lastValueInSession()).fetchSingle(sequenceValueField);
		}
	}

}

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

import org.jooq.SQLDialect;

import java.sql.SQLException;
import java.sql.Statement;

public final class SequenceDefinition<I extends Number> {

	private final String name;
	private final DataType<I> dataType;

	private SequenceDefinition(String name, DataType<I> dataType) {
		this.name = name;
		this.dataType = dataType;
	}

	public static SequenceDefinition<?> bigInteger(String name, long startValue) {
		// MariaDB's maximum permitted sequence is this number
		long maxValue = Long.MAX_VALUE - 1;
		return new SequenceDefinition<>(name, new LongDataType(startValue, 1L, maxValue));
	}

	public static SequenceDefinition<Integer> integer(String name, int startValue) {
		return new SequenceDefinition<>(name, new IntegerDataType(startValue, Integer.MIN_VALUE, Integer.MAX_VALUE));
	}

	public void defineUsing(Statement statement, SQLDialect dialect) throws SQLException {
		if (dialect == SQLDialect.MYSQL) {
			// MySQL does not support sequences, so we must emulate them using tables
			statement.execute(
					"""
							CREATE TABLE "libertybans_%NAME%" (
							  "value" %DATATYPE% NOT NULL,
							  "singleton" INT NOT NULL UNIQUE,
							  CONSTRAINT "libertybans_%NAME%_sequence_uniqueness" CHECK ("singleton" = 1)
							)
							"""
							.replace("%NAME%", name)
							.replace("%DATATYPE%", dataType.sql())
			);
			statement.execute(
					"INSERT INTO \"libertybans_%NAME%\" VALUES (%START_VALUE%, 1)"
							.replace("%NAME%", name)
							.replace("%START_VALUE%", dataType.startValue().toString())
			);
		} else {
			// HSQLDB, PostgreSQL, and CockroachDB use the standard AS <DATATYPE>
			String asDataType = switch (dialect) {
				case HSQLDB, POSTGRES -> "AS " + dataType.sql();
				case MARIADB -> "";
				default -> {
					throw new IllegalArgumentException("Unsupported database " + dialect);
				}
			};
			// MariaDB uses the non-standard NOCYCLE without a space
			String noCycle = dialect == SQLDialect.MARIADB ? "NOCYCLE" : "NO CYCLE";
			statement.execute(
					"""
							CREATE SEQUENCE "libertybans_%NAME%"
							 %AS_DATA_TYPE%
							 START WITH %START_VALUE%
							 MINVALUE %MIN_VALUE%
							 MAXVALUE %MAX_VALUE%
							 %NO_CYCLE%
							 """
							.replace("%NAME%", name)
							.replace("%AS_DATA_TYPE%", asDataType)
							.replace("%START_VALUE%", dataType.startValue().toString())
							.replace("%MIN_VALUE%", dataType.minValue().toString())
							.replace("%MAX_VALUE%", dataType.maxValue().toString())
							.replace("%NO_CYCLE%", noCycle)
			);
		}
	}

	interface DataType<I extends Number> {

		String sql();

		I startValue();

		I minValue();

		I maxValue();

	}

	record LongDataType(Long startValue, Long minValue, Long maxValue) implements DataType<Long> {
		@Override
		public String sql() {
			return "BIGINT";
		}
	}

	record IntegerDataType(Integer startValue, Integer minValue, Integer maxValue) implements DataType<Integer> {
		@Override
		public String sql() {
			return "INT";
		}
	}

}

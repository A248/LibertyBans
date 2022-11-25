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
import org.jooq.BindingSQLContext;
import org.jooq.Converter;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.core.database.sql.EmptyData;
import space.arim.omnibus.util.UUIDUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Similar in kind to {@link UUIDBinding} but provides the special handling relating to,
 * and optimizations arising from, {@link Operator}
 *
 */
public final class OperatorBinding extends BaseBinding<UUID, Operator> {

	private final UUIDBinding uuidBinding = new UUIDBinding();

	private static Operator binaryToOperator(byte[] operatorBytes) {
		if (Arrays.equals(operatorBytes, EmptyData.UUID_BYTES)) {
			return ConsoleOperator.INSTANCE;
		}
		return PlayerOperator.of(UUIDUtil.fromByteArray(operatorBytes));
	}

	private static byte[] operatorToBinary(Operator operator) {
		return switch (operator.getType()) {
			case PLAYER -> UUIDUtil.toByteArray(((PlayerOperator) operator).getUUID());
			case CONSOLE -> EmptyData.UUID_BYTES;
		};
	}

	public Operator uuidToOperator(UUID uuid) {
		if (uuid.equals(EmptyData.UUID)) {
			return ConsoleOperator.INSTANCE;
		}
		return PlayerOperator.of(uuid);
	}

	public UUID operatorToUuid(Operator operator) {
		return switch (operator.getType()) {
			case PLAYER -> ((PlayerOperator) operator).getUUID();
			case CONSOLE -> EmptyData.UUID;
		};
	}

	@Override
	public @NotNull Converter<UUID, Operator> converter() {
		return new Converter<>() {
			@Override
			public Operator from(UUID databaseObject) {
				return (databaseObject == null) ? null : uuidToOperator(databaseObject);
			}

			@Override
			public UUID to(Operator userObject) {
				return (userObject == null) ? null : operatorToUuid(userObject);
			}

			@Override
			public @NotNull Class<UUID> fromType() {
				return UUID.class;
			}

			@Override
			public @NotNull Class<Operator> toType() {
				return Operator.class;
			}
		};
	}

	@Override
	void set(SQLDialect dialect, PreparedStatement statement, int index, Operator value) throws SQLException {
		if (uuidBinding.supportsUUID(dialect)) {
			uuidBinding.set(dialect, statement, index, operatorToUuid(value));
		} else {
			statement.setBytes(index, operatorToBinary(value));
		}
	}

	@Override
	Operator get(SQLDialect dialect, ResultSet resultSet, int index) throws SQLException {
		if (uuidBinding.supportsUUID(dialect)) {
			return uuidToOperator(uuidBinding.get(dialect, resultSet, index));
		} else {
			return binaryToOperator(resultSet.getBytes(index));
		}
	}

	@Override
	Field<?> inline(SQLDialect dialect, Operator value) {
		if (uuidBinding.supportsUUID(dialect)) {
			return uuidBinding.inline(dialect, operatorToUuid(value));
		} else {
			return DSL.inline(operatorToBinary(value), SQLDataType.BINARY(16));
		}
	}

	@Override
	protected void sqlBind(BindingSQLContext<Operator> ctx) throws SQLException {
		if (uuidBinding.supportsUUID(ctx.dialect())) {
			uuidBinding.sqlBind(ctx.convert(converter()));
			return;
		}
		super.sqlBind(ctx);
	}
}

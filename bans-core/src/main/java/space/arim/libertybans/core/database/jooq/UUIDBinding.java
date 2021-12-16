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
import space.arim.omnibus.util.UUIDUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Function;

/**
 * This class is responsible for setting UUID values as binary values or strings,
 * depending on the SQL dialect. (PostgreSQL's UUID type cannot be set directly as binary)
 *
 */
public final class UUIDBinding extends BaseBinding<UUID, UUID> {

	@Override
	public @NotNull Converter<UUID, UUID> converter() {
		return Converter.of(UUID.class, UUID.class, Function.identity(), Function.identity());
	}

	boolean supportsUUID(SQLDialect dialect) {
		return dialect == SQLDialect.POSTGRES || dialect == SQLDialect.HSQLDB;
	}

	@Override
	void set(SQLDialect dialect, PreparedStatement statement, int index, UUID value) throws SQLException {
		if (supportsUUID(dialect)) {
			statement.setObject(index, value);
		} else {
			statement.setBytes(index, UUIDUtil.toByteArray(value));
		}
	}

	@Override
	UUID get(SQLDialect dialect, ResultSet resultSet, int index) throws SQLException {
		if (supportsUUID(dialect)) {
			return resultSet.getObject(index, UUID.class);
		} else {
			return UUIDUtil.fromByteArray(resultSet.getBytes(index));
		}
	}

	@Override
	Field<?> inline(SQLDialect dialect, UUID value) {
		if (supportsUUID(dialect)) {
			return DSL.inline(value, SQLDataType.UUID);
		} else {
			return DSL.inline(UUIDUtil.toByteArray(value), SQLDataType.BINARY(16));
		}
	}

	@Override
	protected void sqlBind(BindingSQLContext<UUID> ctx) throws SQLException {
		if (supportsUUID(ctx.dialect())) {
			ctx.render().sql("cast(" + ctx.variable() + " as uuid)");
			return;
		}
		super.sqlBind(ctx);
	}
}

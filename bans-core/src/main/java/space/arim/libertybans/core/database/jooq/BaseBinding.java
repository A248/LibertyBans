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

import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSQLContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.AbstractBinding;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

abstract class BaseBinding<T, U> extends AbstractBinding<T, U> {

	abstract void set(SQLDialect dialect, PreparedStatement statement, int index, U value) throws SQLException;

	abstract U get(SQLDialect dialect, ResultSet resultSet, int index) throws SQLException;

	abstract Field<?> inline(SQLDialect dialect, U value);

	@Override
	public final void set(BindingSetStatementContext<U> ctx) throws SQLException {
		set(ctx.family(), ctx.statement(), ctx.index(), ctx.value());
	}

	@Override
	public final void get(BindingGetResultSetContext<U> ctx) throws SQLException {
		U value = get(ctx.dialect(), ctx.resultSet(), ctx.index());
		ctx.value(value);
	}

	@Override
	protected final void sqlInline(BindingSQLContext<U> ctx) throws SQLException {
		Field<?> inlined = inline(ctx.family(), ctx.value());
		ctx.render().visit(inlined);
	}

}

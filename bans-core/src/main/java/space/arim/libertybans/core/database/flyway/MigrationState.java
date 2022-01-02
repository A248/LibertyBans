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

package space.arim.libertybans.core.database.flyway;

import org.flywaydb.core.api.ClassProvider;
import org.flywaydb.core.api.migration.JavaMigration;
import org.jooq.DSLContext;
import space.arim.libertybans.core.database.jooq.JooqContext;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

final class MigrationState {

	private final JooqContext jooqContext;

	MigrationState(JooqContext jooqContext) {
		this.jooqContext = Objects.requireNonNull(jooqContext, "jooqContext");
	}

	DSLContext createJooqContext(Connection connection) {
		return jooqContext.createContext(connection);
	}

	ClassProvider<JavaMigration> asClassProvider(List<Class<? extends JavaMigration>> classes) {
		return new FlywayClassProvider(classes);
	}

	private final class FlywayClassProvider implements ClassProvider<JavaMigration> {

		private final List<Class<? extends JavaMigration>> classes;

		private FlywayClassProvider(List<Class<? extends JavaMigration>> classes) {
			this.classes = List.copyOf(classes);
		}

		@Override
		public Collection<Class<? extends JavaMigration>> getClasses() {
			return classes;
		}

		MigrationState outer() {
			return MigrationState.this;
		}
	}

	static MigrationState retrieveState(org.flywaydb.core.api.migration.Context flywayContext) {
		var classProvider = flywayContext.getConfiguration().getJavaMigrationClassProvider();
		return ((FlywayClassProvider) classProvider).outer();
	}
}

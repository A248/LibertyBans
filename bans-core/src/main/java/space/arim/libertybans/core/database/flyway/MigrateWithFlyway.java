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

package space.arim.libertybans.core.database.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import space.arim.libertybans.core.database.Vendor;
import space.arim.libertybans.core.database.jooq.JooqContext;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

public final class MigrateWithFlyway {

	private final DataSource dataSource;
	private final Vendor vendor;

	public MigrateWithFlyway(DataSource dataSource, Vendor vendor) {
		this.dataSource = dataSource;
		this.vendor = vendor;
	}

	private static final String TABLE_PREFIX = "libertybans_";

	public void migrate(JooqContext jooqContext) throws MigrationFailedException {
		Flyway flyway = createFlyway(new MigrationState(jooqContext));
		try {
			if (Boolean.getBoolean("libertybans.database.flywayrepair")) {
				flyway.repair();
			}
			flyway.migrate();
		} catch (FlywayException ex) {
			throw new MigrationFailedException(ex);
		}
	}

	private Flyway createFlyway(MigrationState migrationState) {
		var classProvider = migrationState.asClassProvider(List.of(
				V1__Principle.class, V16__Complete_migration_from_08x.class,
				V31__Track_identifier_sequence.class, V34__Scope_identifier_sequence.class, V38__Scope_migration.class,
				R__Set_Revision.class
		));
		return Flyway
				.configure(getClass().getClassLoader())
				.dataSource(dataSource)
				// Configure tables and migrations
				.table(TABLE_PREFIX + "schema_history")
				.placeholders(Map.of(
						"tableprefix", TABLE_PREFIX,
						"generatedcolumnsuffix", vendor.getGeneratedColumnSuffix(),
						"extratableoptions", vendor.getExtraTableOptions(),
						"uuidtype", vendor.uuidType(),
						"inettype", vendor.inetType(),
						"arbitrarybinarytype", vendor.arbitraryBinaryType(),
						"alterviewstatement", vendor.alterViewStatement(),
						"zerosmallintliteral", vendor.zeroSmallintLiteral(),
						"migratescopestart", vendor.migrateScope()[0],
						"migratescopeend", vendor.migrateScope()[1]
				))
				.locations("classpath:database-migrations")
				// Override classpath scanning
				.javaMigrationClassProvider(classProvider)
				.validateMigrationNaming(true)
				// Allows usage with existing tables, i.e. from other software
				.baselineOnMigrate(true).baselineVersion("0.0")
				.load();
	}
}

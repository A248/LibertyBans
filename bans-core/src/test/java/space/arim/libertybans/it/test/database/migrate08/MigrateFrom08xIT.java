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

package space.arim.libertybans.it.test.database.migrate08;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.MappedTable;
import org.jooq.conf.RenderMapping;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.database.Vendor;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.database.jooq.JooqContext;
import space.arim.libertybans.core.punish.PunishmentCreator;
import space.arim.libertybans.core.service.SettableTime;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetVendor;
import space.arim.libertybans.it.ThrowawayInstance;
import space.arim.libertybans.it.util.RandomUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.jooq.impl.DSL.currentSchema;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static space.arim.libertybans.api.PunishmentType.BAN;
import static space.arim.libertybans.api.PunishmentType.KICK;
import static space.arim.libertybans.api.PunishmentType.MUTE;
import static space.arim.libertybans.api.PunishmentType.WARN;

@ExtendWith(InjectionInvocationContextProvider.class)
@ThrowawayInstance
public class MigrateFrom08xIT {

	private final BaseFoundation foundation;
	private final Provider<InternalDatabase> dbProvider;
	private final SettableTime time;
	private final PunishmentCreator creator;

	@Inject
	public MigrateFrom08xIT(BaseFoundation foundation, Provider<InternalDatabase> dbProvider,
							SettableTime time, PunishmentCreator creator) {
		this.foundation = foundation;
		this.dbProvider = dbProvider;
		this.time = time;
		this.creator = creator;
	}

	private DSLContext rewriteContext(DSLContext context) {
		/*
		Multiple requirements:
		1. Rewrite zeroeight_ to libertybans_
		2. Remap PUBLIC schema to current schema
		3. Enable renderSchema so that information_schema.tables is rendered properly
		*/

		// 1.
		List<MappedTable> mappedTables;
		{
			Pattern zeroEightTablePattern = Pattern.compile("^(zeroeight_)(.*?)$");
			String tableReplacement = "libertybans_$2";
			assert zeroEightTablePattern.matcher("zeroeight_names").replaceAll(tableReplacement).equals("libertybans_names");
			assert !zeroEightTablePattern.matcher("names").matches();

			Pattern oneZeroTablePattern = Pattern.compile("^((?!zeroeight_).)*$");
			String oneZeroTableReplacement = "libertybans_$0";
			assert oneZeroTablePattern.matcher("names").replaceAll(oneZeroTableReplacement).equals("libertybans_names");
			assert !oneZeroTablePattern.matcher("zeroeight_names").matches();

			mappedTables = List.of(
					new MappedTable()
							.withInputExpression(zeroEightTablePattern)
							.withOutput(tableReplacement),
					new MappedTable()
							.withInputExpression(oneZeroTablePattern)
							.withOutput(oneZeroTableReplacement)
			);
		}

		// 2.
		MappedSchema mappedSchema;
		{
			String schema = context
					.select(currentSchema())
					.fetchSingle()
					.value1();
			mappedSchema = new MappedSchema()
					.withInput("PUBLIC")
					.withOutput(schema)
					.withTables(mappedTables);
		}

		Configuration oldConfig = context.configuration();
		Configuration newConfig = oldConfig.derive(oldConfig.settings()
				.withRenderSchema(true) // 3.
				.withRenderMapping(new RenderMapping().withSchemata(mappedSchema))
		);
		return DSL.using(newConfig);
	}

	private MigrationResult prepareExpectedResult(Consumer<MigrationResult.Builder> buildResult) {
		InternalDatabase database = dbProvider.get();
		try (Connection connection = database.getConnection()) {

			DataSource dataSource = mock(DataSource.class);
			when(dataSource.getConnection()).thenReturn(database.getConnection());

			DSLContext context = rewriteContext(
					new JooqContext(database.getVendor().dialect()).createContext(connection)
			);

			var interlocutor = new ZeroeightInterlocutor(context);
			interlocutor.prepareTables(dataSource);

			MigrationResult.Builder builder = new MigrationResult.Builder(time, interlocutor, creator);
			buildResult.accept(builder);

			MigrationResult built = builder.build();
			connection.commit();
			return built;

		} catch (SQLException ex) {
			throw new AssertionError("Test setup failed", ex);
		}
	}

	private void verifyMigration(Consumer<MigrationResult.Builder> buildResult) {
		MigrationResult expectedResult = prepareExpectedResult(buildResult);

		foundation.shutdown();

		// Perform migration automatically at startup
		assertDoesNotThrow(foundation::startup);

		MigrationResult actualResult = dbProvider.get()
				.query(SQLFunction.readOnly((context) -> MigrationResult.retrieveFrom(context, creator)))
				.join();
		assertEquals(expectedResult, actualResult);
	}

	@TestTemplate
	@SetVendor({Vendor.HSQLDB, Vendor.MARIADB, Vendor.MYSQL})
	public void migrateEmptyTables() {
		verifyMigration((builder) -> {});
	}

	@TestTemplate
	@SetVendor({Vendor.HSQLDB, Vendor.MARIADB, Vendor.MYSQL})
	public void migratePunishments() {
		verifyMigration((builder) -> {
			UUID user1 = UUID.randomUUID();
			UUID user2 = UUID.randomUUID();
			UUID user3 = UUID.randomUUID();
			NetworkAddress user1Address = RandomUtil.randomAddress();
			builder
					.addUser(user1, "user1", user1Address)
					.addUser(user2, "user2", RandomUtil.randomAddress());
			time.advanceBy(Duration.ofDays(1L));
			builder
					.addUser(user3, "user3", RandomUtil.randomAddress())
					.addUser(user2, "user2", user1Address);
			time.advanceBy(Duration.ofDays(2L));
			builder.addUser(user1, "user1renamed", user1Address);

			builder.addHistoricalPunishment(WARN, PlayerVictim.of(user1), PlayerOperator.of(user2), "user1 warned by user2", Duration.ZERO);

			time.advanceBy(Duration.ofSeconds(2L));
			builder.addActivePunishment(BAN, PlayerVictim.of(user1), ConsoleOperator.INSTANCE, "user1 temporarily banned by console", Duration.ofHours(3L));

			time.advanceBy(Duration.ofDays(7L).plus(Duration.ofHours(3L)));
			builder.addActivePunishment(MUTE, AddressVictim.of(user1Address), PlayerOperator.of(user3), "user1 and user2 muted by user3", Duration.ZERO);

			time.advanceBy(Duration.ofDays(1L));
			builder.addHistoricalPunishment(KICK, PlayerVictim.of(user3), PlayerOperator.of(user2), "user2 retaliates by kicking user3", Duration.ZERO);

			time.advanceBy(Duration.ofHours(1L));
			builder.addHistoricalPunishment(BAN, PlayerVictim.of(user2), PlayerOperator.of(user3), "but user3 is privileged and capable, and bans user2 to teach them a lesson!", Duration.ofDays(7L));

			time.advanceBy(Duration.ofSeconds(30L));
			builder.addActivePunishment(WARN, PlayerVictim.of(user3), ConsoleOperator.INSTANCE, "user2, returning, is too powerful, and uses the console to warn and demote user3", Duration.ZERO);
		});
	}
}

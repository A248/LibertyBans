/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.database;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.MiscUtil;

import space.arim.jdbcaesar.transact.TransactionQuerySource;

public class TableDefinitions {
	
	private static final int REVISION_MAJOR = Database.REVISION_MAJOR;
	private static final int REVISION_MINOR = Database.REVISION_MINOR;
	private static final Revision REVISION = Revision.of(REVISION_MAJOR, REVISION_MINOR);
	
	private final LibertyBansCore core;
	private final Database database;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	TableDefinitions(LibertyBansCore core, Database database) {
		this.core = core;
		this.database = database;
	}

	private CentralisedFuture<String> readResource(String resourceName) {
		return database.selectAsync(() -> IOUtils.readResource("sql/" + resourceName + ".sql"));
	}
	
	private CentralisedFuture<List<String>> readAllQueries(String resourceName) {
		return database.selectAsync(() -> IOUtils.readSqlQueries("sql/" + resourceName + ".sql"));
	}
	
	CentralisedFuture<Boolean> createTablesAndViews(final boolean useMariaDb) {
		final boolean useHsqldb = !useMariaDb;

		Set<CentralisedFuture<?>> futures = new HashSet<>();

		CentralisedFuture<List<String>> futureCreateTables = readAllQueries("create_tables").thenApply((createTables) -> {
			String punishmentTypeInfo = MiscUtil.javaToSqlEnum(PunishmentType.class);
			String victimInfo = "VARBINARY(16) NOT NULL";
			String victimTypeInfo = MiscUtil.javaToSqlEnum(Victim.VictimType.class);

			for (ListIterator<String> it = createTables.listIterator(); it.hasNext();) {
				String sqlQuery = it.next();
				it.set(sqlQuery
						.replace("<punishmentTypeInfo>", punishmentTypeInfo)
						.replace("<victimInfo>", victimInfo)
						.replace("<victimTypeInfo>", victimTypeInfo));
			}
			return createTables;
		});
		futures.add(futureCreateTables);

		CentralisedFuture<List<String>> futureCreateViews = readAllQueries("create_views").thenApply((rawCreateViews) -> {
			if (rawCreateViews.size() != 3) {
				throw new IllegalStateException("Unexpected raw create views size " + rawCreateViews.size());
			}
			List<String> createViews = new ArrayList<>();
			for (PunishmentType type : MiscUtil.punishmentTypes()) {
				if (type == PunishmentType.KICK) {
					continue;
				}
				String lowerNamePlural = type.getLowercaseNamePlural();
				String simpleForThisType = rawCreateViews.get(0);
				String applicableForThisType = rawCreateViews.get(1);
				createViews.add(simpleForThisType.replace("<lowerNamePlural>", lowerNamePlural));
				createViews.add(applicableForThisType.replace("<lowerNamePlural>", lowerNamePlural));
			}
			createViews.add(rawCreateViews.get(2));
			return createViews;
		});
		futures.add(futureCreateViews);

		EnumMap<PunishmentType, CentralisedFuture<String>> enactmentProcedures = new EnumMap<>(PunishmentType.class);
		for (PunishmentType type : MiscUtil.punishmentTypes()) {
			String resourceName = "procedure_" + MiscUtil.getEnactmentProcedure(type);
			CentralisedFuture<String> enactmentProcedureFuture = readResource(resourceName);
			enactmentProcedures.put(type, enactmentProcedureFuture);
			futures.add(enactmentProcedureFuture);
		}

		CentralisedFuture<String> refreshProcedure = readResource("procedure_refresh");
		futures.add(refreshProcedure);
		CentralisedFuture<String> refresherEventFuture;
		if (useHsqldb) {
			refresherEventFuture = null;
		} else {
			refresherEventFuture = readResource("event_refresher");
			futures.add(refresherEventFuture);
		}
		CentralisedFuture<?> resourcesFuture = core.getFuturesFactory().copyFuture(
				CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)));

		return resourcesFuture.thenCompose((ignore) -> {
			return database.selectAsync(() -> {
				return database.jdbCaesar().transaction().transactor((querySource) -> {

					if (useHsqldb) {
						// MySQL syntax compatibility
						querySource.query("SET DATABASE SQL SYNTAX MYS TRUE").voidResult().execute();
						// Ensure HSQLDB is case-insensitive by default
						querySource.query("SET IGNORECASE TRUE").voidResult().execute();
					}
					/*
					 * Check for existing database revision
					 */
					Object nullForNonexistentTable = querySource.query(
							// Information schema is supported by both HSQLDB and MariaDB
							"SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE `table_name` = 'libertybans_revision'")
								.singleResult((rs) -> new Object()).execute();
					if (nullForNonexistentTable != null) {
						Revision revision = querySource.query("SELECT `major`, `minor` FROM `libertybans_revision`")
								.singleResult((resultSet) -> Revision.of(resultSet.getInt("major"), resultSet.getInt("minor")))
								.execute();

						return migrateFrom(querySource, revision);
					}
					/*
					 * Execute create tables
					 */
					for (String createTable : futureCreateTables.join()) {
						querySource.query(createTable).voidResult().execute();
					}
					/*
					 * Execute create views
					 */
					for (String createView : futureCreateViews.join()) {
						querySource.query(createView).voidResult().execute();
					}
					/*
					 * Add enactment procedures
					 */
					for (PunishmentType type : MiscUtil.punishmentTypes()) {
						String procedure = enactmentProcedures.get(type).join();
						querySource.query(procedure).voidResult().execute();
					}
					/*
					 * Add refresher procedure
					 */
					querySource.query(refreshProcedure.join()).voidResult().execute();

					int realUpdateCount = querySource.query(
							"INSERT INTO `libertybans_major` (`major`, `minor`) VALUES (?, ?)")
							.params(REVISION_MAJOR, REVISION_MINOR)
							.updateCount((updateCount) -> updateCount).execute();
					if (realUpdateCount != 1) {
						logger.error("Unable to update database revision number. updateCount = {}", realUpdateCount);
						return false;
					}
					return true;

				}).onRollback(() -> false).execute();
			}).thenApply((createdTablesAndViews) -> {
				if (createdTablesAndViews) {
					if (useHsqldb) {
						// Using HSQLDB
						// Periodic task will be started for refreshing
						// Handled by Database#setHyperSQLRefreshTask
						assert refresherEventFuture == null;

					} else {
						// Using MariaDB
						// Database-side event will be used
						@SuppressWarnings("null")
						String refresherEvent = refresherEventFuture.join();
						database.jdbCaesar().query(refresherEvent).voidResult().execute();
					}
				}
				return createdTablesAndViews;
			});
		});
	}
	
	/**
	 * Migrates from a previous database revision if necessary. If the revision is already up to date,
	 * this is a no-op.
	 * 
	 * @param querySource the transaction query sources
	 * @param revision the actual database revision
	 * @throws IllegalStateException if {@code revision} is in the future or is not recognised as a past version
	 * @return true if successful, false otherwise
	 */
	private static boolean migrateFrom(TransactionQuerySource querySource, Revision revision) {
		int comparison = REVISION.compareTo(revision);
		if (comparison == 0) {
			// Same version
			return true;
		}
		if (comparison < 0) {
			logger.error("Your database revision ({}) is in the future! Please upgrade LibertyBans", revision);
			return false;
		}
		// Schema migrator here

		// Currently there is no older version to migrate from
		logger.error("Unknown database revision {}", revision);
		return false;
	}
	
}

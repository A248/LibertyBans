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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
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
		return database.selectAsync(() -> IOUtils.readResource("sql/" + resourceName + ".sql"))
				.thenApplyAsync((bos) -> bos.toString(StandardCharsets.UTF_8));
	}
	
	private CentralisedFuture<List<String>> readAllQueries(String resourceName) {
		return database.selectAsync(() -> IOUtils.readSqlQueries("sql/" + resourceName + ".sql"));
	}
	
	CentralisedFuture<Boolean> createTablesAndViews() {
		final Vendor vendor = database.getVendor();
		logger.debug("Initialising for database vendor {}", vendor);
		Set<CentralisedFuture<?>> futures = new HashSet<>();

		/*
		 * Create tables
		 */
		CentralisedFuture<List<String>> futureCreateTables = readAllQueries("create_tables").thenApply((createTables) -> {

			String punishmentTypeInfo = MiscUtil.javaToSqlEnum(PunishmentType.class);
			String victimInfo = "VARBINARY(16) NOT NULL";
			String victimTypeInfo = MiscUtil.javaToSqlEnum(Victim.VictimType.class);

			for (ListIterator<String> it = createTables.listIterator(); it.hasNext();) {
				String sqlQuery = it.next();
				sqlQuery = sqlQuery
						.replace("<punishmentTypeInfo>", punishmentTypeInfo)
						.replace("<victimInfo>", victimInfo)
						.replace("<victimTypeInfo>", victimTypeInfo);

				if (vendor == Vendor.MARIADB) {
					// Ensure InnoDB
					sqlQuery = sqlQuery.concat(" ENGINE = INNODB");
				}
				if (vendor.noUnsignedNumerics()) {
					sqlQuery = sqlQuery.replace("INT UNSIGNED", "INT");
				}
				it.set(sqlQuery);
			}
			return createTables;
		});
		futures.add(futureCreateTables);

		/*
		 * Create views
		 */
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

		/*
		 * Enactment procedures
		 */
		final EnumMap<PunishmentType, CentralisedFuture<String>> enactmentProcedures;
		if (vendor.useEnactmentProcedures()) {
			enactmentProcedures = new EnumMap<>(PunishmentType.class);
			for (PunishmentType type : MiscUtil.punishmentTypes()) {
				String resourceName = "procedure_" + MiscUtil.getEnactmentProcedure(type);
				CentralisedFuture<String> enactmentProcedureFuture = readResource(resourceName);
				enactmentProcedures.put(type, enactmentProcedureFuture);
				futures.add(enactmentProcedureFuture);
			}
		} else {
			enactmentProcedures = null;
		}

		/*
		 * Refresher event, using MariaDB/MySQL database-side events
		 */
		CentralisedFuture<String> refresherEventFuture;
		if (vendor == Vendor.MARIADB) {
			refresherEventFuture = readResource("event_refresher");
			futures.add(refresherEventFuture);
		} else {
			refresherEventFuture = null;
		}
		CentralisedFuture<?> resourcesFuture = core.getFuturesFactory().copyFuture(
				CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)));

		return resourcesFuture.thenCompose((ignore) -> {
			return database.selectAsync(() -> {
				return database.jdbCaesar().transaction().transactor((querySource) -> {

					if (vendor == Vendor.HSQLDB) {
						executeHyperSqlSettings(querySource);
					}
					/*
					 * Check for existing database revision
					 */
					int tableExistenceCount = querySource.query(
							// Information schema is supported per the SQL standard
							"SELECT COUNT(*) AS `found` FROM `INFORMATION_SCHEMA`.`TABLES` WHERE `TABLE_NAME` = 'libertybans_revision'")
								.combinedResult((resultSet) -> (resultSet.next()) ? resultSet.getInt("found") : 0).execute();
					if (tableExistenceCount != 0) {
						Revision revision = querySource.query("SELECT `major`, `minor` FROM `libertybans_revision`")
								.singleResult((resultSet) -> Revision.of(resultSet.getInt("major"), resultSet.getInt("minor")))
								.execute();
						if (revision == null) {
							logger.error("Revision table exists but has no information!");
							return false;
						}

						return migrateFrom(querySource, revision);
					}
					logger.debug("Creating tables transactionally");
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
					if (enactmentProcedures != null) {
						/*
						 * Add enactment procedures
						 */
						for (PunishmentType type : MiscUtil.punishmentTypes()) {
							String procedure = enactmentProcedures.get(type).join();
							querySource.query(procedure).voidResult().execute();
						}
					}

					int realUpdateCount = querySource.query(
							"INSERT INTO `libertybans_revision` (`constant`, `major`, `minor`) VALUES (?, ?, ?)")
							.params("Constant", REVISION_MAJOR, REVISION_MINOR)
							.updateCount((updateCount) -> updateCount).execute();
					if (realUpdateCount != 1) {
						logger.error("Unable to update database revision number. updateCount = {}", realUpdateCount);
						return false;
					}
					return true;

				}).onRollback(() -> false).execute();

			}).thenApply((successOrFailure) -> {
				if (successOrFailure && vendor == Vendor.MARIADB) {
					// Database-side event periodically invokes the refresh procedure
					@SuppressWarnings("null")
					String refresherEvent = refresherEventFuture.join();
					database.jdbCaesar().query(refresherEvent).voidResult().execute();
				}
				return successOrFailure;
			});
		});
	}
	
	/**
	 * Sets settings specific to HyperSQL
	 * 
	 * @param querySource the transaction query source
	 */
	private static void executeHyperSqlSettings(TransactionQuerySource querySource) {
		Map<String, String> properties = Map.of(
				// MySQL syntax compatibility
				"SQL SYNTAX MYS", "TRUE",
				// Ensure HSQLDB is case-insensitive for all comparisons
				"SQL IGNORECASE", "TRUE",

				/*
				 * Enforce SQL standards on
				 * 1.) table and column names
				 * 2.) ambiguous column references
				 * 3.) illegal type conversions
				 */
				"SQL NAMES", "TRUE",
				"SQL REFERENCES", "TRUE",
				"SQL TYPES", "TRUE",

				// Use CACHED  tables
				"DEFAULT TABLE TYPE", "CACHED",
				// Use MVLOCKS concurrency control
				"TRANSACTION CONTROL", "MVLOCKS"
				);
		for (Map.Entry<String, String> property : properties.entrySet()) {
			querySource.query("SET DATABASE " + property.getKey() + ' ' + property.getValue())
					.voidResult().execute();
		}
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

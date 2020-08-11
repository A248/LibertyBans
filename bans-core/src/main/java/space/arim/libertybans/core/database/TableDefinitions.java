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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.util.sql.CloseMe;
import space.arim.api.util.sql.SqlQuery;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.MiscUtil;

public class TableDefinitions {
	
	private static final int REVISION_MAJOR = Database.REVISION_MAJOR;
	private static final int REVISION_MINOR = Database.REVISION_MINOR;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	private static CentralisedFuture<String> readResource(Database database, String resourceName) {
		return database.selectAsync(() -> IOUtils.readSqlResourceBlocking(resourceName));
	}
	
	static CentralisedFuture<Boolean> createTablesAndViews(final Database database, final boolean useMySql) {
		final boolean useHsqldb = !useMySql;

		EnumMap<PunishmentType, CentralisedFuture<String>> enactmentProcedures = new EnumMap<>(PunishmentType.class);
		for (PunishmentType type : MiscUtil.punishmentTypes()) {
			String resourceName = "procedure_" + MiscUtil.getEnactmentProcedure(type) + ".sql";
			enactmentProcedures.put(type, readResource(database, resourceName));
		}
		CentralisedFuture<String> refreshProcedure = readResource(database, "procedure_refresh.sql");
		CentralisedFuture<String> refresherEventFuture = (useHsqldb) ? null : readResource(database, "event_refresher.sql");

		return database.selectAsync(() -> {
			String punishmentTypeInfo = MiscUtil.javaToSqlEnum(PunishmentType.class);
			String victimTypeInfo = MiscUtil.javaToSqlEnum(Victim.VictimType.class);
			List<SqlQuery> queries = new ArrayList<>();

			if (useHsqldb) {
				// HSQLDB compatibility with MySQL non-standard syntax
				// http://hsqldb.org/doc/2.0/guide/compatibility-chapt.html#coc_compatibility_mysql
				queries.add(SqlQuery.of("SET DATABASE SQL SYNTAX MYS TRUE"));
			}
			queries.addAll(List.of(

					/*
					 * Revision schema
					 */
					SqlQuery.of(
							"CREATE TABLE IF NOT EXISTS `libertybans_revision` ("
							+ "`constant` INT NOT NULL UNIQUE DEFAULT 0 "
							+ "COMMENT 'This column is unique and can only have 1 value to ensure this table has only 1 row', "
							+ "`major` INT NOT NULL, "
							+ "`minor` INT NOT NULL)"),
					SqlQuery.of(
							"INSERT INTO `libertybans_major` (`major`, `minor`) "
							+ "VALUES (?, ?) ON DUPLICATE KEY UPDATE `major` = ?, `minor` = ?",
							REVISION_MAJOR, REVISION_MINOR),

					/*
					 * UUIDs, names, and address
					 */
					/*
					 * The reason these are not the same table is that some servers
					 * may need to periodically clear the addresses table per GDPR
					 * regulations. However, using a single table with a null address
					 * would be unwise, since unique constraints don't work nicely
					 * with null values.
					 */
					SqlQuery.of(
							"CREATE TABLE IF NOT EXISTS `libertybans_names` ("
							+ "`uuid` BINARY(16) NOT NULL, "
							+ "`name` VARCHAR(16) NOT NULL, "
							+ "`updated` INT UNSIGNED NOT NULL, "
							+ "PRIMARY KEY (`uuid`),"
							+ "UNIQUE (`uuid`, `name`))"),
					SqlQuery.of(
							"CREATE TABLE IF NOT EXISTS `libertybans_addresses` ("
							+ "`uuid` BINARY(16) NOT NULL, "
							+ "`address` VARBINARY(16) NOT NULL, "
							+ "`updated` INT UNSIGNED NOT NULL, "
							+ "PRIMARY KEY (`uuid`), "
							+ "UNIQUE (`uuid`, `address`))"),

					/*
					 * Core punishments tables
					 */
					SqlQuery.of(
							"CREATE TABLE IF NOT EXISTS `libertybans_punishments` ("
							+ "`id` INT AUTO_INCREMENT PRIMARY KEY, "
							+ "`type` " + punishmentTypeInfo + ", "
							+ "`operator` BINARY(16) NOT NULL, "
							+ "`reason` VARCHAR(256) NOT NULL, "
							+ "`scope` VARCHAR(32) NULL DEFAULT NULL, "
							+ "`start` INT UNSIGNED NOT NULL, "
							+ "`end` INT UNSIGNED NOT NULL)"),
					SqlQuery.of(
							"CREATE TABLE IF NOT EXISTS `libertybans_bans` ("
							+ "`id` INT PRIMARY KEY, "
							+ "`victim` VARBINARY(16) NOT NULL, "
							+ "`victim_type` " + victimTypeInfo + ", "
							+ "FOREIGN KEY (`id`) REFERENCES `libertybans_punishments` (`id`) ON DELETE CASCADE, "
							+ "UNIQUE (`victim`, `victim_type`))"),
					SqlQuery.of(
							"CREATE TABLE IF NOT EXISTS `libertybans_mutes` ("
							+ "`id` INT PRIMARY KEY, "
							+ "`victim` VARBINARY(16) NOT NULL, "
							+ "`victim_type` " + victimTypeInfo + ", "
							+ "FOREIGN KEY (`id`) REFERENCES `libertybans_punishments` (`id`) ON DELETE CASCADE, "
							+ "UNIQUE (`victim`, `victim_type`))"),
					SqlQuery.of(
							"CREATE TABLE IF NOT EXISTS `libertybans_warns` ("
							+ "`id` INT PRIMARY KEY, "
							+ "`victim` VARBINARY(16) NOT NULL, "
							+ "`victim_type` " + victimTypeInfo + ", "
							+ "FOREIGN KEY (`id`) REFERENCES `libertybans_punishments` (`id`) ON DELETE CASCADE)"),
					SqlQuery.of(
							"CREATE TABLE IF NOT EXISTS `libertybans_history` ("
							+ "`id` INT NOT NULL, "
							+ "`victim` VARBINARY(16) NOT NULL, "
							+ "`victim_type` " + victimTypeInfo + ", "
							+ "FOREIGN KEY (`id`) REFERENCES `libertybans_punishments` (`id`) ON DELETE CASCADE)")
					));

			/*
			 * Procedures read from internal resources
			 */
			for (PunishmentType type : MiscUtil.punishmentTypes()) {
				CentralisedFuture<String> procedureFuture = enactmentProcedures.get(type);
				String procedure = procedureFuture.join();
				if (procedure == null) {
					String procedureName = MiscUtil.getEnactmentProcedure(type);
					logger.error("Halting startup having failed to read procedure {}", procedureName);
					return false;
				}
				queries.add(SqlQuery.of(procedure));
			}

			for (PunishmentType type : MiscUtil.punishmentTypes()) {
				if (type == PunishmentType.KICK) {
					// There is no table for kicks, they go straight to history
					continue;
				}
				String lowerNamePlural = type.getLowercaseNamePlural(); // 'ban'
				queries.add(SqlQuery.of(
							"CREATE VIEW IF NOT EXISTS `libertybans_simple_" + lowerNamePlural + "` AS "
							+ "SELECT `puns`.`id`, `puns`.`type`, `thetype`.`victim`, `thetype`.`victim_type`, "
							+ "`puns`.`operator`, `puns`.`reason`, `puns`.`scope`, `puns`.`start`, `puns`.`end` "
							+ "FROM `libertybans_" + lowerNamePlural + "` `thetype` INNER JOIN `libertybans_punishments` `puns` "
							+ "ON `puns`.`id` = `thetype`.`id`"));
				queries.add(SqlQuery.of(
					"CREATE VIEW IF NOT EXISTS `libertybans_applicable_" + lowerNamePlural + "` AS "
					+ "SELECT `puns`.`id`, `puns`.`type`, `puns`.`victim`, `puns`.`victim_type`, `puns`.`operator`, "
					+ "`puns`.`reason`, `puns`.`scope`, `puns`.`start`, `puns`.`end`, `addrs`.`uuid`, `addrs`.`address` "
					+ "FROM `libertybans_simple_" + lowerNamePlural + "` `puns` INNER JOIN `libertybans_addresses` `addrs` "
					+ "ON (`puns`.`victim_type` = 'PLAYER' AND `puns`.`victim` = `addrs`.`uuid` "
					+ "OR `puns`.`victim_type` = 'ADDRESS' AND `puns`.`victim` = `addrs`.`address`)"));
			}
			queries.add(SqlQuery.of(
					"CREATE VIEW IF NOT EXISTS `libertybans_simple_active` AS "
					+ "SELECT * FROM `libertybans_simple_bans` UNION ALL "
					+ "SELECT * FROM `libertybans_simple_mutes` UNION ALL "
					+ "SELECT * FROM `libertybans_simple_warns`"));

			/*
			 * MySQL has the capabilitiy to run periodic refresh on the database-side using events
			 * For HSQLDB the procedure will be added but a periodic task server-side will be used
			 */
			String refreshProc = refreshProcedure.join();
			if (refreshProc == null) {
				logger.error("Halting startup having failed to read the refresh procedure");
				return false;
			}
			queries.add(SqlQuery.of(refreshProc));

			if (useHsqldb) {
				// Using HSQLDB
				// Periodic task will be started below, after table creation
				assert refresherEventFuture == null;
			} else {
				// Using MySQL
				assert refresherEventFuture != null;

				@SuppressWarnings("null")
				String refresherEvent = refresherEventFuture.join();
				if (refresherEvent == null) {
					logger.error("Halting startup having failed to read the refresher event");
					return false;
				}
				queries.add(SqlQuery.of(refresherEvent));
			}

			try (CloseMe cm = database.getBackend().execute(queries.toArray(SqlQuery.EMPTY_QUERY_ARRAY))) {

			} catch (SQLException ex) {
				logger.error("Failed to create tables", ex);
				return false;
			}
			// Initialise cleaner task if using HSQLDB
			/*if (useHsqldb) {
				PlatformScheduler scheduler = new DetectingPlatformHandle(core.getRegistry())
						.registerDefaultServiceIfAbsent(PlatformScheduler.class);

				ScheduledTask task = scheduler.runRepeatingTask(new DbHelper.HsqldbCleanerRunnable(core.getDbHelper()),
						1L, 1L, TimeUnit.HOURS);
				hsqldbCleaner = Objects.requireNonNull(task);
			}*/
			return true;
		});
	}
	
}

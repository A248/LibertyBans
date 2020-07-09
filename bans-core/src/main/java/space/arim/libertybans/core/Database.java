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
package space.arim.libertybans.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;

import space.arim.universal.util.ThisClass;
import space.arim.universal.util.concurrent.CentralisedFuture;

import space.arim.api.env.DetectingPlatformHandle;
import space.arim.api.env.PlatformScheduler;
import space.arim.api.env.PlatformScheduler.ScheduledTask;
import space.arim.api.util.config.Config;
import space.arim.api.util.config.SimpleConfig;
import space.arim.api.util.sql.CloseMe;
import space.arim.api.util.sql.HikariPoolSqlBackend;
import space.arim.api.util.sql.SqlBackend;
import space.arim.api.util.sql.SqlQuery;

import space.arim.libertybans.api.PunishmentDatabase;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.bootstrap.StartupException;

class Database implements PunishmentDatabase, Part {

	private final LibertyBansCore core;
	private final Config config;
	
	private volatile HikariPoolSqlBackend backend;
	private volatile ExecutorService executor;
	
	private volatile ScheduledTask hsqldbCleaner;
	
	private static final int REVISION_MAJOR = 1;
	private static final int REVISION_MINOR = 0;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	private static final boolean TRACE_FOREIGN_CONNECTIONS;
	
	static {
		boolean traceForeign = false;
		try {
			traceForeign = Boolean.getBoolean("space.arim.libertybans.traceForeignConnections");
		} catch (SecurityException ignored) {}
		TRACE_FOREIGN_CONNECTIONS = traceForeign;
	}
	
	Database(LibertyBansCore core) {
		this.core = core;
		config = new SimpleConfig(core.getFolder(), "sql.yml");
	}
	
	public Config getConfig() {
		return config;
	}

	@Override
	public void startup() {
		config.saveDefaultConfig();
		config.reloadConfig();

		HikariConfig hikariConf = new HikariConfig();
		boolean useMySql = config.getBoolean("storage-backend-mysql");
		String username = config.getString("mysql-details.user");
		String password = config.getString("mysql-details.password");
		if (useMySql && (username.equals("username") || password.equals("defaultpass"))) {
			logger.warn("Not using MySQL because authentication details are still default");
			useMySql = false;
		}

		int minPoolSize = config.getInteger("connection-pool.min");
		int maxPoolSize = config.getInteger("connection-pool.max");
		int threadPoolSize = config.getInteger("connection-pool.threads");
		if (minPoolSize < 0 || maxPoolSize < 1 || threadPoolSize < 2) {
			logger.warn("Bad connection pool settings: {}, {}, {}", minPoolSize, maxPoolSize, threadPoolSize);
			minPoolSize = config.getDefaultObject("connection-pool.min", Integer.class);
			maxPoolSize = config.getDefaultObject("connection-pool.max", Integer.class);
			threadPoolSize = config.getDefaultObject("connection-pool.threads", Integer.class);
		}
		hikariConf.setMinimumIdle(minPoolSize);
		hikariConf.setMaximumPoolSize(maxPoolSize);

		int connectionTimeout = config.getInteger("timeouts.connection-timeout-seconds");
		int maxLifetime = config.getInteger("timeouts.max-lifetime-minutes");
		if (connectionTimeout < 1 || maxLifetime < 1) {
			logger.warn("Bad timeout settings: {}, {}", connectionTimeout, maxLifetime);
			connectionTimeout = config.getDefaultObject("timeouts.connection-timeout-seconds", Integer.class);
			maxLifetime = config.getDefaultObject("timeouts.max-lifetime-minutes", Integer.class);
		}
		hikariConf.setConnectionTimeout(TimeUnit.MILLISECONDS.convert(connectionTimeout, TimeUnit.SECONDS));
		hikariConf.setMaxLifetime(TimeUnit.MILLISECONDS.convert(maxLifetime, TimeUnit.MINUTES));

		if (useMySql) {
			String host = config.getString("mysql-details.host");
			int port = config.getInteger("mysql-details.port");
			String database = config.getString("mysql-details.database");
			hikariConf.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
			hikariConf.setUsername(username);
			hikariConf.setPassword(password);
			/*
			hikariConf.addDataSourceProperty("cachePrepStmts", "true");
			hikariConf.addDataSourceProperty("prepStmtCacheSize", "25");
			hikariConf.addDataSourceProperty("prepStmtCacheSqlLimit", "256");
			hikariConf.addDataSourceProperty("useServerPrepStmts", "true");
			*/

		} else {
			hikariConf.setJdbcUrl("jdbc:hsqldb:file:" + core.getFolder() + "/data/database;hsqldb.lock_file=false");
			hikariConf.setUsername("SA");
			hikariConf.setPassword("");
			/*
			 * Because HikariCP checks the context ClassLoader first, set
			 * the context ClassLoader to ensure we use our own HSQLDB classes
			 */
			Thread current = Thread.currentThread();
			ClassLoader original = current.getContextClassLoader();
			try {
				current.setContextClassLoader(getClass().getClassLoader());
				hikariConf.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
			} finally {
				current.setContextClassLoader(original);
			}
		}
		hikariConf.setAutoCommit(false);
		hikariConf.addDataSourceProperty("allowMultiQueries", "true");

		for (String property : config.getStringList("connection-properties")) {
			int index = property.indexOf('=');
			if (index == -1 || index == 0 || index == property.length() - 1) {
				logger.warn("Connection property '{}' does not conform to format 'property=value'", property);
				continue;
			}
			String prop = property.substring(0, index);
			String val = property.substring(index + 1, property.length());
			hikariConf.addDataSourceProperty(prop, val);
		}
		hikariConf.setPoolName("Liberty-HikariCP@" + hikariConf.hashCode());
		executor = Executors.newFixedThreadPool(threadPoolSize, new DbHelper.ThreadFactoryImpl("LibertyBans-pool-thread-"));
		backend = new HikariPoolSqlBackend(hikariConf);

		boolean success = createTablesAndViews(!useMySql).join();
		if (!success) {
			throw new StartupException("Database table and views creation failed");
		}
	}
	
	private CentralisedFuture<String> readResource(String resourceName) {
		return core.getDbHelper().readResource(resourceName);
	}
	
	private CentralisedFuture<Boolean> createTablesAndViews(final boolean useHsqldb) {
		EnumMap<PunishmentType, CentralisedFuture<String>> enactmentProcedures = new EnumMap<>(PunishmentType.class);
		for (PunishmentType type : MiscUtil.punishmentTypes()) {
			String resourceName = "procedure_" + MiscUtil.getEnactmentProcedure(type) + ".sql";
			enactmentProcedures.put(type, readResource(resourceName));
		}
		CentralisedFuture<String> refreshProcedure = readResource("procedure_refresh.sql");
		CentralisedFuture<String> refresherEventFuture = (useHsqldb) ? null : readResource("event_refresher.sql");

		return core.getDbHelper().selectAsync(() -> {
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

			try (CloseMe cm = getBackend().execute(queries.toArray(SqlQuery.EMPTY_QUERY_ARRAY))) {

			} catch (SQLException ex) {
				logger.error("Failed to create tables", ex);
				return false;
			}
			// Initialise cleaner task if using HSQLDB
			if (useHsqldb) {
				PlatformScheduler scheduler = new DetectingPlatformHandle(core.getRegistry())
						.registerDefaultServiceIfAbsent(PlatformScheduler.class);

				ScheduledTask task = scheduler.runRepeatingTask(new DbHelper.HsqldbCleanerRunnable(core.getDbHelper()),
						1L, 1L, TimeUnit.HOURS);
				hsqldbCleaner = Objects.requireNonNull(task);
			}
			return true;
		});
	}
	
	private boolean isHsqlDb() {
		return "org.hsqldb.jdbc.JDBCDriver".equals(backend.getDataSource().getDriverClassName());
	}
	
	@Override
	public int getMajorRevision() {
		return REVISION_MAJOR;
	}
	
	@Override
	public int getMinorRevision() {
		return REVISION_MINOR;
	}

	@Override
	public void shutdown() {
		List<SqlQuery> queries = new ArrayList<>();
		for (PunishmentType type : MiscUtil.punishmentTypes()) {
			queries.add(SqlQuery.of("DROP PROCEDURE IF EXISTS `" + MiscUtil.getEnactmentProcedure(type) + '`'));
		}
		queries.add(SqlQuery.of("DROP PROCEDURE IF EXISTS `libertybans_refresh`"));
		if (isHsqlDb()) {
			ScheduledTask hsqldbCleaner = this.hsqldbCleaner;
			if (hsqldbCleaner == null) {
				logger.debug("No cleaner task for HSQLDB backend");
			} else {
				hsqldbCleaner.cancel();
			}
			queries.add(SqlQuery.of("SHUTDOWN"));
		} else {
			queries.add(SqlQuery.of("DROP EVENT IF EXISTS `libertybans_refresher`"));
		}
		try (CloseMe cm = backend.execute(queries.toArray(SqlQuery.EMPTY_QUERY_ARRAY))) {
			
		} catch (SQLException ex) {
			logger.warn("Failed shutting down database", ex);
		}
		executor.shutdown();
		backend.getDataSource().close();
		try {
			boolean finished = executor.awaitTermination(10L, TimeUnit.SECONDS);
			if (!finished) {
				logger.warn("ExecutorService did not complete termination");
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			logger.warn("Interrupted while awaiting thread pool termination", ex);
		}
		backend = null;
		executor = null;
	}

	SqlBackend getBackend() {
		if (logger.isDebugEnabled()) {
			logger.debug("Getting backend on thread {}", Thread.currentThread());
		}
		return backend;
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		if (logger.isDebugEnabled()) {
			logger.debug("Foreign caller acquiring connection on thread  {}", Thread.currentThread());
			if (TRACE_FOREIGN_CONNECTIONS) {
				logger.trace("Call trace as requested by space.arim.libertybans.traceForeignConnections=true", new Exception());
			}
		}
		HikariPoolSqlBackend backend = Objects.requireNonNull(this.backend, "Database not yet started");
		return backend.getDataSource().getConnection();
	}

	@Override
	public Executor getExecutor() {
		return Objects.requireNonNull(executor, "Database not yet started");
	}

}

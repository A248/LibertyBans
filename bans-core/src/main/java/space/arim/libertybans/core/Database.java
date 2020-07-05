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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;

import space.arim.universal.util.ThisClass;
import space.arim.universal.util.concurrent.CentralisedFuture;

import space.arim.api.util.config.Config;
import space.arim.api.util.config.SimpleConfig;
import space.arim.api.util.sql.CloseMe;
import space.arim.api.util.sql.HikariPoolSqlBackend;
import space.arim.api.util.sql.SqlBackend;
import space.arim.api.util.sql.SqlQuery;

import space.arim.libertybans.api.PunishmentDatabase;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;

class Database implements PunishmentDatabase, Part {

	private final LibertyBansCore core;
	private final Config config;
	
	private volatile HikariPoolSqlBackend backend;
	private volatile ExecutorService executor;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
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
			hikariConf.addDataSourceProperty("cachePrepStmts", "true");
			//hikariConf.addDataSourceProperty("prepStmtCacheSize", "25");
			//hikariConf.addDataSourceProperty("prepStmtCacheSqlLimit", "256");
			hikariConf.addDataSourceProperty("useServerPrepStmts", "true");

		} else {
			hikariConf.setJdbcUrl("jdbc:hsqldb:file:" + core.getFolder() + "/data/storage;hsqldb.lock_file=false");
			hikariConf.setUsername("SA");
			hikariConf.setPassword("");
			/*
			 * Because HikariCP checks the context ClassLoader first, we set
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
		hikariConf.setPoolName("LibertyBans@" + core.getEnvironment().hashCode());
		executor = Executors.newFixedThreadPool(threadPoolSize);
		backend = new HikariPoolSqlBackend(hikariConf);

		createTablesAndViews();
	}
	
	private CentralisedFuture<?> createTablesAndViews() {
		return executeAsync(() -> {
			String punishmentTypeInfo = javaToSqlEnum(PunishmentType.class);
			String victimTypeInfo = javaToSqlEnum(Victim.VictimType.class);
			List<SqlQuery> queries = new ArrayList<>();
			if (isHsqlDb()) {
				queries.add(SqlQuery.of("SET DATABASE SQL SYNTAX MYS TRUE"));
			}
			queries.addAll(List.of(
					SqlQuery.of("CREATE TABLE IF NOT EXISTS `libertybans_names` ("
							+ "`uuid` BINARY(16) NOT NULL, "
							+ "`name` VARCHAR(16), "
							+ "`updated` BIGINT NOT NULL, "
							+ "PRIMARY KEY(`uuid`, `name`))"),
					SqlQuery.of("CREATE TABLE IF NOT EXISTS `libertybans_addresses` ("
							+ "`uuid` BINARY(16) NOT NULL, "
							+ "`address` VARBINARY(16) NOT NULL, "
							+ "`updated` BIGINT NOT NULL, "
							+ "PRIMARY KEY (`uuid`, `address`))"),
					SqlQuery.of("CREATE TABLE IF NOT EXISTS `libertybans_punishments_singular` ("
							+ "`id` INT AUTO_INCREMENT NOT NULL, "
							+ "`type` " + punishmentTypeInfo + ", "
							+ "`victim` VARBINARY(16) NOT NULL, "
							+ "`victim_type` " + victimTypeInfo + ", "
							+ "`operator` BINARY(16) NOT NULL, "
							+ "`reason` VARCHAR(256) NOT NULL, "
							+ "`scope` VARCHAR(16) NULL DEFAULT NULL, "
							+ "`start` INT NOT NULL, "
							+ "`end` INT NOT NULL, "
							+ "`undone` BOOLEAN NOT NULL, "
							+ "PRIMARY KEY(`type`, `victim`, `victim_type`)"),
					SqlQuery.of("CREATE TABLE IF NOT EXISTS `libertybans_punishments_multiple` ("
							+ "`id` INT AUTO_INCREMENT PRIMARY KEY, "
							+ "`type` " + punishmentTypeInfo + ", "
							+ "`victim` VARBINARY(16) NOT NULL, "
							+ "`victim_type` " + victimTypeInfo + ", "
							+ "`operator` BINARY(16) NOT NULL, "
							+ "`reason` VARCHAR(256) NOT NULL, "
							+ "`scope` VARCHAR(16) NULL DEFAULT NULL, "
							+ "`start` INT NOT NULL, "
							+ "`end` INT NOT NULL, "
							+ "`undone` BOOLEAN NOT NULL)"),
					SqlQuery.of("CREATE OR REPLACE VIEW `libertybans_punishments_all` AS "
							+ "SELECT * FROM `libertybans_punishments_singular` UNION "
							+ "SELECT * FROM `libertybans_punishments_multiple`"),
					SqlQuery.of("CREATE OR REPLACE VIEW `libertybans_applicable_singular` AS "
							+ "SELECT `puns`.`id`, `puns`.`type`, `puns`.`victim`, `puns`.`victim_type`, `puns`.`operator`, "
							+ "`puns`.`reason`, `puns`.`scope`, `puns`.`start`, `puns`.`end`, `addrs`.`uuid`, `addrs`.`address` "
							+ "FROM `libertybans_singular` `puns` INNER JOIN `libertybans_addresses` `addrs` "
							+ "ON (`puns`.`victim_type` = 'PLAYER' AND `puns`.`victim` = `addrs`.`uuid` "
							+ "OR `puns`.`victim_type` = 'ADDRESS' AND `puns`.`victim` = `addrs`.`address`) "
							+ "WHERE `undone` = FALSE"),
					SqlQuery.of("CREATE OR REPLACE VIEW `libertybans_applicable_multiple` AS "
							+ "SELECT `puns`.`id`, `puns`.`type`, `puns`.`victim`, `puns`.`victim_type`, `puns`.`operator`, "
							+ "`puns`.`reason`, `puns`.`scope`, `puns`.`start`, `puns`.`end`, `addrs`.`uuid`, `addrs`.`address` "
							+ "FROM `libertybans_singular` `puns` INNER JOIN `libertybans_addresses` `addrs` "
							+ "ON (`puns`.`victim_type` = 'PLAYER' AND `puns`.`victim` = `addrs`.`uuid` "
							+ "OR `puns`.`victim_type` = 'ADDRESS' AND `puns`.`victim` = `addrs`.`address`) "
							+ "WHERE `undone` = FALSE")/*,
					SqlQuery.of("CREATE OR REPLACE VIEW `libertybans_singular_byname` AS "
							+ "SELECT `puns`.`id`, `puns`.`type`, `puns`.`operator`, `puns`.`reason`, "
							+ "`puns`.`scope`, `puns`.`start`, `puns.end` "
							+ "FROM `libertybans_singular` `puns` "
							+ "INNER JOIN `libertybans_names` `names` "
							+ "ON (`puns`.`victim_type` = 'PLAYER' AND `puns`.`victim` = `names`.`uuid`)"),
					SqlQuery.of("CREATE OR REPLACE VIEW `libertybans_singular_byaddr` AS "
							+ "SELECT `puns`.`id`, `puns`.`type`, `puns`.`operator`, `puns`.`reason`, "
							+ "`puns`.`scope`, `puns`.`start`, `puns.end` "
							+ "FROM `libertybans_singular` `puns` "
							+ "INNER JOIN `libertybans_addresses` `addrs` "
							+ "ON (`puns`.`victim_type` = 'ADDRESS' AND `puns`.`victim` = `addrs`.`address`)"),
					SqlQuery.of("CREATE OR REPLACE VIEW `libertybans_multiple_byname` AS "
							+ "SELECT `puns`.`id`, `puns`.`type`, `puns`.`operator`, `puns`.`reason`, "
							+ "`puns`.`scope`, `puns`.`start`, `puns.end` "
							+ "FROM `libertybans_singular` `puns` "
							+ "INNER JOIN `libertybans_names` `names` "
							+ "ON (`puns`.`victim_type` = 'PLAYER' AND `puns`.`victim` = `names`.`uuid`)"),
					SqlQuery.of("CREATE OR REPLACE VIEW `libertybans_multiple_byaddr` AS "
							+ "SELECT `puns`.`id`, `puns`.`type`, `puns`.`operator`, `puns`.`reason`, "
							+ "`puns`.`scope`, `puns`.`start`, `puns.end` "
							+ "FROM `libertybans_singular` `puns` "
							+ "INNER JOIN `libertybans_addresses` `addrs` "
							+ "ON (`puns`.`victim_type` = 'ADDRESS' AND `puns`.`victim` = `addrs`.`address`)")*/
					));

			try (CloseMe cm = getBackend().execute(queries.toArray(new SqlQuery[] {}))) {
				
			} catch (SQLException ex) {
				logger.error("Failed to create tables", ex);
			}
		});
	}
	
	private static <E extends Enum<E>> String javaToSqlEnum(Class<E> enumClass) {
		StringBuilder builder = new StringBuilder("ENUM (");
		E[] elements = enumClass.getEnumConstants();
		for (int n = 0; n < elements.length; n++) {
			if (n != 0) {
				builder.append(", ");
			}
			String name = elements[n].name();
			builder.append('\'').append(name).append('\'');
		}
		return builder.append(')').toString();
	}
	
	private boolean isHsqlDb() {
		return backend.getDataSource().getDriverClassName().equals("org.hsqldb.jdbc.JDBCDriver");
	}

	@Override
	public void shutdown() {
		if (isHsqlDb()) {
			try (CloseMe cm = backend.execute("SHUTDOWN")) {
				
			} catch (SQLException ex) {
				logger.warn("Failed shutting down database", ex);
			}
		}
		executor.shutdown();
		backend.getDataSource().close();
		try {
			boolean finished = executor.awaitTermination(10L, TimeUnit.SECONDS);
			if (!finished) {
				logger.warn("ExecutorService did not complete termination");
			}
		} catch (InterruptedException ex) {
			logger.warn("Interrupted while awaiting thread pool termination", ex);
			Thread.currentThread().interrupt();
		}
	}

	SqlBackend getBackend() {
		return Objects.requireNonNull(backend, "Database not yet initialized");
	}
	
	CentralisedFuture<?> executeAsync(Runnable command) {
		return core.getFuturesFactory().runAsync(command, executor);
	}
	
	<T> CentralisedFuture<T> selectAsync(Supplier<T> supplier) {
		return core.getFuturesFactory().supplyAsync(supplier, executor);
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		if (backend == null) {
			throw new IllegalStateException("Database not yet initialized");
		}
		return backend.getDataSource().getConnection();
	}

	@Override
	public Executor getExecutor() {
		if (executor == null) {
			throw new IllegalStateException("Database not yet initialized");
		}
		return executor;
	}

}

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

package space.arim.libertybans.core.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import space.arim.libertybans.core.database.flyway.MigrateWithFlyway;
import space.arim.libertybans.core.database.flyway.MigrationFailedException;
import space.arim.libertybans.core.database.jooq.JooqClassloading;
import space.arim.libertybans.core.database.jooq.JooqContext;
import space.arim.libertybans.core.database.execute.JooqQueryExecutor;
import space.arim.libertybans.core.service.SimpleThreadFactory;
import space.arim.omnibus.util.ThisClass;

import space.arim.libertybans.bootstrap.StartupException;
import space.arim.libertybans.core.config.SqlConfig;

/**
 * Database settings creator, NOT thread safe!
 * 
 * @author A248
 *
 */
public class DatabaseSettings {

	private final Path folder;
	private final DatabaseManager manager;

	private DatabaseSettingsConfig config;
	private Vendor vendor;
	private HikariConfig hikariConf;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	public DatabaseSettings(Path folder, DatabaseManager manager) {
		this.folder = folder;
		this.manager = manager;
	}
	
	/**
	 * Shortcut for <code>create(core.getConfigs().getSql())</code>
	 * 
	 * @return a database result, which should be checked for success or failure
	 */
	DatabaseResult create() {
		return create(manager.configs().getSqlConfig());
	}

	/**
	 * Creates a data source connection pool. <b>Exposed only for testing purposes</b>
	 *
	 * @param config the config
	 * @return the data source
	 */
	public HikariDataSource createDataSource(DatabaseSettingsConfig config) {
		this.config = config;
		vendor = config.vendor();
		hikariConf = new HikariConfig();
		setHikariConfig();
		return new HikariDataSource(hikariConf);
	}

	/**
	 * Creates an accessible database
	 * 
	 * @param config the config
	 * @return a database result, which should be checked for success or failure
	 */
	public DatabaseResult create(DatabaseSettingsConfig config) {
		HikariDataSource hikariDataSource = createDataSource(config);
		// Check database compatibility and provide retro support if necessary
		boolean retroSupport = checkCompatibilityAndYieldRetroSupport(hikariDataSource);

		JooqContext jooqContext = new JooqContext(vendor.dialect(), retroSupport);
		ExecutorService threadPool = Executors.newFixedThreadPool(
				hikariConf.getMaximumPoolSize(),
				SimpleThreadFactory.create("Database")
		);
		StandardDatabase database  = new StandardDatabase(
				manager, vendor, hikariDataSource,
				new JooqQueryExecutor(jooqContext, hikariDataSource, manager.futuresFactory(), threadPool),
				threadPool
		);

		JooqClassloading jooqClassloading = new JooqClassloading(jooqContext);
		MigrateWithFlyway migrateWithFlyway = new MigrateWithFlyway(hikariDataSource, vendor);
		try {
			migrateWithFlyway.migrate(jooqContext);
		} catch (MigrationFailedException ex) {
			logger.error("Unable to migrate your database. Please create a backup of your database "
					+ "and promptly report this issue.", ex);
			return new DatabaseResult(database, jooqClassloading, false);
		}
		return new DatabaseResult(database, jooqClassloading, true);
	}

	private boolean checkCompatibilityAndYieldRetroSupport(HikariDataSource dataSource) {
		if (Boolean.getBoolean("libertybans.database.disablecheck")) {
			return false;
		}
		try (Connection connection = dataSource.getConnection()) {
			connection.setReadOnly(true);

			return new DatabaseRequirements(
					vendor, connection
			).checkRequirementsAndYieldRetroSupport();

		} catch (java.sql.SQLException ex) {
			throw new IllegalStateException(
					"Unable to connect to database. Please make sure your authentication details are correct.", ex);
		}
	}

	private void setHikariConfig() {
		setUsernameAndPassword();
		setConfiguredDriver();

		// Timeouts
		SqlConfig.Timeouts timeouts = config.timeouts();
		Duration connectionTimeout = Duration.ofSeconds(timeouts.connectionTimeoutSeconds());
		Duration maxLifetime = Duration.ofMinutes(timeouts.maxLifetimeMinutes());
		hikariConf.setConnectionTimeout(connectionTimeout.toMillis());
		hikariConf.setMaxLifetime(maxLifetime.toMillis());

		// Pool size
		int poolSize = config.poolSize();
		hikariConf.setMinimumIdle(poolSize);
		hikariConf.setMaximumPoolSize(poolSize);

		// Other settings
		hikariConf.setAutoCommit(DatabaseConstants.AUTOCOMMIT);
		hikariConf.setTransactionIsolation("TRANSACTION_REPEATABLE_READ");
		hikariConf.setPoolName("LibertyBansPool-" + vendor);
		hikariConf.setConnectionInitSql(vendor.getConnectionInitSql());
		hikariConf.setIsolateInternalQueries(true);
	}

	private void setUsernameAndPassword() {
		SqlConfig.AuthDetails authDetails = config.authDetails();
		String username = authDetails.username();
		String password = authDetails.password();
		if (vendor.isRemote() && (username.equals("defaultuser") || password.equals("defaultpass"))) {
			logger.warn("The database authentication details are still set to the default values - " +
					"please set the correct credentials. For now, a local database will be used.");
			vendor = Vendor.HSQLDB;
		}
		if (vendor == Vendor.HSQLDB) {
			username = "SA";
			password = "";
		}
		hikariConf.setUsername(username);
		hikariConf.setPassword(password);
	}

	private void setConfiguredDriver() {
		String jdbcUrl = getBaseUrl() + getUrlProperties();

		if (config.useTraditionalJdbcUrl()) {
			setDriverClassName(vendor.driver.driverClassName());
			hikariConf.setJdbcUrl(jdbcUrl);

		} else {
			hikariConf.setDataSourceClassName(vendor.driver.dataSourceClassName());
			hikariConf.addDataSourceProperty("url", jdbcUrl);
		}
	}

	private String getBaseUrl() {
		return switch (vendor) {
			case MARIADB, MYSQL, POSTGRES, COCKROACH -> {
				SqlConfig.AuthDetails authDetails = config.authDetails();
				String host = authDetails.host();
				int port = authDetails.port();
				String database = authDetails.database();

				if (vendor.isPostgresLike()) {
					yield "jdbc:postgresql://" + host + ":" + port + "/" + database;
				} else {
					assert vendor.isMySQLLike();
					yield "jdbc:mariadb://" + host + ":" + port + "/" + database;
				}
			}
			case HSQLDB -> {
				Path databaseFolder = folder.resolve("internal").resolve("hypersql");
				if (!Files.exists(databaseFolder)) {
					try {
						Files.createDirectories(databaseFolder);
					} catch (IOException ex) {
						throw new StartupException("Cannot create database folder", ex);
					}
				}
				Path databaseFile = databaseFolder.resolve("punishments-database").toAbsolutePath();
				yield "jdbc:hsqldb:file:" + databaseFile;
			}
		};
	}
	
	private String getUrlProperties() {
		Map<String, Object> properties = switch (vendor) {
			case HSQLDB -> Map.of(
					// Prevent execution of multiple queries in one Statement
					"sql.restrict_exec", true,
					// Make the names of generated indexes the same as the names of the constraints
					"sql.sys_index_names", true,
					/*
					 * Enforce SQL standards on
					 * 1.) table and column names
					 * 2.) ambiguous column references
					 * 3.) illegal type conversions
					 */
					"sql.enforce_names", true,
					"sql.enforce_refs", true,
					"sql.enforce_types", true,
					// Respect interrupt status during query execution
					"hsqldb.tx_interrupt_rollback", true,
					// Use CACHED tables by default
					"hsqldb.default_table_type", "cached"
			);
			case MARIADB, MYSQL -> {
				Map<String, Object> props = new HashMap<>(Map.of(
						// Performance improvements
						"autocommit", DatabaseConstants.AUTOCOMMIT,
						"defaultFetchSize", DatabaseConstants.FETCH_SIZE,

						// Help debug in case of deadlock
						"includeInnodbStatusInDeadlockExceptions", true,
						"includeThreadDumpInDeadlockExceptions", true,

						// https://github.com/brettwooldridge/HikariCP/wiki/Rapid-Recovery#mysql
						"socketTimeout", DatabaseConstants.SOCKET_TIMEOUT
				));
				// Properties preceding can be overridden
				props.putAll(config.mariaDb().connectionProperties());
				// Properties following cannot be overridden

				props.putAll(Map.of(
						// Needed for use with connection init-SQL (hikariConf.setConnectionInitSql)
						"allowMultiQueries", true,
						// Help debug in case of exceptions
						"dumpQueriesOnException", true
				));
				yield props;
			}
			case POSTGRES, COCKROACH -> {
				Map<String, Object> props = new HashMap<>(Map.of(
						// Set default connecting settings
						"defaultRowFetchSize", DatabaseConstants.FETCH_SIZE,

						// https://github.com/brettwooldridge/HikariCP/wiki/Rapid-Recovery#postgresql
						"socketTimeout", DatabaseConstants.SOCKET_TIMEOUT
				));
				// Properties preceding can be overridden
				props.putAll(config.postgres().connectionProperties());

				yield props;
			}
		};
		logger.trace("Using connection properties {}", properties);
		return vendor.driver.formatConnectionProperties(properties);
	}
	
	/**
	 * Sets the driver class name utilizing the context classloader
	 * 
	 * @param driverClassName the driver class name
	 */
	private void setDriverClassName(String driverClassName) {
		Thread currentThread = Thread.currentThread();
		ClassLoader initialContextLoader = currentThread.getContextClassLoader();
		currentThread.setContextClassLoader(getClass().getClassLoader());
		try {
			hikariConf.setDriverClassName(driverClassName);
		} finally {
			currentThread.setContextClassLoader(initialContextLoader);
		}
	}
	
}

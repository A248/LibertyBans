/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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
import space.arim.libertybans.core.punish.MiscUtil;

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

		JooqContext jooqContext = new JooqContext(vendor.dialect());
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

	private void setHikariConfig() {
		setUsernameAndPassword();
		setConfiguredDriver();

		// Check database preconditions as soon as we are able
		// Provide the system property for the benefit of advanced users
		if (!Boolean.getBoolean("libertybans.database.disablecheck")) {
			hikariConf.setPoolName("LibertyBansTemporaryPool-" + vendor);
			try (HikariDataSource temporaryDataSource = new HikariDataSource(hikariConf);
				 Connection connection = temporaryDataSource.getConnection()) {
				connection.setReadOnly(true);
				new DatabaseRequirements(vendor, connection).checkRequirements();
			} catch (java.sql.SQLException ex) {
				throw new IllegalStateException(
						"Unable to connect to database. Please make sure your authentication details are correct.", ex);
			}
		}

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
		JdbcDriver jdbcDriver = vendor.driver();

		if (config.useTraditionalJdbcUrl()) {
			setDriverClassName(jdbcDriver.driverClassName());
			hikariConf.setJdbcUrl(jdbcUrl);

		} else {
			hikariConf.setDataSourceClassName(jdbcDriver.dataSourceClassName());
			hikariConf.addDataSourceProperty("url", jdbcUrl);
		}
	}

	private String getBaseUrl() {
		String url;
		switch (vendor) {

		case MARIADB:
		case MYSQL:
		case POSTGRES:
		case COCKROACH:
			SqlConfig.AuthDetails authDetails = config.authDetails();
			String host = authDetails.host();
			int port = authDetails.port();
			String database = authDetails.database();

			hikariConf.addDataSourceProperty("databaseName", database);
			if (vendor.isPostgresLike()) {
				url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
			} else {
				assert vendor.isMySQLLike();
				url = "jdbc:mariadb://" + host + ":" + port + "/" + database;
			}
			break;

		case HSQLDB:
			Path databaseFolder = folder.resolve("hypersql");
			try {
				Files.createDirectories(databaseFolder);
			} catch (IOException ex) {
				throw new StartupException("Cannot create database folder", ex);
			}
			Path databaseFile = databaseFolder.resolve("punishments-database").toAbsolutePath();
			url = "jdbc:hsqldb:file:" + databaseFile;
			break;

		default:
			throw MiscUtil.unknownVendor(vendor);
		}
		return url;
	}
	
	private String getUrlProperties() {
		Map<String, Object> properties = new HashMap<>();

		switch (vendor) {
		case HSQLDB:
			// Prevent execution of multiple queries in one Statement
			properties.put("sql.restrict_exec", true);
			// Make the names of generated indexes the same as the names of the constraints
			properties.put("sql.sys_index_names", true);
			/* 
			 * Enforce SQL standards on
			 * 1.) table and column names
			 * 2.) ambiguous column references
			 * 3.) illegal type conversions
			 */
			properties.put("sql.enforce_names", true);
			properties.put("sql.enforce_refs", true);
			properties.put("sql.enforce_types", true);
			// Respect interrupt status during query execution
			properties.put("hsqldb.tx_interrupt_rollback", true);
			// Use CACHED tables by default
			properties.put("hsqldb.default_table_type", "cached");

			break;

		case MARIADB:
		case MYSQL:
			// Performance improvements
			properties.put("autocommit", DatabaseConstants.AUTOCOMMIT);
			properties.put("defaultFetchSize", DatabaseConstants.FETCH_SIZE);

			// Help debug in case of deadlock
			properties.put("includeInnodbStatusInDeadlockExceptions", true);
			properties.put("includeThreadDumpInDeadlockExceptions", true);

			// https://github.com/brettwooldridge/HikariCP/wiki/Rapid-Recovery#mysql
			properties.put("socketTimeout", DatabaseConstants.SOCKET_TIMEOUT);

			// Properties preceding can be overridden
			properties.putAll(config.mariaDb().connectionProperties());
			// Properties following cannot be overridden

			// Needed for use with connection init-SQL (hikariConf.setConnectionInitSql)
			properties.put("allowMultiQueries", true);
			break;

		case POSTGRES:
		case COCKROACH:
			// Set default connecting settings
			properties.put("defaultRowFetchSize", DatabaseConstants.FETCH_SIZE);

			// https://github.com/brettwooldridge/HikariCP/wiki/Rapid-Recovery#postgresql
			properties.put("socketTimeout", DatabaseConstants.SOCKET_TIMEOUT);

			// Properties preceding can be overridden
			properties.putAll(config.postgres().connectionProperties());
			break;

		default:
			throw MiscUtil.unknownVendor(vendor);
		}
		logger.trace("Using connection properties {}", properties);
		return vendor.driver().formatConnectionProperties(properties);
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

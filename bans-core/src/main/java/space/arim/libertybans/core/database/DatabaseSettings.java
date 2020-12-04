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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

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
class DatabaseSettings {

	private final DatabaseManager manager;

	private SqlConfig config;
	private Vendor vendor;
	private HikariConfig hikariConf;
	private boolean refresherEvent;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	static {
		// Prevent HSQLDB from reconfiguring JUL/Log4j2
		System.setProperty("hsqldb.reconfig_logging", "false");
	}

	DatabaseSettings(DatabaseManager manager) {
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
	 * Creates an accessible database
	 * 
	 * @param config the config accessor
	 * @return a database result, which should be checked for success or failure
	 */
	DatabaseResult create(SqlConfig config) {
		this.config = config;
		vendor = config.vendor();
		hikariConf = new HikariConfig();

		setHikariConfig();

		HikariDataSource hikariDataSource = new HikariDataSource(hikariConf);
		refresherEvent = vendor == Vendor.MARIADB && config.mariaDb().useEventScheduler();

		StandardDatabase database = StandardDatabase.create(
				manager, vendor, hikariDataSource, hikariConf.getMaximumPoolSize(), refresherEvent);

		Flyway flyway = createFlyway(hikariDataSource);
		try {
			flyway.migrate();
		} catch (FlywayException ex) {
			logger.error("Unable to migrate your database. Please create a backup of your database "
					+ "and promptly report this issue.", ex);
			return new DatabaseResult(database, false);
		}
		return new DatabaseResult(database, true);
	}
	
	private Flyway createFlyway(HikariDataSource hikariDataSource) {
		List<String> locations = new ArrayList<>();
		locations.add("classpath:sql-migrations/common");
		//locations.add("classpath:sql-migrations/" + vendor);
		if (refresherEvent) {
			locations.add("classpath:sql-migrations/refresher-event");
		}
		return Flyway.configure(getClass().getClassLoader())
				.dataSource(hikariDataSource).table("libertybans_flyway")
				.locations(locations.toArray(String[]::new))
				.ignoreFutureMigrations(false).validateMigrationNaming(true).group(true)
				.load();
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
		hikariConf.setAutoCommit(DatabaseDefaults.AUTOCOMMIT);
		hikariConf.setTransactionIsolation("TRANSACTION_" + DatabaseDefaults.ISOLATION.name());
		hikariConf.setPoolName("LibertyBans-HikariCP-" + vendor.displayName());
	}
	
	private void setUsernameAndPassword() {
		SqlConfig.AuthDetails authDetails = config.authDetails();
		String username = authDetails.username();
		String password = authDetails.password();
		if (vendor == Vendor.MARIADB && (username.equals("username") || password.equals("defaultpass"))) {
			logger.warn("Not using MariaDB/MySQL because authentication details are still default");
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
			setDriverClassName(vendor.driverClassName());
			hikariConf.setJdbcUrl(jdbcUrl);

		} else {
			hikariConf.setDataSourceClassName(vendor.dataSourceClassName());
			hikariConf.addDataSourceProperty("url", jdbcUrl);
		}
	}
	
	private String getBaseUrl() {
		String url;
		switch (vendor) {

		case MARIADB:
			SqlConfig.AuthDetails authDetails = config.authDetails();
			String host = authDetails.host();
			int port = authDetails.port();
			String database = authDetails.database();

			hikariConf.addDataSourceProperty("databaseName", database);
			url = "jdbc:mariadb://" + host + ":" + port + "/" + database;
			break;

		case HSQLDB:
			Path databaseFolder = manager.folder().resolve("hypersql");
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
			// MySQL syntax compatibility
			properties.put("sql.syntax_mys", true);
			// Use case-insensitive comparisons
			properties.put("sql.ignore_case", true);
			// Prevent execution of multiple queries in one Statement
			properties.put("sql.restrict_exec", true);
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

			break;

		case MARIADB:
			// Set default connection settings
			properties.put("autocommit", DatabaseDefaults.AUTOCOMMIT);
			properties.put("defaultFetchSize", DatabaseDefaults.FETCH_SIZE);

			// Help debug in case of deadlock
			properties.put("includeInnodbStatusInDeadlockExceptions", true);
			properties.put("includeThreadDumpInDeadlockExceptions", true);

			// https://github.com/brettwooldridge/HikariCP/wiki/Rapid-Recovery#mysql
			properties.put("socketTimeout", Duration.ofSeconds(30L).toMillis());

			// User-defined additional properties
			properties.putAll(config.mariaDb().connectionProperties());

			break;

		default:
			throw MiscUtil.unknownVendor(vendor);
		}
		logger.trace("Using connection properties {}", properties);
		return vendor.formatConnectionProperties(properties);
	}
	
	/**
	 * Sets the driver class name utilising the context classloader
	 * 
	 * @param driverClassName the driver class name
	 */
	private void setDriverClassName(String driverClassName) {
		Thread currentThread = Thread.currentThread();
		ClassLoader initialContextLoader = currentThread.getContextClassLoader();
		try {
			currentThread.setContextClassLoader(getClass().getClassLoader());
			hikariConf.setDriverClassName(driverClassName);
		} finally {
			currentThread.setContextClassLoader(initialContextLoader);
		}
	}
	
}

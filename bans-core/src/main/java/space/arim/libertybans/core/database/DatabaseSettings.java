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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.bootstrap.StartupException;
import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.config.SqlConfig;
import space.arim.libertybans.core.punish.MiscUtil;

/**
 * Database settings creator, NOT thread safe!
 * 
 * @author A248
 *
 */
class DatabaseSettings {

	private final LibertyBansCore core;
	
	private SqlConfig config;
	private Vendor vendor;
	private HikariConfig hikariConf;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	DatabaseSettings(LibertyBansCore core) {
		this.core = core;
	}
	
	/**
	 * Shortcut for <code>create(core.getConfigs().getSql())</code>
	 * 
	 * @return a database result, which should be checked for success or failure
	 */
	CentralisedFuture<DatabaseResult> create() {
		return create(core.getConfigs().getSqlConfig());
	}
	
	/**
	 * Creates an accessible database
	 * 
	 * @param config the config accessor
	 * @return a database result, which should be checked for success or failure
	 */
	CentralisedFuture<DatabaseResult> create(SqlConfig config) {
		this.config = config;
		vendor = config.vendor();
		hikariConf = new HikariConfig();

		setHikariConfig();

		Database database = new Database(core, vendor, hikariConf, hikariConf.getMaximumPoolSize());

		CentralisedFuture<Boolean> tablesAndViewsCreation = new TableDefinitions(core, database).createTablesAndViews();
		return tablesAndViewsCreation.thenApply((success) -> new DatabaseResult(database, success));
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
		hikariConf.setAutoCommit(false);
		hikariConf.setTransactionIsolation("TRANSACTION_REPEATABLE_READ");
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
		final String jdbcUrl;
		switch (vendor) {

		case MARIADB:
			SqlConfig.AuthDetails authDetails = config.authDetails();
			String host = authDetails.host();
			int port = authDetails.port();
			String database = authDetails.database();

			hikariConf.addDataSourceProperty("databaseName", database);
			jdbcUrl = "jdbc:mariadb://" + host + ":" + port + "/" + database + getMariaDbUrlProperties();
			break;

		case HSQLDB:
			Path databaseFolder = core.getFolder().resolve("hypersql");
			try {
				Files.createDirectories(databaseFolder);
			} catch (IOException ex) {
				throw new StartupException("Cannot create database folder", ex);
			}
			Path databaseFile = databaseFolder.resolve("punishments-database").toAbsolutePath();
			jdbcUrl = "jdbc:hsqldb:file:" + databaseFile;
			break;

		default:
			throw MiscUtil.unknownVendor(vendor);
		}
		if (config.useTraditionalJdbcUrl()) {
			setDriverClassName(vendor.driverClassName());
			hikariConf.setJdbcUrl(jdbcUrl);

		} else {
			hikariConf.setDataSourceClassName(vendor.dataSourceClassName());
			hikariConf.addDataSourceProperty("url", jdbcUrl);
		}
	}
	
	private String getMariaDbUrlProperties() {
		List<String> connectProps = new ArrayList<>();
		for (Map.Entry<String, String> property : config.connectionProperties().entrySet()) {
			String propName = property.getKey();
			String propValue = property.getValue();
			logger.trace("Adding connection property {} with value {}", propName, propValue);
			connectProps.add(propName + "=" + propValue);
		}
		String properties = String.join("&", connectProps);
		return (properties.isEmpty()) ? "" : "?" + properties;
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

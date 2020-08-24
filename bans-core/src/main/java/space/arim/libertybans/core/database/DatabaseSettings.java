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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.configure.ConfigAccessor;

import space.arim.libertybans.bootstrap.StartupException;
import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.driver.DriverCreator;

/**
 * Database settings creator, NOT thread safe!
 * 
 * @author A248
 *
 */
class DatabaseSettings {

	private final LibertyBansCore core;
	
	private ConfigAccessor config;
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
		return create(core.getConfigs().getSql());
	}
	
	/**
	 * Creates an accessible database
	 * 
	 * @param config the config accessor
	 * @return a database result, which should be checked for success or failure
	 */
	CentralisedFuture<DatabaseResult> create(ConfigAccessor config) {
		this.config = config;
		vendor = config.getObject("rdms-vendor", Vendor.class);
		hikariConf = new HikariConfig();

		setHikariConfig();

		Database database = new Database(core, vendor, hikariConf, hikariConf.getMaximumPoolSize());

		CentralisedFuture<Boolean> tablesAndViewsCreation = new TableDefinitions(core, database).createTablesAndViews();
		return tablesAndViewsCreation.thenApply((success) -> new DatabaseResult(database, success));
	}
	
	private void setUsernameAndPassword() {
		String username = config.getString("auth-details.user");
		String password = config.getString("auth-details.password");
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
	
	private void setHikariConfig() {
		setUsernameAndPassword();

		int poolSize = config.getInteger("connection-pool-size");
		hikariConf.setMinimumIdle(poolSize);
		hikariConf.setMaximumPoolSize(poolSize);

		int connectionTimeout = config.getInteger("timeouts.connection-timeout-seconds");
		int maxLifetime = config.getInteger("timeouts.max-lifetime-minutes");
		hikariConf.setConnectionTimeout(TimeUnit.MILLISECONDS.convert(connectionTimeout, TimeUnit.SECONDS));
		hikariConf.setMaxLifetime(TimeUnit.MILLISECONDS.convert(maxLifetime, TimeUnit.MINUTES));

		setConfiguredDriver();

		hikariConf.setAutoCommit(false);

		@SuppressWarnings("unchecked")
		Map<String, Object> connectProps = config.getObject("connection-properties", Map.class);
		for (Map.Entry<String, Object> property : connectProps.entrySet()) {
			String propName = property.getKey();
			Object propValue = property.getValue();
			logger.trace("Setting data source property {} to {}", propName, propValue);
			hikariConf.addDataSourceProperty(propName, propValue);
		}

		hikariConf.setPoolName("LibertyBans-HikariCP-" + vendor.displayName());
	}
	
	private void setConfiguredDriver() {
		boolean jdbcUrl = config.getBoolean("use-traditional-jdbc-url");
		DriverCreator driverCreator = new DriverCreator(hikariConf, jdbcUrl);

		switch (vendor) {

		case MARIADB:
			String host = config.getString("auth-details.host");
			int port = config.getInteger("auth-details.port");
			String database = config.getString("auth-details.database");
			driverCreator.createMariaDb(host, port, database);
			break;

		case HSQLDB:
			Path databaseFolder = core.getFolder().resolve("hypersql");
			if (!Files.isDirectory(databaseFolder)) {
				try {
					Files.createDirectory(databaseFolder);
				} catch (IOException ex) {
					throw new StartupException("Cannot create database folder", ex);
				}
			}
			driverCreator.createHsqldb(databaseFolder + "/punishments-database");
			break;

		default:
			throw new IllegalStateException("Unknown database vendor " + vendor);
		}			
	}
	
}

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

import space.arim.api.configure.ConfigAccessor;

import space.arim.libertybans.bootstrap.StartupException;
import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.driver.DriverCreator;

class DatabaseSettings {

	private final LibertyBansCore core;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	DatabaseSettings(LibertyBansCore core) {
		this.core = core;
	}
	
	/**
	 * Shortcut for <code>create(core.getConfigs().getSql())</code>
	 * 
	 * @return a database result, which should be checked for success or failure
	 */
	DatabaseResult create() {
		return create(core.getConfigs().getSql());
	}
	
	/**
	 * Creates an accessible database
	 * 
	 * @param config the config accessor
	 * @return a database result, which should be checked for success or failure
	 */
	DatabaseResult create(ConfigAccessor config) {
		HikariConfig hikariConf = getHikariConfig(config);
		boolean usesMariaDb = hikariConf.getPoolName().contains("MariaDB"); // See end of #getHikariConfg

		Database database = new Database(core, hikariConf, hikariConf.getMaximumPoolSize());
		boolean success = new TableDefinitions(core, database).createTablesAndViews(usesMariaDb).join();

		return new DatabaseResult(database, success);
	}
	
	// Returns true if authentication details still allow MariaDB usage
	private static boolean setUsernameAndPassword(ConfigAccessor config, boolean useMariaDb, HikariConfig hikariConf) {
		String username = config.getString("mariadb-details.user");
		String password = config.getString("mariadb-details.password");
		if (useMariaDb && (username.equals("username") || password.equals("defaultpass"))) {
			logger.warn("Not using MySQL because authentication details are still default");
			useMariaDb = false;
		}
		if (!useMariaDb) {
			username = "SA";
			password = "";
		}
		hikariConf.setUsername(username);
		hikariConf.setPassword(password);
		return useMariaDb;
	}
	
	private HikariConfig getHikariConfig(ConfigAccessor config) {
		HikariConfig hikariConf = new HikariConfig();
		boolean useMariaDb = config.getBoolean("storage-backend-mysql");

		useMariaDb = setUsernameAndPassword(config, useMariaDb, hikariConf);

		int poolSize = config.getInteger("connection-pool-size");
		hikariConf.setMinimumIdle(poolSize);
		hikariConf.setMaximumPoolSize(poolSize);

		int connectionTimeout = config.getInteger("timeouts.connection-timeout-seconds");
		int maxLifetime = config.getInteger("timeouts.max-lifetime-minutes");
		hikariConf.setConnectionTimeout(TimeUnit.MILLISECONDS.convert(connectionTimeout, TimeUnit.SECONDS));
		hikariConf.setMaxLifetime(TimeUnit.MILLISECONDS.convert(maxLifetime, TimeUnit.MINUTES));

		setConfiguredDriver(config, hikariConf, useMariaDb);

		hikariConf.setAutoCommit(false);

		@SuppressWarnings("unchecked")
		Map<String, Object> connectProps = config.getObject("connection-properties", Map.class);
		for (Map.Entry<String, Object> property : connectProps.entrySet()) {
			String propName = property.getKey();
			Object propValue = property.getValue();
			logger.trace("Setting data source property {} to {}", propName, propValue);
			hikariConf.addDataSourceProperty(propName, propValue);
		}
		/*
		 * The presence of "MariaDB" in the pool name is relied on elsewhere:
		 * 1. DatabaseSettings#create(ConfigAccessor)
		 * 2. Database#usesHyperSQL()
		 */
		String mode = (useMariaDb) ? "MariaDB" : "HyperSQL";
		hikariConf.setPoolName("LibertyBans-HikariCP-" + mode);
		return hikariConf;
	}
	
	private void setConfiguredDriver(ConfigAccessor config, HikariConfig hikariConf, boolean useMariaDb) {
		boolean jdbcUrl = config.getBoolean("use-traditional-jdbc-url");
		DriverCreator driverCreator = new DriverCreator(hikariConf, jdbcUrl);
		if (useMariaDb) {
			String host = config.getString("mariadb-details.host");
			int port = config.getInteger("mariadb-details.port");
			String database = config.getString("mariadb-details.database");
			driverCreator.createMariaDb(host, port, database);

		} else {
			Path databaseFolder = core.getFolder().resolve("hypersql");
			if (!Files.isDirectory(databaseFolder)) {
				try {
					Files.createDirectory(databaseFolder);
				} catch (IOException ex) {
					throw new StartupException("Cannot create database folder", ex);
				}
			}
			driverCreator.createHsqldb(databaseFolder + "/punishments-database");
		}
	}
	
}

/* 
 * LibertyBans-bootstrap
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-bootstrap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-bootstrap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-bootstrap. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.bootstrap;

enum RelocationCheck {
	
	HSQLDB("HSQLDB", "org.hsqldb.jdbc.JDBCDriver"),
	MARIADB("MariaDB-Connector", "org.mariadb.jdbc.Driver"),
	
	HIKARICP("HikariCP", "com.zaxxer.hikari.HikariConfig"),
	FLYWAY("Flyway", "org.flywaydb.core.Flyway"),
	
	DAZZLECONF_CORE("DazzleConf-Core", "space.arim.dazzleconf.ConfigurationFactory"),
	DAZZLECONF_EXT_SNAKEYAML("DazzleConf-SnakeYaml",
			"space.arim.dazzleconf.ext.snakeyaml.SnakeYamlConfigurationFactory"),
	
	JAKARTA_INJECT("Jakarta-Inject", "jakarta.inject.Provider"),
	SOLID_INJECTOR("SolidInjector", "space.arim.injector.Injector");

	private final String libName;
	private final String className;

	RelocationCheck(String libName, String className) {
		this.libName = libName;
		this.className = className;
	}

	ClassPresence classPresence() {
		return new ClassPresence(className, libName);
	}
	
}
